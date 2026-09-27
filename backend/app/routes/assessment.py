"""
Assessment API Router
=====================
Provides endpoints for psychological and wellness assessments.
Enforces strict RBAC:
- Personnel: Submit and view own assessments.
- Welfare Officer: View individual assessments with consent check and audit logging.
- Commander: View aggregated unit-level summary only (no individual PII).
"""

from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.auth.dependencies import (
    ROLE_ADMIN,
    ROLE_COMMANDER,
    ROLE_PERSONNEL,
    ROLE_WELFARE_OFFICER,
    require_roles,
)
from app.database import get_db
from app.models.entities import Assessment, Consent, Prediction, User
from app.schemas.assessment import AssessmentCreate, AssessmentResponse
from app.services.audit_service import log_audit

router = APIRouter(prefix="/assessment", tags=["Assessments"])


@router.post(
    "/submit",
    response_model=AssessmentResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Submit Assessment Answers",
)
def submit_assessment(
    payload: AssessmentCreate,
    current_user: User = Depends(require_roles([ROLE_PERSONNEL, ROLE_ADMIN])),
    db: Session = Depends(get_db),
) -> Assessment:
    """Personnel endpoint for submitting self-assessments."""
    assessment = Assessment(
        user_id=current_user.id,
        answers=payload.answers,
        assessment_type=payload.assessment_type,
    )
    db.add(assessment)
    db.commit()
    db.refresh(assessment)
    return assessment


@router.get(
    "/history",
    response_model=List[AssessmentResponse],
    summary="View Own Assessment History",
)
def assessment_history(
    current_user: User = Depends(require_roles([ROLE_PERSONNEL, ROLE_WELFARE_OFFICER, ROLE_ADMIN])),
    db: Session = Depends(get_db),
) -> List[Assessment]:
    """Personnel endpoint returning personal assessment history."""
    return (
        db.query(Assessment)
        .filter(Assessment.user_id == current_user.id)
        .order_by(Assessment.created_at.desc())
        .all()
    )


@router.get(
    "/personnel/{target_user_id}",
    response_model=List[AssessmentResponse],
    summary="View Individual Personnel Assessment (Welfare Officer & Admin)",
)
def view_personnel_assessments(
    target_user_id: int,
    current_user: User = Depends(require_roles([ROLE_WELFARE_OFFICER, ROLE_ADMIN])),
    db: Session = Depends(get_db),
) -> List[Assessment]:
    """Welfare Officer & Admin endpoint for viewing individual personnel assessments with consent check and audit logging."""
    target_user = db.query(User).filter(User.id == target_user_id).first()
    if not target_user:
        log_audit(
            db=db,
            user_id=current_user.id,
            action="SENSITIVE_ACCESS_ASSESSMENTS",
            resource=f"user:{target_user_id}",
            status="FAILURE",
            details={"reason": "Target user not found"},
        )
        raise HTTPException(status_code=404, detail="Personnel user not found")

    # Enforce consent for Welfare Officer role
    if current_user.role == ROLE_WELFARE_OFFICER:
        consent = (
            db.query(Consent)
            .filter(
                Consent.user_id == target_user_id,
                Consent.consent_type == "data_sharing_wellness",
                Consent.granted.is_(True),
            )
            .first()
        )
        if not consent:
            log_audit(
                db=db,
                user_id=current_user.id,
                action="SENSITIVE_ACCESS_ASSESSMENTS",
                resource=f"user:{target_user_id}",
                status="FAILURE",
                details={"reason": "Consent not granted by target personnel"},
            )
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Personnel has not granted data sharing consent",
            )

    log_audit(
        db=db,
        user_id=current_user.id,
        action="SENSITIVE_ACCESS_ASSESSMENTS",
        resource=f"user:{target_user_id}",
        status="SUCCESS",
        details={"accessed_by": current_user.username, "role": current_user.role},
    )

    return (
        db.query(Assessment)
        .filter(Assessment.user_id == target_user_id)
        .order_by(Assessment.created_at.desc())
        .all()
    )


@router.get(
    "/unit-summary",
    summary="View Aggregated Unit Assessment Summary (Commander & Admin)",
)
def view_unit_summary(
    current_user: User = Depends(require_roles([ROLE_COMMANDER, ROLE_ADMIN])),
    db: Session = Depends(get_db),
):
    """
    Commander & Admin endpoint returning strictly aggregated unit-level metrics.
    Excludes all personal identifiers (names, employee IDs, individual responses).
    """
    total_assessments = db.query(func.count(Assessment.id)).scalar() or 0
    total_personnel = db.query(func.count(User.id)).filter(User.role == ROLE_PERSONNEL).scalar() or 0

    # Risk distributions from predictions table
    high_risk_count = db.query(func.count(Prediction.id)).filter(Prediction.label == "HIGH").scalar() or 0
    moderate_risk_count = db.query(func.count(Prediction.id)).filter(Prediction.label == "MODERATE").scalar() or 0
    low_risk_count = db.query(func.count(Prediction.id)).filter(Prediction.label == "LOW").scalar() or 0

    return {
        "unit_name": "Operations Command Sector 4",
        "total_active_personnel": total_personnel,
        "total_assessments_completed": total_assessments,
        "aggregate_risk_distribution": {
            "HIGH": high_risk_count,
            "MODERATE": moderate_risk_count,
            "LOW": low_risk_count,
        },
        "anonymized_disclaimer": "This report presents aggregated unit data for operational planning.",
    }
