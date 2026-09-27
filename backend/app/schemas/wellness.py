from datetime import datetime

from pydantic import Field

from app.schemas.common import ORMModel


class WellnessCheckinCreate(ORMModel):
    mood: int = Field(ge=1, le=10)
    stress_level: int = Field(ge=1, le=10)
    sleep_hours: float = Field(ge=0, le=24)
    notes: str | None = Field(default=None, max_length=2000)


class WellnessCheckinResponse(WellnessCheckinCreate):
    id: int
    created_at: datetime
