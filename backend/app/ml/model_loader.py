"""
ML Model Loader Service for Personnel Wellness FastAPI Backend
================================================================
Lazily loads and caches the trained scikit-learn model, preprocessing pipeline,
label encoder, and metadata from the ML directory.

Provides prediction and tree-based feature contribution explainability.
"""

import json
import os
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple
import joblib
import numpy as np
import pandas as pd

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

    @classmethod
    def reset_instance(cls) -> None:
        """Reset singleton instance (useful for test resets)."""
        cls._instance = None

    def _resolve_model_dir(self, custom_dir: Optional[Path]) -> Path:
        if custom_dir and custom_dir.exists():
            return custom_dir

        env_dir = os.environ.get("MODEL_DIR")
        if env_dir and Path(env_dir).exists():
            return Path(env_dir)

        curr_file = Path(__file__).resolve()
        candidates = [
            curr_file.parents[3] / "ml" / "models",  # Repo Root / ml / models
            curr_file.parents[2] / "ml" / "models",  # backend / ml / models
            Path.cwd() / "ml" / "models",           # Working Directory / ml / models
            Path.cwd().parent / "ml" / "models",    # Parent Working Directory / ml / models
        ]

        for cand in candidates:
            if cand.exists() and (
                (cand / "stress_risk_model.joblib").exists() or (cand / "pipeline.joblib").exists()
            ):
                return cand

        raise FileNotFoundError(
            f"Could not locate ML models directory containing stress_risk_model.joblib or pipeline.joblib. "
            f"Checked candidate locations: {[str(c) for c in candidates]}"
        )

    def _load_artifacts(self):
        model_path = self.model_dir / "stress_risk_model.joblib"
        pipeline_path = self.model_dir / "pipeline.joblib"
        preprocessor_path = self.model_dir / "preprocessor.joblib"
        encoder_path = self.model_dir / "label_encoder.joblib"
        metadata_path = self.model_dir / "model_metadata.json"

        if not model_path.exists() and not pipeline_path.exists():
            raise RuntimeError(
                f"Production ML artifacts missing: Neither {model_path} nor {pipeline_path} exists."
            )

        # 1. Prefer pipeline.joblib if present
        if pipeline_path.exists():
            self.pipeline = joblib.load(pipeline_path)

        # 2. Load model
        if model_path.exists():
            self.model = joblib.load(model_path)
        elif self.pipeline is not None and hasattr(self.pipeline, "steps"):
            self.model = self.pipeline.steps[-1][1]

        # 3. Load or extract preprocessor / scaler step
        if preprocessor_path.exists():
            self.preprocessor = joblib.load(preprocessor_path)
        elif self.pipeline is not None:
            if hasattr(self.pipeline, "named_steps") and "scaler" in self.pipeline.named_steps:
                self.preprocessor = self.pipeline.named_steps["scaler"]
            elif hasattr(self.pipeline, "steps") and len(self.pipeline.steps) > 0:
                self.preprocessor = self.pipeline.steps[0][1]

        # 4. Load label encoder
        if encoder_path.exists():
            self.label_encoder = joblib.load(encoder_path)

        # 5. Load metadata
        if metadata_path.exists():
            with open(metadata_path, "r", encoding="utf-8") as f:
                self.metadata = json.load(f)

    @property
    def model_version(self) -> str:
        return self.metadata.get("selected_timestamp", "v1.0.0-prototype")

    @property
    def model_name(self) -> str:
        return self.metadata.get("model_name", "Gradient Boosting")

    def is_ready(self) -> Tuple[bool, str]:
        """Returns (is_ready, message) for readiness health endpoint validation."""
        if self.pipeline is None and self.model is None:
            return False, "ML Model and Pipeline uninitialized."
        return True, "Model loaded and operational."

    def predict(self, sample_data: Dict[str, Any]) -> Dict[str, Any]:
        df = pd.DataFrame([sample_data])[FEATURE_COLUMNS]

        if self.pipeline is not None:
            pred_encoded = self.pipeline.predict(df)[0]
            probs = self.pipeline.predict_proba(df)[0]
        elif self.model is not None and self.preprocessor is not None:
            scaled = self.preprocessor.transform(df)
            pred_encoded = self.model.predict(scaled)[0]
            probs = self.model.predict_proba(scaled)[0]
        else:
            raise RuntimeError("ML Prediction Failure: Neither Pipeline nor Model+Preprocessor available.")

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

        target_model = self.model
        if target_model is None and self.pipeline is not None and hasattr(self.pipeline, "steps"):
            target_model = self.pipeline.steps[-1][1]

        if hasattr(target_model, "feature_importances_"):
            weights = target_model.feature_importances_
        elif hasattr(target_model, "coef_"):
            weights = np.mean(np.abs(target_model.coef_), axis=0)
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
