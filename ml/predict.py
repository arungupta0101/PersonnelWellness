"""
Inference Script for Personnel Wellness Risk Prediction
======================================================
Loads trained pipeline and label encoder to predict stress risk levels (LOW, MODERATE, HIGH)
along with probability distributions.

DISCLAIMER:
This model is a welfare-risk screening prototype for demonstration purposes.
It is NOT clinically validated and is NOT a medical diagnosis system.
"""

from typing import Any, Dict, Union
from pathlib import Path
import joblib
import pandas as pd

BASE_DIR = Path(__file__).resolve().parent
MODELS_DIR = BASE_DIR / "models"

PIPELINE_FILE = MODELS_DIR / "pipeline.joblib"
LABEL_ENCODER_FILE = MODELS_DIR / "label_encoder.joblib"
METADATA_FILE = MODELS_DIR / "model_metadata.json"

FEATURE_COLUMNS = [
    "duty_hours_day",
    "weekly_duty_hours",
    "deployment_days",
    "night_shifts",
    "workload_score",
    "training_load",
    "transfer_frequency",
    "sleep_hours",
    "leave_days",
    "rest_days",
    "self_reported_stress",
    "fatigue_score",
    "mood_score",
    "social_support_score",
]

DISCLAIMER = (
    "This system is a welfare-risk screening prototype for demonstration purposes. "
    "It is NOT clinically validated and is NOT a medical diagnosis system."
)


class WellnessRiskPredictor:

    def __init__(self):
        if not PIPELINE_FILE.exists():
            from train import train_and_select_model
            train_and_select_model()

        self.pipeline = joblib.load(PIPELINE_FILE)
        self.label_encoder = joblib.load(LABEL_ENCODER_FILE)

    def predict(self, sample_data: Union[Dict[str, Any], pd.DataFrame]) -> Dict[str, Any]:
        if isinstance(sample_data, dict):
            df = pd.DataFrame([sample_data])
        elif isinstance(sample_data, pd.DataFrame):
            df = sample_data.copy()
        else:
            raise ValueError("Input sample_data must be a dictionary or pandas DataFrame.")

        # Ensure all required features are present
        for col in FEATURE_COLUMNS:
            if col not in df.columns:
                raise ValueError(f"Missing required feature: '{col}'")

        X = df[FEATURE_COLUMNS]

        # Predict encoded class index and class probabilities
        pred_encoded = self.pipeline.predict(X)[0]
        probs = self.pipeline.predict_proba(X)[0]

        predicted_label = str(self.label_encoder.inverse_transform([pred_encoded])[0])
        class_names = [str(c) for c in self.label_encoder.classes_]

        prob_dict = {class_name: round(float(prob), 4) for class_name, prob in zip(class_names, probs)}

        return {
            "predicted_risk_level": predicted_label,
            "class_probabilities": prob_dict,
            "disclaimer": DISCLAIMER,
            "is_prototype": True,
        }


def predict_wellness_risk(sample_data: Dict[str, Any]) -> Dict[str, Any]:
    predictor = WellnessRiskPredictor()
    return predictor.predict(sample_data)


if __name__ == "__main__":
    sample = {
        "duty_hours_day": 12.0,
        "weekly_duty_hours": 70.0,
        "deployment_days": 45,
        "night_shifts": 8,
        "workload_score": 8.5,
        "training_load": 7.0,
        "transfer_frequency": 2,
        "sleep_hours": 4.5,
        "leave_days": 2,
        "rest_days": 1,
        "self_reported_stress": 8.0,
        "fatigue_score": 8.5,
        "mood_score": 3.0,
        "social_support_score": 4.0,
    }

    res = predict_wellness_risk(sample)
    print("Sample Inference Result:")
    print(res)
