"""
ML Model Loader Service for Personnel Wellness FastAPI Backend
================================================================
Lazily loads and caches the trained scikit-learn model, preprocessing pipeline,
label encoder, and metadata from the ML directory.

Provides prediction and tree-based feature contribution explainability.
"""

import json
from pathlib import Path
from typing import Any, Dict, List, Optional
import joblib
import numpy as np
import pandas as pd

# Default model directory path
PRIMARY_MODEL_DIR = Path("D:/Projects/PersonnelWellness/ml/models")
FALLBACK_MODEL_DIR = Path(__file__).resolve().parent.parent.parent.parent / "ml" / "models"

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


class ModelLoader:
    _instance: Optional["ModelLoader"] = None

    def __init__(self, model_dir: Optional[Path] = None):
        self.model_dir = self._resolve_model_dir(model_dir)
        self.model = None
        self.pipeline = None
        self.preprocessor = None
        self.label_encoder = None
        self.metadata = {}
        self._load_artifacts()

    @classmethod
    def get_instance(cls) -> "ModelLoader":
        if cls._instance is None:
            cls._instance = ModelLoader()
        return cls._instance

    def _resolve_model_dir(self, custom_dir: Optional[Path]) -> Path:
        if custom_dir and custom_dir.exists():
            return custom_dir
        if PRIMARY_MODEL_DIR.exists():
            return PRIMARY_MODEL_DIR
        if FALLBACK_MODEL_DIR.exists():
            return FALLBACK_MODEL_DIR
        raise FileNotFoundError(
            f"Could not locate ML models directory at {PRIMARY_MODEL_DIR} or {FALLBACK_MODEL_DIR}"
        )

    def _load_artifacts(self):
        model_path = self.model_dir / "stress_risk_model.joblib"
        pipeline_path = self.model_dir / "pipeline.joblib"
        preprocessor_path = self.model_dir / "preprocessor.joblib"
        encoder_path = self.model_dir / "label_encoder.joblib"
        metadata_path = self.model_dir / "model_metadata.json"

        if not model_path.exists():
            raise FileNotFoundError(f"Model file missing: {model_path}")

        self.model = joblib.load(model_path)
        if pipeline_path.exists():
            self.pipeline = joblib.load(pipeline_path)
        if preprocessor_path.exists():
            self.preprocessor = joblib.load(preprocessor_path)
        if encoder_path.exists():
            self.label_encoder = joblib.load(encoder_path)
        if metadata_path.exists():
            with open(metadata_path, "r", encoding="utf-8") as f:
                self.metadata = json.load(f)

    @property
    def model_version(self) -> str:
        return self.metadata.get("selected_timestamp", "v1.0.0-prototype")

    @property
    def model_name(self) -> str:
        return self.metadata.get("model_name", "Random Forest")

    def predict(self, sample_data: Dict[str, Any]) -> Dict[str, Any]:
        df = pd.DataFrame([sample_data])[FEATURE_COLUMNS]

        if self.pipeline is not None:
            pred_encoded = self.pipeline.predict(df)[0]
            probs = self.pipeline.predict_proba(df)[0]
        else:
            scaled = self.preprocessor.transform(df)
            pred_encoded = self.model.predict(scaled)[0]
            probs = self.model.predict_proba(scaled)[0]

        if self.label_encoder is not None:
            predicted_label = str(self.label_encoder.inverse_transform([pred_encoded])[0])
            classes = [str(c) for c in self.label_encoder.classes_]
        else:
            classes = ["LOW", "MODERATE", "HIGH"]
            predicted_label = classes[pred_encoded]

        prob_dict = {cls_name: round(float(p), 4) for cls_name, p in zip(classes, probs)}
        max_prob = round(float(np.max(probs)), 4)

        return {
            "risk_level": predicted_label,
            "risk_score": max_prob,
            "confidence": max_prob,
            "class_probabilities": prob_dict,
        }

    def explain(self, sample_data: Dict[str, Any], top_k: int = 5) -> List[Dict[str, Any]]:
        df = pd.DataFrame([sample_data])[FEATURE_COLUMNS]
        if self.preprocessor is not None:
            scaled_vals = self.preprocessor.transform(df)[0]
        else:
            scaled_vals = df.iloc[0].values

        if hasattr(self.model, "feature_importances_"):
            weights = self.model.feature_importances_
        elif hasattr(self.model, "coef_"):
            weights = np.mean(np.abs(self.model.coef_), axis=0)
        else:
            weights = np.ones(len(FEATURE_COLUMNS)) / len(FEATURE_COLUMNS)

        factors = []
        for name, val, scaled_v, w in zip(FEATURE_COLUMNS, df.iloc[0], scaled_vals, weights):
            impact_score = round(float(scaled_v * w), 4)
            readable_name = name.replace("_", " ").title()
            factors.append({
                "feature": name,
                "display_name": readable_name,
                "value": float(val),
                "impact_score": impact_score,
                "description": f"{readable_name} ({val}) contributing with impact factor {impact_score:+.3f}",
            })

        sorted_factors = sorted(factors, key=lambda x: abs(x["impact_score"]), reverse=True)
        return sorted_factors[:top_k]


def get_model_loader() -> ModelLoader:
    return ModelLoader.get_instance()
