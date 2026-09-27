"""
Authentication Router
=====================
Handles user authentication and JWT token generation.
Supports both Swagger UI form-data (OAuth2PasswordRequestForm / Form) and JSON payloads.
"""

from typing import Optional
from fastapi import APIRouter, Depends, Form, HTTPException, Request, status
from sqlalchemy.orm import Session

from app.auth.dependencies import get_current_user
from app.auth.security import authenticate_user, create_access_token
from app.database import get_db
from app.models.entities import User
from app.schemas.common import TokenResponse

router = APIRouter(prefix="/auth", tags=["Authentication"])


@router.post("/login", response_model=TokenResponse, summary="Log in and receive a JWT access token")
async def login(
    request: Request,
    username: Optional[str] = Form(default=None),
    password: Optional[str] = Form(default=None),
    db: Session = Depends(get_db),
) -> TokenResponse:
    """
    Authenticates username and password, returning JWT access token with user details.
    Supports both Swagger UI form-data (Authorize modal) and JSON payloads.
    """
    u = username
    p = password

    # If form data not supplied, parse JSON body
    if not u or not p:
        try:
            body = await request.json()
            if isinstance(body, dict):
                u = body.get("username")
                p = body.get("password")
        except Exception:
            pass

    if not u or not p:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="Username and password are required.",
        )

    user = authenticate_user(db, u, p)
    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid username or password",
            headers={"WWW-Authenticate": "Bearer"},
        )

    token_data = {"sub": str(user.id), "username": user.username, "role": user.role}
    access_token = create_access_token(token_data)

    return TokenResponse(
        access_token=access_token,
        token_type="bearer",
        user_id=user.id,
        username=user.username,
        role=user.role,
    )


@router.get("/me", summary="Get Current Authenticated User Profile")
def get_me(current_user: User = Depends(get_current_user)):
    """Returns the authenticated user's profile information."""
    personnel_info = None
    if current_user.personnel:
        personnel_info = {
            "employee_id": current_user.personnel.employee_id,
            "full_name": current_user.personnel.full_name,
            "department": current_user.personnel.department,
            "designation": current_user.personnel.designation,
            "phone": current_user.personnel.phone,
        }

    return {
        "id": current_user.id,
        "username": current_user.username,
        "email": current_user.email,
        "role": current_user.role,
        "is_active": current_user.is_active,
        "personnel": personnel_info,
    }
