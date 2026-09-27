"""
Wellness API Router
====================
Provides endpoints for personnel wellness check-ins with server-side rate limiting (1 check-in per calendar day).
"""

from datetime import datetime, timedelta, timezone
from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.auth.dependencies import (
    ROLE_ADMIN,
    ROLE_PERSONNEL,
    ROLE_WELFARE_OFFICER,
    require_roles,
)
from app.database import get_db
from app.models.entities import Consent, User, WellnessCheckin
from app.schemas.wellness import WellnessCheckinCreate, WellnessCheckinResponse
from app.services.audit_service import log_audit

router = APIRouter(prefix="/wellness", tags=["Wellness"])


@router.post(
    "/checkin",
    response_model=WellnessCheckinResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Submit Daily Wellness Check-in",
)
def create_checkin(
    payload: WellnessCheckinCreate,
    current_user: User = Depends(require_roles([ROLE_PERSONNEL, ROLE_ADMIN])),
    db: Session = Depends(get_db),
) -> WellnessCheckin:
    """
    Personnel endpoint for creating personal daily wellness check-ins.
    Enforces server-side rule: Maximum 1 check-in per calendar day per user.
    """
    now_utc = datetime.now(timezone.utc)
    start_of_day = now_utc.replace(hour=0, minute=0, second=0, microsecond=0)
    end_of_day = start_of_day + timedelta(days=1)

    existing_today = (
        db.query(WellnessCheckin)
        .filter(
            WellnessCheckin.user_id == current_user.id,
            WellnessCheckin.created_at >= start_of_day,
            WellnessCheckin.created_at < end_of_day,
        )
        .first()
    )
    if existing_today:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Today's wellness check-in has already been submitted.",
        )

    checkin = WellnessCheckin(user_id=current_user.id, **payload.model_dump())
    db.add(checkin)
    db.commit()
    db.refresh(checkin)
    return checkin


@router.get(
    "/history",
    response_model=List[WellnessCheckinResponse],
    summary="View Personal Wellness Check-in History",
)
def wellness_history(
    current_user: User = Depends(require_roles([ROLE_PERSONNEL, ROLE_WELFARE_OFFICER, ROLE_ADMIN])),
    db: Session = Depends(get_db),
) -> List[WellnessCheckin]:
    """Personnel endpoint returning own check-in history."""
    return (
        db.query(WellnessCheckin)
        .filter(WellnessCheckin.user_id == current_user.id)
        .order_by(WellnessCheckin.created_at.desc())
        .all()
    )


@router.get(
    "/personnel/{target_user_id}",
    response_model=List[WellnessCheckinResponse],
    summary="View Personnel Wellness Check-ins (Welfare Officer & Admin Only)",
)
def view_personnel_checkins(
    target_user_id: int,
    current_user: User = Depends(require_roles([ROLE_WELFARE_OFFICER, ROLE_ADMIN])),
    db: Session = Depends(get_db),
) -> List[WellnessCheckin]:
    """
    Welfare Officer & Admin endpoint for viewing individual personnel wellness check-ins.
    Enforces data sharing consent check for Welfare Officers.
    Audit log entry is recorded for sensitive data access.
    """
    target_user = db.query(User).filter(User.id == target_user_id).first()
    if not target_user:
        log_audit(
            db=db,
            user_id=current_user.id,
            action="SENSITIVE_ACCESS_WELLNESS_CHECKINS",
            resource=f"user:{target_user_id}",
            status="FAILURE",
            details={"reason": "Target user not found"},
        )
        raise HTTPException(status_code=404, detail="Target personnel user not found")

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
                action="SENSITIVE_ACCESS_WELLNESS_CHECKINS",
                resource=f"user:{target_user_id}",
                status="FAILURE",
                details={"reason": "Consent not granted by target personnel"},
            )
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Personnel has not granted data sharing consent",
            )

    # Log successful sensitive data access
    log_audit(
        db=db,
        user_id=current_user.id,
        action="SENSITIVE_ACCESS_WELLNESS_CHECKINS",
        resource=f"user:{target_user_id}",
        status="SUCCESS",
        details={"accessed_by": current_user.username, "role": current_user.role},
    )

    return (
        db.query(WellnessCheckin)
        .filter(WellnessCheckin.user_id == target_user_id)
        .order_by(WellnessCheckin.created_at.desc())
        .all()
    )
