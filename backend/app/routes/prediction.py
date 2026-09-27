"""
Prediction API Router
=====================
Exposes POST /prediction/predict endpoint for stress risk prediction and GET /prediction/history for past predictions.
Requires JWT authentication.
"""

from typing import Any, Dict, List
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.auth.dependencies import get_current_user
from app.database import get_db
from app.models.entities import Prediction, User
from app.schemas.prediction import PredictionRequest, PredictionResponse
from app.services.prediction_service import predict_and_store

router = APIRouter(prefix="/prediction", tags=["Prediction"])


@router.post(
    "/predict",
    response_model=PredictionResponse,
    summary="Predict Welfare Stress Risk (Authenticated Users)",
)
def predict_stress_risk(
    payload: PredictionRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> PredictionResponse:
    """
    Evaluates personnel wellness risk levels (LOW, MODERATE, HIGH) using the trained ML model.
    Requires JWT authentication. Automatically links prediction to current_user if user_id is omitted.
    """
    if payload.user_id is None:
        payload.user_id = current_user.id
    return predict_and_store(payload, db=db)


@router.get(
    "/history",
    summary="View Own Prediction History",
)
def prediction_history(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> List[Dict[str, Any]]:
    """Returns past prediction records for the authenticated user."""
    records = (
        db.query(Prediction)
        .filter(Prediction.user_id == current_user.id)
        .order_by(Prediction.created_at.desc())
        .all()
    )
    return [
        {
            "prediction_id": rec.id,
            "user_id": rec.user_id,
            "assessment_id": rec.assessment_id,
            "model_version": rec.model_version,
            "risk_level": rec.label,
            "probability": rec.probability,
            "created_at": rec.created_at.isoformat() if rec.created_at else None,
        }
        for rec in records
    ]
