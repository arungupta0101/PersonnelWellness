"""
Support Router
==============
Provides endpoints for requesting welfare interventions and support resources.
"""

from typing import List
from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.auth.dependencies import (
    ROLE_ADMIN,
    ROLE_PERSONNEL,
    ROLE_WELFARE_OFFICER,
    get_current_user,
    require_roles,
)
from app.database import get_db
from app.models.entities import Intervention, User
from app.schemas.support import SupportRequestCreate, SupportRequestResponse

router = APIRouter(prefix="/support", tags=["Support"])


@router.post(
    "/request",
    response_model=SupportRequestResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Request Welfare Support / Peer Intervention",
)
def request_support(
    payload: SupportRequestCreate,
    current_user: User = Depends(require_roles([ROLE_PERSONNEL, ROLE_WELFARE_OFFICER, ROLE_ADMIN])),
    db: Session = Depends(get_db),
) -> Intervention:
    """Personnel endpoint for requesting welfare support or peer interventions."""
    intervention = Intervention(user_id=current_user.id, **payload.model_dump())
    db.add(intervention)
    db.commit()
    db.refresh(intervention)
    return intervention


@router.get(
    "/interventions",
    response_model=List[SupportRequestResponse],
    summary="List Active Welfare Interventions (Welfare Officer & Admin Only)",
)
def list_interventions(
    current_user: User = Depends(require_roles([ROLE_WELFARE_OFFICER, ROLE_ADMIN])),
    db: Session = Depends(get_db),
) -> List[Intervention]:
    """Welfare Officer and Admin endpoint for managing support requests."""
    return db.query(Intervention).all()
