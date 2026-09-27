import pytest
from fastapi.testclient import TestClient
from sqlalchemy import inspect
from app.database import Base, SessionLocal, engine
from app.auth.service import ensure_seed_data
from app.main import app
from app.models import (
    Assessment,
    AuditLog,
    Consent,
    DutyRecord,
    Intervention,
    Personnel,
    Prediction,
    Recommendation,
    User,
    WellnessCheckin,
)


@pytest.fixture(scope="module", autouse=True)
def setup_database():
    Base.metadata.create_all(bind=engine)
    with SessionLocal() as db:
        ensure_seed_data(db)
    yield


def test_required_tables_exist():
    inspector = inspect(engine)
    tables = set(inspector.get_table_names())
    required_tables = {
        "users",
        "personnel",
        "wellness_checkins",
        "assessments",
        "duty_records",
        "predictions",
        "recommendations",
        "interventions",
        "consents",
        "audit_logs",
    }
    assert required_tables.issubset(tables), f"Missing tables: {required_tables - tables}"


def test_seed_users_for_all_four_roles():
    with SessionLocal() as db:
        roles = ["personnel", "welfare_officer", "commander", "admin"]
        for role in roles:
            user = db.query(User).filter(User.role == role).first()
            assert user is not None, f"Seed user with role '{role}' not found"
            assert user.personnel is not None, f"Personnel profile for role '{role}' not found"
            assert user.personnel.employee_id is not None
            assert user.personnel.full_name is not None


def test_orm_relationships_and_cascade():
    with SessionLocal() as db:
        # Fetch personnel user
        user = db.query(User).filter(User.username == "demo").first()
        assert user is not None

        # Check seeded check-in, consent, duty record
        assert len(user.wellness_checkins) >= 1
        assert len(user.consents) >= 1
        assert len(user.duty_records) >= 1

        # Test adding assessment, prediction, recommendation, intervention, audit log
        assessment = Assessment(
            user_id=user.id,
            assessment_type="PHQ-9",
            answers={"q1": 1, "q2": 0},
            score=1.0,
        )
        db.add(assessment)
        db.flush()

        prediction = Prediction(
            user_id=user.id,
            assessment_id=assessment.id,
            model_version="v1.0.0-demo",
            label="Low Risk",
            probability=0.12,
        )
        db.add(prediction)
        db.flush()

        recommendation = Recommendation(
            user_id=user.id,
            prediction_id=prediction.id,
            title="Daily Mindfulness",
            content="Practice 5 minutes of mindful breathing.",
        )
        db.add(recommendation)

        intervention = Intervention(
            user_id=user.id,
            intervention_type="Peer Support",
            status="requested",
            details="Synthetic support request test",
        )
        db.add(intervention)

        audit_log = AuditLog(
            user_id=user.id,
            action="TEST_ACTION",
            resource="test_resource",
            details={"test": True},
        )
        db.add(audit_log)

        db.commit()

        # Re-query user and assert relationships
        db.refresh(user)
        assert len(user.assessments) >= 1
        assert len(user.predictions) >= 1
        assert len(user.recommendations) >= 1
        assert len(user.interventions) >= 1
        assert len(user.audit_logs) >= 1


def test_health_endpoint():
    client = TestClient(app)
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok", "database": "connected"}
