"""
Model Evaluation Script
=======================
Evaluates the trained stress_risk_model on the holdout test dataset.
Computes accuracy, precision, recall, F1-score, and confusion matrix.
Generates evaluation_report.json.

DISCLAIMER: Prototype evaluation script. Not clinically validated.
"""

import json
from pathlib import Path
import joblib
import numpy as np
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix, precision_recall_fscore_support

BASE_DIR = Path(__file__).resolve().parent
DATA_DIR = BASE_DIR / "data"
MODELS_DIR = BASE_DIR / "models"
REPORTS_DIR = BASE_DIR / "reports"

PROCESSED_DATA_FILE = DATA_DIR / "processed_data.joblib"
MODEL_FILE = MODELS_DIR / "stress_risk_model.joblib"
METADATA_FILE = MODELS_DIR / "model_metadata.json"
REPORT_FILE = REPORTS_DIR / "evaluation_report.json"


def evaluate_model():
    REPORTS_DIR.mkdir(parents=True, exist_ok=True)

    if not PROCESSED_DATA_FILE.exists() or not MODEL_FILE.exists():
        from train import train_and_select_model
        train_and_select_model()

    data = joblib.load(PROCESSED_DATA_FILE)
    model = joblib.load(MODEL_FILE)

    X_test_scaled = data["X_test_scaled"]
    y_test = data["y_test"]
    class_names = data["class_names"]

    y_pred = model.predict(X_test_scaled)

    acc = float(accuracy_score(y_test, y_pred))
    prec_macro, rec_macro, f1_macro, _ = precision_recall_fscore_support(y_test, y_pred, average="macro")
    prec_weighted, rec_weighted, f1_weighted, _ = precision_recall_fscore_support(y_test, y_pred, average="weighted")

    cm = confusion_matrix(y_test, y_pred).tolist()
    clf_report_dict = classification_report(y_test, y_pred, target_names=class_names, output_dict=True)

    print("=" * 60)
    print("MODEL EVALUATION REPORT")
    print("=" * 60)
    print(f"Accuracy         : {acc:.4f}")
    print(f"Precision (Macro): {prec_macro:.4f} | (Weighted): {prec_weighted:.4f}")
    print(f"Recall    (Macro): {rec_macro:.4f} | (Weighted): {rec_weighted:.4f}")
    print(f"F1-Score  (Macro): {f1_macro:.4f} | (Weighted): {f1_weighted:.4f}")
    print("-" * 60)
    print("Classification Report:")
    print(classification_report(y_test, y_pred, target_names=class_names))
    print("-" * 60)
    print("Confusion Matrix:")
    print(f"Labels order: {class_names}")
    for row_name, row in zip(class_names, cm):
        print(f"Actual {row_name:8s}: {row}")
    print("=" * 60)

    report_data = {
        "accuracy": acc,
        "precision_macro": float(prec_macro),
        "recall_macro": float(rec_macro),
        "f1_macro": float(f1_macro),
        "precision_weighted": float(prec_weighted),
        "recall_weighted": float(rec_weighted),
        "f1_weighted": float(f1_weighted),
        "confusion_matrix": cm,
        "class_names": class_names,
        "classification_report": clf_report_dict,
        "disclaimer": (
            "Welfare-risk screening prototype evaluation results. "
            "Synthetic dataset used for prototype demonstration. Not clinically validated."
        ),
    }

    with open(REPORT_FILE, "w", encoding="utf-8") as f:
        json.dump(report_data, f, indent=2)
    print(f"[Evaluate] Saved evaluation report to {REPORT_FILE}")

    return report_data


if __name__ == "__main__":
    evaluate_model()
