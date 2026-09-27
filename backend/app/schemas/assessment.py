from datetime import datetime
from typing import Any

from pydantic import Field

from app.schemas.common import ORMModel


class AssessmentCreate(ORMModel):
    assessment_type: str = Field(min_length=2, max_length=80)
    answers: dict[str, Any] = Field(min_length=1)


class AssessmentResponse(AssessmentCreate):
    id: int
    score: float | None
    created_at: datetime
