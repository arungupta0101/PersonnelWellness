from datetime import datetime
from typing import Optional
from pydantic import BaseModel, ConfigDict


class MessageResponse(BaseModel):
    message: str


class ORMModel(BaseModel):
    model_config = ConfigDict(from_attributes=True)


class HealthResponse(BaseModel):
    status: str
    database: str


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user_id: Optional[int] = None
    username: Optional[str] = None
    role: Optional[str] = None


class CreatedResponse(MessageResponse):
    id: int
    created_at: datetime
