"""
Consent Management Router
=========================
Enables personnel to grant, revoke, and check their data sharing consent status.
"""

from datetime import datetime, timezone
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.auth.dependencies import ROLE_PERSONNEL, require_roles
from app.database import get_db
from app.models.entities import Consent, User
from app.schemas.consent import ConsentStatusResponse, ConsentUpdateRequest
from app.services.audit_service import log_audit

router = APIRouter(prefix="/consent", tags=["Consent Management"])


@router.get("/status", response_model=ConsentStatusResponse, summary="Get Current User Data Sharing Consent Status")
def get_consent_status(
    consent_type: str = "data_sharing_wellness",
    current_user: User = Depends(require_roles([ROLE_PERSONNEL])),
    db: Session = Depends(get_db),
) -> ConsentStatusResponse:
    """Returns the current data sharing consent state for the authenticated personnel."""
    consent = (
        db.query(Consent)
        .filter(Consent.user_id == current_user.id, Consent.consent_type == consent_type)
        .first()
    )
    if not consent:
        return ConsentStatusResponse(
            user_id=current_user.id,
            consent_type=consent_type,
            granted=False,
            granted_at=None,
        )
    return ConsentStatusResponse(
        user_id=current_user.id,
        consent_type=consent.consent_type,
        granted=consent.granted,
        granted_at=consent.granted_at,
    )


@router.post("/grant", response_model=ConsentStatusResponse, summary="Grant Data Sharing Consent")
def grant_consent(
    payload: ConsentUpdateRequest = ConsentUpdateRequest(),
    current_user: User = Depends(require_roles([ROLE_PERSONNEL])),
    db: Session = Depends(get_db),
) -> ConsentStatusResponse:
    """Grants data sharing consent for wellness and assessment visibility."""
    consent = (
        db.query(Consent)
        .filter(Consent.user_id == current_user.id, Consent.consent_type == payload.consent_type)
        .first()
    )
    if not consent:
        consent = Consent(
            user_id=current_user.id,
            consent_type=payload.consent_type,
            granted=True,
            granted_at=datetime.now(timezone.utc),
        )
        db.add(consent)
    else:
        consent.granted = True
        consent.granted_at = datetime.now(timezone.utc)
        db.add(consent)

    db.commit()
    db.refresh(consent)

    log_audit(
        db=db,
        user_id=current_user.id,
        action="GRANT_DATA_SHARING_CONSENT",
        resource=f"user:{current_user.id}",
        status="SUCCESS",
        details={"consent_type": payload.consent_type},
    )

    return ConsentStatusResponse(
        user_id=current_user.id,
        consent_type=consent.consent_type,
        granted=consent.granted,
        granted_at=consent.granted_at,
    )


@router.post("/revoke", response_model=ConsentStatusResponse, summary="Revoke Data Sharing Consent")
def revoke_consent(
    payload: ConsentUpdateRequest = ConsentUpdateRequest(),
    current_user: User = Depends(require_roles([ROLE_PERSONNEL])),
    db: Session = Depends(get_db),
) -> ConsentStatusResponse:
    """Revokes data sharing consent, preventing Welfare Officers from viewing individual wellness records."""
    consent = (
        db.query(Consent)
        .filter(Consent.user_id == current_user.id, Consent.consent_type == payload.consent_type)
        .first()
    )
    if not consent:
        consent = Consent(
            user_id=current_user.id,
            consent_type=payload.consent_type,
            granted=False,
            granted_at=datetime.now(timezone.utc),
        )
        db.add(consent)
    else:
        consent.granted = False
        consent.granted_at = datetime.now(timezone.utc)
        db.add(consent)

    db.commit()
    db.refresh(consent)

    log_audit(
        db=db,
        user_id=current_user.id,
        action="REVOKE_DATA_SHARING_CONSENT",
        resource=f"user:{current_user.id}",
        status="SUCCESS",
        details={"consent_type": payload.consent_type},
    )

    return ConsentStatusResponse(
        user_id=current_user.id,
        consent_type=consent.consent_type,
        granted=consent.granted,
        granted_at=consent.granted_at,
    )
