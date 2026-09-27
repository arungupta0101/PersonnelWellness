"""
Comprehensive Automated Test Suite for Personnel Wellness & ML Pipeline (SIH26186)
===================================================================================
Tests all 13 core user flows, edge cases, negative cases, ML pipeline validity, RBAC, consent, and error handling.
"""

from datetime import datetime, timedelta, timezone
from jose import jwt
import pytest
from fastapi.testclient import TestClient

from app.auth.service import ensure_seed_data
from app.config import settings
from app.database import Base, SessionLocal, engine
from app.main import app
from app.models.entities import Consent, Prediction, User


@pytest.fixture(scope="module", autouse=True)
def setup_database():
    Base.metadata.create_all(bind=engine)
    with SessionLocal() as db:
        ensure_seed_data(db)
    yield


# --------------------------------------------------------------------------
# Flow 1 & 8: Authentication (Personnel Login & Welfare Officer Login)
# --------------------------------------------------------------------------
def test_flow_1_and_8_personnel_and_officer_login():
    client = TestClient(app)

    # 1. Personnel Login
    p_res = client.post("/auth/login", json={"username": "demo", "password": "DemoPass123!"})
    assert p_res.status_code == 200
    p_data = p_res.json()
    assert "access_token" in p_data
    assert p_data["role"] == "personnel"

    # 2. Welfare Officer Login
    w_res = client.post("/auth/login", json={"username": "welfare.officer", "password": "DemoPass123!"})
    assert w_res.status_code == 200
    w_data = w_res.json()
    assert "access_token" in w_data
    assert w_data["role"] == "welfare_officer"


# --------------------------------------------------------------------------
# Flow 2: Wellness Check-in API
# --------------------------------------------------------------------------
def test_flow_2_wellness_checkin():
    client = TestClient(app)
    login_res = client.post("/auth/login", json={"username": "demo", "password": "DemoPass123!"})
    token = login_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    payload = {
        "mood": 7,
        "stress_level": 4,
        "sleep_hours": 7.0,
        "notes": "Feeling good today, standard operational shift.",
    }
    res = client.post("/wellness/checkin", json=payload, headers=headers)
    assert res.status_code == 201
    data = res.json()
    assert data["mood"] == 7
    assert data["stress_level"] == 4
    assert data["sleep_hours"] == 7.0

    # Own History
    history_res = client.get("/wellness/history", headers=headers)
    assert history_res.status_code == 200
    assert len(history_res.json()) >= 1


# --------------------------------------------------------------------------
# Flow 3: Assessment Submission API
# --------------------------------------------------------------------------
def test_flow_3_assessment_submission():
    client = TestClient(app)
    login_res = client.post("/auth/login", json={"username": "demo", "password": "DemoPass123!"})
    token = login_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    payload = {
        "assessment_type": "phq9_screening",
        "answers": {"q1": 1, "q2": 2, "q3": 0, "q4": 1},
    }
    res = client.post("/assessment/submit", json=payload, headers=headers)
    assert res.status_code == 201
    data = res.json()
    assert data["assessment_type"] == "phq9_screening"
    assert data["answers"]["q1"] == 1


# --------------------------------------------------------------------------
# Flow 4, 5, 6, 7: ML Prediction, Risk Explanation, Recommendations & History
# --------------------------------------------------------------------------
def test_flow_4_5_6_7_ml_prediction_explanation_recommendations_history():
    client = TestClient(app)
    login_res = client.post("/auth/login", json={"username": "demo", "password": "DemoPass123!"})
    token = login_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    # HIGH RISK payload
    payload = {
        "duty_hours_day": 14.0,
        "weekly_duty_hours": 75.0,
        "deployment_days": 180,
        "night_shifts": 12,
        "workload_score": 9.5,
        "training_load": 8.0,
        "transfer_frequency": 3,
        "sleep_hours": 4.0,
        "leave_days": 1,
        "rest_days": 1,
        "self_reported_stress": 9.0,
        "fatigue_score": 9.0,
        "mood_score": 2.0,
        "social_support_score": 2.0,
    }

    res = client.post("/prediction/predict", json=payload, headers=headers)
    assert res.status_code == 200
    data = res.json()

    # Flow 4: Prediction
    assert data["risk_level"] in ["LOW", "MODERATE", "HIGH"]
    assert 0.0 <= data["risk_score"] <= 1.0
    assert data["is_medical_diagnosis"] is False
    assert "screening prototype" in data["disclaimer"].lower()

    # Flow 5: Risk Explanation (top_contributing_factors)
    assert len(data["top_contributing_factors"]) > 0
    first_factor = data["top_contributing_factors"][0]
    assert "feature" in first_factor
    assert "impact_score" in first_factor

    # Flow 6: Recommendations
    assert len(data["welfare_recommendations"]) > 0

    # Flow 7: History
    hist_res = client.get("/prediction/history", headers=headers)
    assert hist_res.status_code == 200
    hist_data = hist_res.json()
    assert len(hist_data) >= 1
    assert hist_data[0]["risk_level"] == data["risk_level"]


# --------------------------------------------------------------------------
# Flow 9 & 10 & 12: Officer Dashboard APIs, RBAC & Consent APIs
# --------------------------------------------------------------------------
def test_flow_9_10_12_officer_dashboard_rbac_consent():
    client = TestClient(app)

    # 1. Personnel & Officer logins
    p_login = client.post("/auth/login", json={"username": "demo", "password": "DemoPass123!"}).json()
    p_headers = {"Authorization": f"Bearer {p_login['access_token']}"}
    p_user_id = p_login["user_id"]

    w_login = client.post("/auth/login", json={"username": "welfare.officer", "password": "DemoPass123!"}).json()
    w_headers = {"Authorization": f"Bearer {w_login['access_token']}"}

    # 2. Revoke Consent as Personnel
    client.post("/consent/revoke", headers=p_headers)

    # 3. Officer attempts access -> 403 Forbidden
    blocked_res = client.get(f"/wellness/personnel/{p_user_id}", headers=w_headers)
    assert blocked_res.status_code == 403

    # 4. Grant Consent as Personnel
    grant_res = client.post("/consent/grant", headers=p_headers)
    assert grant_res.status_code == 200
    assert grant_res.json()["granted"] is True

    # 5. Officer accesses dashboard -> 200 OK
    allowed_res = client.get(f"/wellness/personnel/{p_user_id}", headers=w_headers)
    assert allowed_res.status_code == 200


# --------------------------------------------------------------------------
# Flow 11: Intervention / Support APIs
# --------------------------------------------------------------------------
def test_flow_11_interventions():
    client = TestClient(app)

    # Personnel requests support
    p_login = client.post("/auth/login", json={"username": "demo", "password": "DemoPass123!"}).json()
    p_headers = {"Authorization": f"Bearer {p_login['access_token']}"}

    req_payload = {
        "intervention_type": "peer_counseling",
        "details": "Requesting peer interaction support for fatigue management.",
    }
    req_res = client.post("/support/request", json=req_payload, headers=p_headers)
    assert req_res.status_code == 201
    assert req_res.json()["intervention_type"] == "peer_counseling"

    # Officer lists interventions
    w_login = client.post("/auth/login", json={"username": "welfare.officer", "password": "DemoPass123!"}).json()
    w_headers = {"Authorization": f"Bearer {w_login['access_token']}"}

    list_res = client.get("/support/interventions", headers=w_headers)
    assert list_res.status_code == 200
    assert len(list_res.json()) >= 1


# --------------------------------------------------------------------------
# Flow 13 & Edge Cases: JWT Expired/Invalid, Negative Inputs & Edge Cases
# --------------------------------------------------------------------------
def test_flow_13_jwt_invalidation_and_invalid_tokens():
    client = TestClient(app)

    # 1. Invalid JWT Signature / Token
    invalid_headers = {"Authorization": "Bearer invalid.jwt.token.string"}
    inv_res = client.get("/auth/me", headers=invalid_headers)
    assert inv_res.status_code == 401

    # 2. Expired JWT Token
    expired_payload = {
        "sub": "1",
        "role": "personnel",
        "exp": datetime.now(timezone.utc) - timedelta(minutes=10),
    }
    expired_token = jwt.encode(expired_payload, settings.jwt_secret_key, algorithm=settings.jwt_algorithm)
    exp_headers = {"Authorization": f"Bearer {expired_token}"}
    exp_res = client.get("/auth/me", headers=exp_headers)
    assert exp_res.status_code == 401


def test_edge_cases_and_negative_inputs():
    client = TestClient(app)
    login_res = client.post("/auth/login", json={"username": "demo", "password": "DemoPass123!"}).json()
    headers = {"Authorization": f"Bearer {login_res['access_token']}"}

    # 1. Invalid Login Inputs (Missing fields)
    bad_login = client.post("/auth/login", json={"username": "demo"})
    assert bad_login.status_code == 422

    # 2. Invalid Check-in Input (Out of range values)
    bad_checkin = client.post(
        "/wellness/checkin",
        json={"mood": 15, "stress_level": -2, "sleep_hours": 30.0},
        headers=headers,
    )
    assert bad_checkin.status_code == 422

    # 3. Invalid Prediction Features (Out of bounds inputs)
    bad_predict = client.post(
        "/prediction/predict",
        json={"duty_hours_day": 28.0, "sleep_hours": -5.0},
        headers=headers,
    )
    assert bad_predict.status_code == 422

    # 4. Non-existent User Access
    w_login = client.post("/auth/login", json={"username": "welfare.officer", "password": "DemoPass123!"}).json()
    w_headers = {"Authorization": f"Bearer {w_login['access_token']}"}

    no_user_res = client.get("/wellness/personnel/99999", headers=w_headers)
    assert no_user_res.status_code == 404
