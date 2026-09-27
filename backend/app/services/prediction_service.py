"""
Prediction Service
==================
Handles ML model execution, SHAP/tree-based feature explanations, recommendation generation,
and database persistence for stress risk predictions.
"""

from typing import List, Optional
from sqlalchemy.orm import Session

from app.ml.model_loader import get_model_loader
from app.models.entities import Prediction, Recommendation, User
from app.schemas.prediction import FeatureContribution, PredictionRequest, PredictionResponse


def generate_recommendations(risk_level: str, top_factors: List[FeatureContribution]) -> List[str]:
    recs = []

    factor_names = [f.feature for f in top_factors]

    if risk_level == "HIGH":
        recs.append("Immediate Welfare Review: Schedule a 1-on-1 check-in with your assigned Welfare Officer.")
        if "sleep_hours" in factor_names or "fatigue_score" in factor_names:
            recs.append("Mandatory Recovery Window: Enforce minimum 8-hour continuous rest prior to next operational shift.")
        if "duty_hours_day" in factor_names or "weekly_duty_hours" in factor_names:
            recs.append("Workload Adjustment: Recommend temporary shift duration capping or duty reassignment.")
        if "night_shifts" in factor_names:
            recs.append("Night Shift Rotation: Limit consecutive night duties to maximum 2 shifts.")

    elif risk_level == "MODERATE":
        recs.append("Proactive Wellness Check: Review recent duty patterns and track daily fatigue scores.")
        if "sleep_hours" in factor_names:
            recs.append("Sleep Hygiene Optimization: Aim for 7-8 hours of regular uninterrupted sleep.")
        if "rest_days" in factor_names or "leave_days" in factor_names:
            recs.append("Leave Scheduling: Plan accumulated rest days over the coming 14 days.")
        if "self_reported_stress" in factor_names or "mood_score" in factor_names:
            recs.append("Peer Support Engagement: Utilize available departmental stress relief & peer support resources.")

    else:  # LOW
        recs.append("Maintain Optimal Balance: Continue current duty-rest balance and healthy sleep routine.")
        recs.append("Routine Logging: Submit periodic wellness check-ins to monitor long-term trends.")

    # Fallback default rec if list is small
    if len(recs) < 2:
        recs.append("General Support: Access wellness resources via the Personnel Support Console.")

    return recs[:4]


def predict_and_store(
    request: PredictionRequest, db: Optional[Session] = None
) -> PredictionResponse:
    loader = get_model_loader()
    feature_dict = request.to_feature_dict()

    # 1. Run model prediction
    pred_res = loader.predict(feature_dict)
    risk_level = pred_res["risk_level"]
    risk_score = pred_res["risk_score"]
    confidence = pred_res["confidence"]

    # 2. Get top tree feature explanations
    raw_factors = loader.explain(feature_dict, top_k=5)
    top_factors = [FeatureContribution(**f) for f in raw_factors]

    # 3. Generate welfare recommendations
    recommendations = generate_recommendations(risk_level, top_factors)

    db_prediction_id = None

    # 4. Store in DB if DB session and valid user_id provided
    if db is not None and request.user_id is not None:
        user_exists = db.query(User).filter(User.id == request.user_id).first()
        if user_exists:
            prediction_record = Prediction(
                user_id=request.user_id,
                assessment_id=request.assessment_id,
                model_version=loader.model_version,
                label=risk_level,
                probability=risk_score,
            )
            db.add(prediction_record)
            db.commit()
            db.refresh(prediction_record)
            db_prediction_id = prediction_record.id

            # Save recommendations
            for rec_text in recommendations:
                rec_record = Recommendation(
                    user_id=request.user_id,
                    prediction_id=db_prediction_id,
                    title=f"Welfare Action ({risk_level} Risk)",
                    content=rec_text,
                    is_completed=False,
                )
                db.add(rec_record)
            db.commit()

    return PredictionResponse(
        prediction_id=db_prediction_id,
        risk_level=risk_level,
        risk_score=risk_score,
        confidence=confidence,
        top_contributing_factors=top_factors,
        welfare_recommendations=recommendations,
        model_version=loader.model_version,
        is_medical_diagnosis=False,
        disclaimer=(
            "This system is a welfare-risk screening prototype for operational decision support. "
            "It is NOT a medical diagnosis tool."
        ),
    )
