"""
Automated Test Suite for One Wellness Check-in per Calendar Day Constraint (SIH 2026)
======================================================================================
Tests:
A. First check-in today -> HTTP 201 Created success
B. Second check-in same day for same user -> HTTP 409 Conflict
C. Different user submitting on same day -> HTTP 201 Created success
D. Check-in on next calendar day -> HTTP 201 Created success
E. Existing history endpoint still returns stored records -> HTTP 200 OK
F. Unauthorized user still receives existing authorization response -> HTTP 401 Unauthorized
"""

from datetime import datetime, timedelta, timezone
import pytest
from fastapi.testclient import TestClient

from app.auth.service import ensure_seed_data
from app.database import Base, SessionLocal, engine
from app.main import app
from app.models.entities import User, WellnessCheckin


@pytest.fixture(scope="module", autouse=True)
def setup_database():
    Base.metadata.create_all(bind=engine)
    with SessionLocal() as db:
        ensure_seed_data(db)
        # Ensure any pre-seeded check-in is backdated so current day starts fresh
        demo_user = db.query(User).filter(User.username == "demo").first()
        if demo_user:
            for c in db.query(WellnessCheckin).filter(WellnessCheckin.user_id == demo_user.id).all():
                c.created_at = datetime.now(timezone.utc) - timedelta(days=1)
            db.commit()
    yield


def test_requirements_a_through_f_daily_checkin_rule():
    client = TestClient(app)

    # ----------------------------------------------------------------------
    # Requirement F: Unauthorized user receives HTTP 401 Unauthorized
    # ----------------------------------------------------------------------
    unauth_res = client.post("/wellness/checkin", json={"mood": 8, "stress_level": 3, "sleep_hours": 7.0})
    assert unauth_res.status_code == 401

    # ----------------------------------------------------------------------
    # Requirement A: First check-in today -> 201 Created Success
    # ----------------------------------------------------------------------
    p_login = client.post("/auth/login", json={"username": "demo", "password": "DemoPass123!"}).json()
    p_headers = {"Authorization": f"Bearer {p_login['access_token']}"}

    payload_a = {
        "mood": 8,
        "stress_level": 3,
        "sleep_hours": 7.5,
        "notes": "Requirement A: First check-in today.",
    }
    res_a = client.post("/wellness/checkin", json=payload_a, headers=p_headers)
    assert res_a.status_code == 201
    data_a = res_a.json()
    assert data_a["mood"] == 8
    assert data_a["stress_level"] == 3

    # ----------------------------------------------------------------------
    # Requirement B: Second check-in same day for same user -> 409 Conflict
    # ----------------------------------------------------------------------
    payload_b = {
        "mood": 4,
        "stress_level": 8,
        "sleep_hours": 5.0,
        "notes": "Requirement B: Second check-in same day.",
    }
    res_b = client.post("/wellness/checkin", json=payload_b, headers=p_headers)
    assert res_b.status_code == 409
    assert res_b.json()["detail"] == "Daily wellness check-in already submitted for today."

    # ----------------------------------------------------------------------
    # Requirement C: Different user submitting on same day -> 201 Created Success
    # ----------------------------------------------------------------------
    admin_login = client.post("/auth/login", json={"username": "admin", "password": "DemoPass123!"}).json()
    admin_headers = {"Authorization": f"Bearer {admin_login['access_token']}"}

    payload_c = {
        "mood": 9,
        "stress_level": 2,
        "sleep_hours": 8.0,
        "notes": "Requirement C: Admin check-in on same day.",
    }
    res_c = client.post("/wellness/checkin", json=payload_c, headers=admin_headers)
    assert res_c.status_code == 201
    assert res_c.json()["mood"] == 9

    # ----------------------------------------------------------------------
    # Requirement D: Check-in on next calendar day -> 201 Created Success
    # ----------------------------------------------------------------------
    # Backdate demo's existing checkins to 2 days ago to simulate a fresh next calendar day
    with SessionLocal() as db:
        demo_user = db.query(User).filter(User.username == "demo").first()
        demo_checkins = db.query(WellnessCheckin).filter(WellnessCheckin.user_id == demo_user.id).all()
        for c in demo_checkins:
            c.created_at = datetime.now(timezone.utc) - timedelta(days=2)
        db.commit()

    payload_d = {
        "mood": 7,
        "stress_level": 4,
        "sleep_hours": 7.0,
        "notes": "Requirement D: Next calendar day check-in.",
    }
    res_d = client.post("/wellness/checkin", json=payload_d, headers=p_headers)
    assert res_d.status_code == 201
    assert res_d.json()["notes"] == "Requirement D: Next calendar day check-in."

    # ----------------------------------------------------------------------
    # Requirement E: Existing history endpoint still returns stored records
    # ----------------------------------------------------------------------
    history_res = client.get("/wellness/history", headers=p_headers)
    assert history_res.status_code == 200
    history_data = history_res.json()
    assert len(history_data) >= 2
