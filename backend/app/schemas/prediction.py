"""
Pydantic Schemas for Stress Risk Prediction
============================================
"""

from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field, model_validator


class FeatureContribution(BaseModel):
    feature: str
    display_name: str
    value: float
    impact_score: float
    description: str


class PredictionRequest(BaseModel):
    duty_hours_day: Optional[float] = Field(default=None, ge=0.0, le=24.0, description="Average daily duty hours")
    duty_hours: Optional[float] = Field(default=None, ge=0.0, le=24.0, description="Alias for duty_hours_day")
    weekly_duty_hours: float = Field(default=40.0, ge=0.0, le=168.0)
    deployment_days: int = Field(default=0, ge=0)
    night_shifts: int = Field(default=0, ge=0)
    workload_score: float = Field(default=5.0, ge=1.0, le=10.0)
    training_load: float = Field(default=5.0, ge=1.0, le=10.0)
    transfer_frequency: int = Field(default=0, ge=0)
    sleep_hours: float = Field(default=7.0, ge=0.0, le=24.0)
    leave_days: int = Field(default=10, ge=0)
    rest_days: int = Field(default=4, ge=0)
    self_reported_stress: Optional[float] = Field(default=None, ge=1.0, le=10.0)
    stress_score: Optional[float] = Field(default=None, ge=1.0, le=10.0, description="Alias for self_reported_stress")
    fatigue_score: float = Field(default=5.0, ge=1.0, le=10.0)
    mood_score: float = Field(default=5.0, ge=1.0, le=10.0)
    social_support_score: float = Field(default=5.0, ge=1.0, le=10.0)

    user_id: Optional[int] = Field(default=None, description="Optional DB user ID to link prediction")
    assessment_id: Optional[int] = Field(default=None, description="Optional DB assessment ID")

    @model_validator(mode="before")
    @classmethod
    def resolve_aliases(cls, values: Any) -> Any:
        if isinstance(values, dict):
            # Resolve duty_hours alias
            if values.get("duty_hours_day") is None and values.get("duty_hours") is not None:
                values["duty_hours_day"] = values["duty_hours"]
            elif values.get("duty_hours_day") is None:
                values["duty_hours_day"] = 8.0

            # Resolve stress_score alias
            if values.get("self_reported_stress") is None and values.get("stress_score") is not None:
                values["self_reported_stress"] = values["stress_score"]
            elif values.get("self_reported_stress") is None:
                values["self_reported_stress"] = 5.0
        return values

    def to_feature_dict(self) -> Dict[str, Any]:
        return {
            "duty_hours_day": self.duty_hours_day if self.duty_hours_day is not None else 8.0,
            "weekly_duty_hours": self.weekly_duty_hours,
            "deployment_days": self.deployment_days,
            "night_shifts": self.night_shifts,
            "workload_score": self.workload_score,
            "training_load": self.training_load,
            "transfer_frequency": self.transfer_frequency,
            "sleep_hours": self.sleep_hours,
            "leave_days": self.leave_days,
            "rest_days": self.rest_days,
            "self_reported_stress": self.self_reported_stress if self.self_reported_stress is not None else 5.0,
            "fatigue_score": self.fatigue_score,
            "mood_score": self.mood_score,
            "social_support_score": self.social_support_score,
        }


class PredictionResponse(BaseModel):
    prediction_id: Optional[int] = None
    risk_level: str = Field(description="Risk assessment category: LOW, MODERATE, HIGH")
    risk_score: float = Field(description="Model prediction confidence score")
    confidence: float = Field(description="Confidence level")
    top_contributing_factors: List[FeatureContribution] = Field(default_factory=list)
    welfare_recommendations: List[str] = Field(default_factory=list)
    model_version: str = Field(description="Timestamp/version identifier of the model")
    is_medical_diagnosis: bool = Field(default=False, description="Flag indicating system is a non-clinical screening prototype")
    disclaimer: str = Field(
        default="This system is a welfare-risk screening prototype for operational decision support. It is NOT a medical diagnosis tool."
    )
