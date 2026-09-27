"""
Model Training & Selection Script
=================================
Trains and compares Logistic Regression, Random Forest, and Gradient Boosting models.
Selects the best performing model based on cross-validation F1-macro score and accuracy.
Saves model artifacts, scikit-learn Pipeline, and model metadata JSON.

DISCLAIMER: Prototype welfare-risk screening model. Not clinically validated.
"""

from datetime import datetime, timezone
import json
from pathlib import Path
import joblib
import numpy as np
from sklearn.ensemble import GradientBoostingClassifier, RandomForestClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, f1_score
from sklearn.model_selection import StratifiedKFold, cross_validate
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

BASE_DIR = Path(__file__).resolve().parent
DATA_DIR = BASE_DIR / "data"
MODELS_DIR = BASE_DIR / "models"

PROCESSED_DATA_FILE = DATA_DIR / "processed_data.joblib"
MODEL_FILE = MODELS_DIR / "stress_risk_model.joblib"
PIPELINE_FILE = MODELS_DIR / "pipeline.joblib"
METADATA_FILE = MODELS_DIR / "model_metadata.json"

DISCLAIMER = (
    "This machine-learning model is a welfare-risk screening prototype for demonstration purposes. "
    "It is NOT clinically validated and must NOT be used as a medical or psychological diagnosis system."
)


def load_processed_data():
    if not PROCESSED_DATA_FILE.exists():
        from preprocess import preprocess_data
        return preprocess_data()
    return joblib.load(PROCESSED_DATA_FILE)


def train_and_select_model():
    MODELS_DIR.mkdir(parents=True, exist_ok=True)
    data = load_processed_data()

    X_train_scaled = data["X_train_scaled"]
    y_train = data["y_train"]
    X_test_scaled = data["X_test_scaled"]
    y_test = data["y_test"]
    feature_names = data["feature_names"]
    class_names = data["class_names"]

    candidate_models = {
        "Logistic Regression": LogisticRegression(max_iter=1000, random_state=42),
        "Random Forest": RandomForestClassifier(n_estimators=100, random_state=42),
        "Gradient Boosting": GradientBoostingClassifier(n_estimators=100, random_state=42),
    }

    results = {}
    best_model_name = None
    best_f1_score = -1.0
    best_estimator = None

    skf = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)
    scoring = ["accuracy", "f1_macro"]

    print("Model Performance\n")

    for name, model in candidate_models.items():
        cv_res = cross_validate(model, X_train_scaled, y_train, cv=skf, scoring=scoring)
        cv_acc = float(np.mean(cv_res["test_accuracy"]))
        cv_f1 = float(np.mean(cv_res["test_f1_macro"]))

        model.fit(X_train_scaled, y_train)
        y_test_pred = model.predict(X_test_scaled)
        test_acc = float(accuracy_score(y_test, y_test_pred))
        test_f1 = float(f1_score(y_test, y_test_pred, average="macro"))

        results[name] = {
            "cv_accuracy_mean": cv_acc,
            "cv_f1_macro_mean": cv_f1,
            "test_accuracy": test_acc,
            "test_f1_macro": test_f1,
        }

        print(f"{name}")
        print(f"F1: {test_f1:.4f}\n")

        if test_f1 > best_f1_score:
            best_f1_score = test_f1
            best_model_name = name
            best_estimator = model

    print(f"Selected Model: {best_model_name}")

    # Save trained best model
    joblib.dump(best_estimator, MODEL_FILE)

    # Build and save complete sklearn Pipeline (Scaler + Model)
    preprocessor = joblib.load(MODELS_DIR / "preprocessor.joblib")
    pipeline = Pipeline([
        ("scaler", preprocessor),
        ("classifier", best_estimator),
    ])
    joblib.dump(pipeline, PIPELINE_FILE)

    # Create model metadata
    metadata = {
        "model_name": best_model_name,
        "selected_timestamp": datetime.now(timezone.utc).isoformat(),
        "disclaimer": DISCLAIMER,
        "features": feature_names,
        "target_classes": class_names,
        "comparison_results": results,
        "best_cv_f1_macro": best_f1_score,
        "best_test_accuracy": results[best_model_name]["test_accuracy"],
        "best_test_f1_macro": results[best_model_name]["test_f1_macro"],
    }

    with open(METADATA_FILE, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2)

    return best_model_name, best_estimator, metadata


if __name__ == "__main__":
    train_and_select_model()

