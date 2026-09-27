from datetime import datetime

from pydantic import Field

from app.schemas.common import ORMModel


class SupportRequestCreate(ORMModel):
    intervention_type: str = Field(min_length=2, max_length=80)
    details: str | None = Field(default=None, max_length=3000)


class SupportRequestResponse(SupportRequestCreate):
    id: int
    status: str
    created_at: datetime
