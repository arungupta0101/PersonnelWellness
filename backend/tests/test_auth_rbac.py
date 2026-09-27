"""
Integration Tests for Authentication, RBAC, and Audit Logging
===============================================================
"""

import pytest
from fastapi.testclient import TestClient

from app.auth.service import ensure_seed_data
from app.database import Base, SessionLocal, engine
from app.main import app
from app.models.entities import AuditLog, Consent, User


@pytest.fixture(scope="module", autouse=True)
def setup_database():
    Base.metadata.create_all(bind=engine)
    with SessionLocal() as db:
        ensure_seed_data(db)
    yield


def test_login_and_jwt_generation():
    client = TestClient(app)

    # 1. Successful Login
    response = client.post(
        "/auth/login",
        json={"username": "demo", "password": "DemoPass123!"},
    )
    assert response.status_code == 200, response.text
    data = response.json()
    assert "access_token" in data
    assert data["role"] == "personnel"
    assert data["username"] == "demo"

    # 2. Invalid Credentials Login
    invalid_res = client.post(
        "/auth/login",
        json={"username": "demo", "password": "WrongPassword!"},
    )
    assert invalid_res.status_code == 401


def test_authenticated_get_me():
    client = TestClient(app)

    # Obtain token
    login_res = client.post(
        "/auth/login",
        json={"username": "demo", "password": "DemoPass123!"},
    )
    token = login_res.json()["access_token"]

    # 1. Unauthenticated request -> 401
    res_unauth = client.get("/auth/me")
    assert res_unauth.status_code == 401

    # 2. Authenticated request -> 200
    res_auth = client.get(
        "/auth/me",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert res_auth.status_code == 200
    user_data = res_auth.json()
    assert user_data["username"] == "demo"
    assert user_data["role"] == "personnel"


def test_personnel_role_rules():
    client = TestClient(app)

    # Login as Personnel
    login_res = client.post(
        "/auth/login",
        json={"username": "demo", "password": "DemoPass123!"},
    )
    token = login_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    # 1. Submit Checkin -> 201
    checkin_payload = {
        "mood": 8,
        "stress_level": 3,
        "sleep_hours": 7.5,
        "notes": "Testing RBAC personnel checkin",
    }
    checkin_res = client.post("/wellness/checkin", json=checkin_payload, headers=headers)
    assert checkin_res.status_code == 201

    # 2. View Own Checkins -> 200
    history_res = client.get("/wellness/history", headers=headers)
    assert history_res.status_code == 200

    # 3. Attempt Unauthorized View of Other Personnel -> 403 Forbidden
    unauth_res = client.get("/wellness/personnel/2", headers=headers)
    assert unauth_res.status_code == 403


def test_welfare_officer_role_rules_and_audit_logging():
    client = TestClient(app)

    # Ensure target personnel (user 1) has granted data sharing consent
    with SessionLocal() as db:
        user_1 = db.query(User).filter(User.username == "demo").first()
        assert user_1 is not None
        consent = db.query(Consent).filter(Consent.user_id == user_1.id, Consent.consent_type == "data_sharing_wellness").first()
        if not consent:
            db.add(Consent(user_id=user_1.id, consent_type="data_sharing_wellness", granted=True))
        else:
            consent.granted = True
            db.add(consent)
        db.commit()

    # Login as Welfare Officer
    login_res = client.post(
        "/auth/login",
        json={"username": "welfare.officer", "password": "DemoPass123!"},
    )
    token = login_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    # 1. Access Personnel 1 Checkins -> 200 OK
    res = client.get(f"/wellness/personnel/{user_1.id}", headers=headers)
    assert res.status_code == 200

    # 2. Verify Audit Log recorded in DB
    with SessionLocal() as db:
        audit = (
            db.query(AuditLog)
            .filter(
                AuditLog.action == "SENSITIVE_ACCESS_WELLNESS_CHECKINS",
                AuditLog.resource == f"user:{user_1.id}",
                AuditLog.status == "SUCCESS",
            )
            .order_by(AuditLog.id.desc())
            .first()
        )
        assert audit is not None
        assert audit.details["role"] == "welfare_officer"


def test_commander_role_rules_and_aggregation():
    client = TestClient(app)

    # Login as Commander
    login_res = client.post(
        "/auth/login",
        json={"username": "commander", "password": "DemoPass123!"},
    )
    token = login_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    # 1. Commander views aggregated unit summary -> 200 OK
    summary_res = client.get("/assessment/unit-summary", headers=headers)
    assert summary_res.status_code == 200
    data = summary_res.json()
    assert "aggregate_risk_distribution" in data
    assert "total_active_personnel" in data

    # 2. Commander attempts to view individual wellness checkins -> 403 Forbidden
    forbidden_res = client.get("/wellness/personnel/1", headers=headers)
    assert forbidden_res.status_code == 403


def test_admin_role_rules():
    client = TestClient(app)

    # Login as Admin
    login_res = client.post(
        "/auth/login",
        json={"username": "admin", "password": "DemoPass123!"},
    )
    token = login_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    # Admin access unit summary & personnel directory -> 200 OK
    dir_res = client.get("/personnel/list", headers=headers)
    assert dir_res.status_code == 200
    assert len(dir_res.json()) >= 4
