"""
Automated Test Suite for One Wellness Check-in per Calendar Day Constraint
==========================================================================
Tests:
1. First check-in today -> HTTP 201 Created
2. Duplicate check-in today -> HTTP 409 Conflict
3. Next-day check-in -> HTTP 201 Created
4. Multi-user isolation (different user can submit once per day) -> HTTP 201 Created
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
    yield


def test_one_checkin_per_day_rule():
    client = TestClient(app)

    # 1. Login as Personnel (demo)
    login_res = client.post("/auth/login", json={"username": "demo", "password": "DemoPass123!"})
    assert login_res.status_code == 200
    token = login_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    payload_1 = {
        "mood": 8,
        "stress_level": 3,
        "sleep_hours": 7.5,
        "notes": "First check-in of today.",
    }

    # 2. First check-in today -> HTTP 201 Created
    res_1 = client.post("/wellness/checkin", json=payload_1, headers=headers)
    assert res_1.status_code == 201
    data_1 = res_1.json()
    assert data_1["mood"] == 8
    assert data_1["stress_level"] == 3

    # 3. Second check-in today (duplicate) -> HTTP 409 Conflict
    payload_2 = {
        "mood": 5,
        "stress_level": 7,
        "sleep_hours": 5.0,
        "notes": "Attempting duplicate check-in today.",
    }
    res_2 = client.post("/wellness/checkin", json=payload_2, headers=headers)
    assert res_2.status_code == 409
    assert res_2.json()["detail"] == "Today's wellness check-in has already been submitted."

    # 4. Multi-user isolation: Admin user (who has admin role allowed in endpoint) submits check-in today -> 201 Created
    admin_login = client.post("/auth/login", json={"username": "admin", "password": "DemoPass123!"})
    assert admin_login.status_code == 200
    admin_headers = {"Authorization": f"Bearer {admin_login.json()['access_token']}"}

    admin_payload = {
        "mood": 9,
        "stress_level": 2,
        "sleep_hours": 8.0,
        "notes": "Admin check-in today.",
    }
    admin_res = client.post("/wellness/checkin", json=admin_payload, headers=admin_headers)
    assert admin_res.status_code == 201

    # 5. Next-day submission test (simulate tomorrow by updating database timestamp of earlier checkin to yesterday)
    with SessionLocal() as db:
        demo_user = db.query(User).filter(User.username == "demo").first()
        today_checkins = (
            db.query(WellnessCheckin)
            .filter(WellnessCheckin.user_id == demo_user.id)
            .all()
        )
        # Backdate demo's checkins to 2 days ago
        for c in today_checkins:
            c.created_at = datetime.now(timezone.utc) - timedelta(days=2)
        db.commit()

    # Demo submits again (simulating next day) -> HTTP 201 Created
    payload_next_day = {
        "mood": 9,
        "stress_level": 2,
        "sleep_hours": 8.0,
        "notes": "Fresh check-in on the next day.",
    }
    res_next_day = client.post("/wellness/checkin", json=payload_next_day, headers=headers)
    assert res_next_day.status_code == 201
    assert res_next_day.json()["notes"] == "Fresh check-in on the next day."
