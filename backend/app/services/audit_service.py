"""
Audit Logging Service
=====================
Logs security and sensitive data access events to the audit_logs database table.
Includes automatic redaction of confidential / sensitive fields.
"""

from typing import Any, Dict, Optional
from sqlalchemy.orm import Session

from app.models.entities import AuditLog

SENSITIVE_KEYS = {
    "password",
    "hashed_password",
    "secret",
    "token",
    "access_token",
    "answers",
    "notes",
    "stress_score",
    "phq9",
}


def sanitize_details(details: Optional[Dict[str, Any]]) -> Optional[Dict[str, Any]]:
    """Recursively redacts sensitive payload attributes from audit log details."""
    if not details:
        return details
    clean_details = {}
    for key, value in details.items():
        if key.lower() in SENSITIVE_KEYS:
            clean_details[key] = "[REDACTED]"
        elif isinstance(value, dict):
            clean_details[key] = sanitize_details(value)
        else:
            clean_details[key] = value
    return clean_details


def log_audit(
    db: Session,
    user_id: Optional[int],
    action: str,
    resource: Optional[str] = None,
    status: str = "SUCCESS",
    details: Optional[Dict[str, Any]] = None,
) -> AuditLog:
    """Records an audit log entry for security and data privacy tracking."""
    sanitized = sanitize_details(details)
    audit_record = AuditLog(
        user_id=user_id,
        action=action,
        resource=resource,
        status=status,
        details=sanitized,
    )
    db.add(audit_record)
    db.commit()
    db.refresh(audit_record)
    return audit_record
