"""
Consent Schemas
===============
"""

from datetime import datetime
from typing import Optional
from pydantic import BaseModel, Field


class ConsentStatusResponse(BaseModel):
    user_id: int
    consent_type: str = Field(default="data_sharing_wellness")
    granted: bool
    granted_at: Optional[datetime] = None

    class Config:
        from_attributes = True


class ConsentUpdateRequest(BaseModel):
    consent_type: str = Field(default="data_sharing_wellness")
