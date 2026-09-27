"""
FastAPI Security & RBAC Dependencies
=====================================
Provides JWT authentication dependencies and role-based authorization checks.
"""

from typing import Callable, List, Optional
from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from sqlalchemy.orm import Session

from app.auth.security import decode_access_token
from app.database import get_db
from app.models.entities import User

# Role Constants
ROLE_PERSONNEL = "personnel"
ROLE_WELFARE_OFFICER = "welfare_officer"
ROLE_COMMANDER = "commander"
ROLE_ADMIN = "admin"

ALL_ROLES = [ROLE_PERSONNEL, ROLE_WELFARE_OFFICER, ROLE_COMMANDER, ROLE_ADMIN]

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/auth/login", auto_error=False)


def get_current_user(
    token: Optional[str] = Depends(oauth2_scheme), db: Session = Depends(get_db)
) -> User:
    """Authenticates JWT token and retrieves the current active User entity."""
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Could not validate credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )

    if not token:
        raise credentials_exception

    payload = decode_access_token(token)
    if payload is None:
        raise credentials_exception

    user_id_str = payload.get("sub")
    if not user_id_str:
        raise credentials_exception

    try:
        user_id = int(user_id_str)
    except ValueError:
        raise credentials_exception

    user = db.query(User).filter(User.id == user_id).first()
    if not user or not user.is_active:
        raise credentials_exception

    return user


def require_roles(allowed_roles: List[str]) -> Callable[[User], User]:
    """Dependency factory restricting endpoint access to specific user roles."""
    def role_checker(current_user: User = Depends(get_current_user)) -> User:
        if current_user.role not in allowed_roles:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail=f"Permission denied. Required roles: {allowed_roles}, Current role: '{current_user.role}'",
            )
        return current_user

    return role_checker
