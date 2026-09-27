"""
Explainability Script for Personnel Wellness Model
===================================================
Provides global feature importance analysis and local sample prediction feature contribution
explanations for prototype risk assessment transparency.

DISCLAIMER:
Prototype screening feature explanation module. Not clinically validated.
"""

from typing import Any, Dict, List
from pathlib import Path
import joblib
import numpy as np
import pandas as pd

BASE_DIR = Path(__file__).resolve().parent
MODELS_DIR = BASE_DIR / "models"
DATA_DIR = BASE_DIR / "data"

MODEL_FILE = MODELS_DIR / "stress_risk_model.joblib"
PROCESSED_DATA_FILE = DATA_DIR / "processed_data.joblib"
METADATA_FILE = MODELS_DIR / "model_metadata.json"
PREPROCESSOR_FILE = MODELS_DIR / "preprocessor.joblib"

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
    "Feature importance scores are provided for prototype transparency only. "
    "This system is not clinically validated and does not replace professional medical advice."
)


def get_global_feature_importances() -> List[Dict[str, Any]]:
    model = joblib.load(MODEL_FILE)
    data = joblib.load(PROCESSED_DATA_FILE)
    feature_names = data["feature_names"]

    if hasattr(model, "feature_importances_"):
        importances = model.feature_importances_
    elif hasattr(model, "coef_"):
        importances = np.mean(np.abs(model.coef_), axis=0)
    else:
        importances = np.ones(len(feature_names)) / len(feature_names)

    ranked = sorted(
        [
            {"feature": name, "importance": round(float(imp), 4)}
            for name, imp in zip(feature_names, importances)
        ],
        key=lambda x: x["importance"],
        reverse=True,
    )

    return ranked


def explain_sample(sample_data: Dict[str, Any], top_k: int = 5) -> Dict[str, Any]:
    scaler = joblib.load(PREPROCESSOR_FILE)
    model = joblib.load(MODEL_FILE)

    df = pd.DataFrame([sample_data])[FEATURE_COLUMNS]
    scaled_values = scaler.transform(df)[0]

    if hasattr(model, "feature_importances_"):
        weights = model.feature_importances_
    elif hasattr(model, "coef_"):
        weights = np.mean(np.abs(model.coef_), axis=0)
    else:
        weights = np.ones(len(FEATURE_COLUMNS))

    # Calculate weighted feature deviation score relative to population baseline
    contributions = []
    for name, val, scaled_val, weight in zip(FEATURE_COLUMNS, df.iloc[0], scaled_values, weights):
        contrib_score = float(scaled_val * weight)
        contributions.append({
            "feature": name,
            "value": float(val),
            "z_score": round(float(scaled_val), 2),
            "importance_weight": round(float(weight), 4),
            "risk_contribution_score": round(contrib_score, 4),
        })

    # Sort by absolute risk contribution score
    sorted_contribs = sorted(contributions, key=lambda x: abs(x["risk_contribution_score"]), reverse=True)

    return {
        "top_risk_factors": sorted_contribs[:top_k],
        "all_feature_contributions": sorted_contribs,
        "disclaimer": DISCLAIMER,
    }


if __name__ == "__main__":
    print("=" * 60)
    print("GLOBAL FEATURE IMPORTANCE RANKING")
    print("=" * 60)
    importances = get_global_feature_importances()
    for item in importances:
        print(f"  {item['feature']:22s}: {item['importance']:.4f}")

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

    print("\n" + "=" * 60)
    print("SAMPLE PREDICTION EXPLANATION (Top 5 Factors)")
    print("=" * 60)
    explanation = explain_sample(sample, top_k=5)
    for factor in explanation["top_risk_factors"]:
        print(f"  {factor['feature']:22s} = {factor['value']:<5} (z-score: {factor['z_score']:>5.2f}, contrib: {factor['risk_contribution_score']:>6.3f})")
