"""
Privacy, Consent, and Security Hardening Test Suite
====================================================
Tests consent workflows, consent enforcement on welfare routes, sensitive data redaction in audit logs,
security headers, and minimum necessary data exposure.
"""

import pytest
from fastapi.testclient import TestClient

from app.auth.service import ensure_seed_data
from app.database import Base, SessionLocal, engine
from app.main import app
from app.models.entities import AuditLog, Consent, User
from app.services.audit_service import sanitize_details


@pytest.fixture(scope="module", autouse=True)
def setup_database():
    Base.metadata.create_all(bind=engine)
    with SessionLocal() as db:
        ensure_seed_data(db)
    yield


def test_security_headers():
    client = TestClient(app)
    response = client.get("/health")
    assert response.status_code == 200
    assert response.headers.get("x-content-type-options") == "nosniff"
    assert response.headers.get("x-frame-options") == "DENY"
    assert "max-age=31536000" in response.headers.get("strict-transport-security", "")
    assert response.headers.get("x-xss-protection") == "1; mode=block"


def test_consent_grant_and_revoke_flow():
    client = TestClient(app)

    # 1. Login as personnel
    login_res = client.post("/auth/login", json={"username": "demo", "password": "DemoPass123!"})
    assert login_res.status_code == 200
    token = login_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    # 2. Grant Consent
    grant_res = client.post("/consent/grant", headers=headers)
    assert grant_res.status_code == 200
    assert grant_res.json()["granted"] is True

    # 3. Check Consent Status
    status_res = client.get("/consent/status", headers=headers)
    assert status_res.status_code == 200
    assert status_res.json()["granted"] is True

    # 4. Revoke Consent
    revoke_res = client.post("/consent/revoke", headers=headers)
    assert revoke_res.status_code == 200
    assert revoke_res.json()["granted"] is False

    # 5. Verify Consent Status Revoked
    status_revoked_res = client.get("/consent/status", headers=headers)
    assert status_revoked_res.status_code == 200
    assert status_revoked_res.json()["granted"] is False


def test_consent_enforcement_on_welfare_officer_access():
    client = TestClient(app)

    # Login as Personnel and Revoke Consent
    p_login = client.post("/auth/login", json={"username": "demo", "password": "DemoPass123!"})
    p_token = p_login.json()["access_token"]
    p_headers = {"Authorization": f"Bearer {p_token}"}
    p_user_id = p_login.json()["user_id"]

    client.post("/consent/revoke", headers=p_headers)

    # Login as Welfare Officer
    w_login = client.post("/auth/login", json={"username": "welfare.officer", "password": "DemoPass123!"})
    w_token = w_login.json()["access_token"]
    w_headers = {"Authorization": f"Bearer {w_token}"}

    # 1. Access un-consented personnel checkins -> 403 Forbidden
    blocked_wellness = client.get(f"/wellness/personnel/{p_user_id}", headers=w_headers)
    assert blocked_wellness.status_code == 403
    assert "consent" in blocked_wellness.json()["detail"].lower()

    # Verify audit failure recorded
    with SessionLocal() as db:
        failed_audit = (
            db.query(AuditLog)
            .filter(
                AuditLog.action == "SENSITIVE_ACCESS_WELLNESS_CHECKINS",
                AuditLog.resource == f"user:{p_user_id}",
                AuditLog.status == "FAILURE",
            )
            .order_by(AuditLog.id.desc())
            .first()
        )
        assert failed_audit is not None

    # 2. Grant Consent
    client.post("/consent/grant", headers=p_headers)

    # 3. Access consented personnel checkins -> 200 OK
    allowed_wellness = client.get(f"/wellness/personnel/{p_user_id}", headers=w_headers)
    assert allowed_wellness.status_code == 200

    # Verify audit success recorded
    with SessionLocal() as db:
        success_audit = (
            db.query(AuditLog)
            .filter(
                AuditLog.action == "SENSITIVE_ACCESS_WELLNESS_CHECKINS",
                AuditLog.resource == f"user:{p_user_id}",
                AuditLog.status == "SUCCESS",
            )
            .order_by(AuditLog.id.desc())
            .first()
        )
        assert success_audit is not None


def test_sensitive_data_redaction():
    raw_details = {
        "user_id": 1,
        "password": "SuperSecretPassword123!",
        "notes": "Sensitive psychological answers",
        "answers": {"q1": 5, "q2": 4},
        "role": "personnel",
    }

    sanitized = sanitize_details(raw_details)
    assert sanitized["password"] == "[REDACTED]"
    assert sanitized["notes"] == "[REDACTED]"
    assert sanitized["answers"] == "[REDACTED]"
    assert sanitized["user_id"] == 1
    assert sanitized["role"] == "personnel"


def test_minimum_necessary_data_exposure_unit_summary():
    client = TestClient(app)

    c_login = client.post("/auth/login", json={"username": "commander", "password": "DemoPass123!"})
    c_token = c_login.json()["access_token"]
    c_headers = {"Authorization": f"Bearer {c_token}"}

    res = client.get("/assessment/unit-summary", headers=c_headers)
    assert res.status_code == 200
    data = res.json()

    # Ensure no PII in response payload
    assert "user_id" not in data
    assert "full_name" not in data
    assert "employee_id" not in data
    assert "aggregate_risk_distribution" in data
    assert "total_active_personnel" in data
