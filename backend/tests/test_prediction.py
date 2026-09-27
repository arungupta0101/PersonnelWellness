"""
Integration Tests for Stress Risk Prediction API Endpoint
===========================================================
"""

import pytest
from fastapi.testclient import TestClient

from app.auth.service import ensure_seed_data
from app.database import Base, SessionLocal, engine
from app.main import app
from app.models.entities import Prediction, Recommendation, User


@pytest.fixture(scope="module", autouse=True)
def setup_database():
    Base.metadata.create_all(bind=engine)
    with SessionLocal() as db:
        ensure_seed_data(db)
    yield


def test_predict_endpoint_high_risk():
    client = TestClient(app)

    # 1. Login to receive JWT token
    login_res = client.post(
        "/auth/login",
        json={"username": "demo", "password": "DemoPass123!"},
    )
    assert login_res.status_code == 200, login_res.text
    token = login_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    # Get user id of seeded personnel user
    with SessionLocal() as db:
        user = db.query(User).filter(User.username == "demo").first()
        assert user is not None
        user_id = user.id

    payload = {
        "duty_hours": 12.0,
        "weekly_duty_hours": 70.0,
        "deployment_days": 45,
        "night_shifts": 8,
        "workload_score": 9.0,
        "training_load": 8.0,
        "transfer_frequency": 2,
        "sleep_hours": 4.0,
        "leave_days": 1,
        "rest_days": 1,
        "stress_score": 9.0,
        "fatigue_score": 9.0,
        "mood_score": 2.0,
        "social_support_score": 3.0,
        "user_id": user_id,
    }

    response = client.post("/prediction/predict", json=payload, headers=headers)
    assert response.status_code == 200, response.text

    data = response.json()
    assert data["risk_level"] in ["LOW", "MODERATE", "HIGH"]
    assert "risk_score" in data
    assert "confidence" in data
    assert data["is_medical_diagnosis"] is False
    assert len(data["top_contributing_factors"]) > 0
    assert len(data["welfare_recommendations"]) > 0
    assert data["prediction_id"] is not None

    # Verify DB persistence
    with SessionLocal() as db:
        prediction_in_db = db.query(Prediction).filter(Prediction.id == data["prediction_id"]).first()
        assert prediction_in_db is not None
        assert prediction_in_db.user_id == user_id
        assert prediction_in_db.label == data["risk_level"]

        recs_in_db = db.query(Recommendation).filter(Recommendation.prediction_id == data["prediction_id"]).all()
        assert len(recs_in_db) > 0


def test_predict_endpoint_low_risk():
    client = TestClient(app)

    # Login to receive JWT token
    login_res = client.post(
        "/auth/login",
        json={"username": "demo", "password": "DemoPass123!"},
    )
    token = login_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    payload = {
        "duty_hours_day": 7.0,
        "weekly_duty_hours": 35.0,
        "deployment_days": 0,
        "night_shifts": 0,
        "workload_score": 3.0,
        "training_load": 4.0,
        "transfer_frequency": 0,
        "sleep_hours": 8.5,
        "leave_days": 15,
        "rest_days": 6,
        "self_reported_stress": 2.0,
        "fatigue_score": 2.5,
        "mood_score": 9.0,
        "social_support_score": 8.5,
    }

    response = client.post("/prediction/predict", json=payload, headers=headers)
    assert response.status_code == 200

    data = response.json()
    assert data["is_medical_diagnosis"] is False
    assert data["risk_level"] in ["LOW", "MODERATE", "HIGH"]
