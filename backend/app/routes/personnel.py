"""
Personnel Router
================
Endpoints for retrieving personnel profile information.
"""

from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.auth.dependencies import (
    ROLE_ADMIN,
    ROLE_COMMANDER,
    ROLE_WELFARE_OFFICER,
    get_current_user,
    require_roles,
)
from app.database import get_db
from app.models.entities import Personnel, User
from app.schemas.personnel import PersonnelResponse

router = APIRouter(prefix="/personnel", tags=["Personnel"])


@router.get("/profile", response_model=PersonnelResponse, summary="Get Current User Personnel Profile")
def get_profile(
    current_user: User = Depends(get_current_user), db: Session = Depends(get_db)
) -> Personnel:
    """Returns the personnel profile for the authenticated user."""
    profile = db.query(Personnel).filter(Personnel.user_id == current_user.id).first()
    if not profile:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Personnel profile not found")
    return profile


@router.get(
    "/list",
    response_model=List[PersonnelResponse],
    summary="List Unit Personnel Directory (Welfare Officer, Commander & Admin Only)",
)
def list_personnel(
    current_user: User = Depends(require_roles([ROLE_WELFARE_OFFICER, ROLE_COMMANDER, ROLE_ADMIN])),
    db: Session = Depends(get_db),
) -> List[Personnel]:
    """Returns operational personnel directory for authorized welfare/command roles."""
    return db.query(Personnel).all()
