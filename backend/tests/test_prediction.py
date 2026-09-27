"""
Integration Tests for Stress Risk Prediction API Endpoint & ModelLoader (SIH 2026)
=====================================================================================
Tests:
A. ModelLoader can load all production artifacts.
B. ModelLoader.predict() returns risk_level, risk_score, confidence.
C. ModelLoader.explain() returns feature contributions.
D. POST /prediction/predict returns HTTP 200.
E. Response validates against PredictionResponse schema.
F. A Prediction record is stored in DB for an authenticated user.
G. GET /health/ml returns HTTP 200 with model_loaded=True.
"""

import pytest
from fastapi.testclient import TestClient

from app.auth.service import ensure_seed_data
from app.database import Base, SessionLocal, engine
from app.main import app
from app.ml.model_loader import get_model_loader
from app.models.entities import Prediction, Recommendation, User
from app.schemas.prediction import PredictionResponse


@pytest.fixture(scope="module", autouse=True)
def setup_database():
    Base.metadata.create_all(bind=engine)
    with SessionLocal() as db:
        ensure_seed_data(db)
    yield


def test_task4_a_model_loader_artifacts_loaded():
    loader = get_model_loader()
    assert loader is not None
    is_ready, msg = loader.is_ready()
    assert is_ready is True
    assert loader.model_name in ["Gradient Boosting", "Random Forest", "Logistic Regression"]
    assert loader.pipeline is not None or loader.model is not None


def test_task4_b_c_model_loader_predict_and_explain():
    loader = get_model_loader()
    sample_data = {
        "duty_hours_day": 14.0,
        "weekly_duty_hours": 75.0,
        "deployment_days": 120,
        "night_shifts": 10,
        "workload_score": 9.0,
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

    # B. Predict
    pred_res = loader.predict(sample_data)
    assert pred_res["risk_level"] in ["LOW", "MODERATE", "HIGH"]
    assert isinstance(pred_res["risk_score"], float)
    assert isinstance(pred_res["confidence"], float)

    # C. Explain
    factors = loader.explain(sample_data, top_k=5)
    assert isinstance(factors, list)
    assert len(factors) > 0
    assert "feature" in factors[0]
    assert "impact_score" in factors[0]


def test_task4_d_e_f_predict_endpoint_and_db_persistence():
    client = TestClient(app)

    # Login to receive JWT token
    login_res = client.post(
        "/auth/login",
        json={"username": "demo", "password": "DemoPass123!"},
    )
    assert login_res.status_code == 200, login_res.text
    token = login_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    with SessionLocal() as db:
        user = db.query(User).filter(User.username == "demo").first()
        assert user is not None
        user_id = user.id

    payload = {
        "duty_hours_day": 12.0,
        "weekly_duty_hours": 70.0,
        "deployment_days": 45,
        "night_shifts": 8,
        "workload_score": 9.0,
        "training_load": 8.0,
        "transfer_frequency": 2,
        "sleep_hours": 4.0,
        "leave_days": 1,
        "rest_days": 1,
        "self_reported_stress": 9.0,
        "fatigue_score": 9.0,
        "mood_score": 2.0,
        "social_support_score": 3.0,
        "user_id": user_id,
    }

    # D. POST /prediction/predict returns HTTP 200
    response = client.post("/prediction/predict", json=payload, headers=headers)
    assert response.status_code == 200, response.text
    data = response.json()

    # E. Validates against PredictionResponse schema
    validated_response = PredictionResponse(**data)
    assert validated_response.risk_level in ["LOW", "MODERATE", "HIGH"]
    assert validated_response.prediction_id is not None

    # F. Prediction record stored in DB for authenticated user
    with SessionLocal() as db:
        prediction_in_db = db.query(Prediction).filter(Prediction.id == data["prediction_id"]).first()
        assert prediction_in_db is not None
        assert prediction_in_db.user_id == user_id
        assert prediction_in_db.label == data["risk_level"]


def test_task5_health_ml_endpoint():
    client = TestClient(app)
    res = client.get("/health/ml")
    assert res.status_code == 200
    data = res.json()
    assert data["status"] == "ok"
    assert data["model_loaded"] is True
    assert "model_name" in data
    assert "model_version" in data
