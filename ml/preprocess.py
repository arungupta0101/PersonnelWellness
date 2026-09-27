"""
Preprocessing Pipeline for Personnel Wellness Model
====================================================
Performs scaling, target encoding, and train/test dataset splitting.

DISCLAIMER: Prototype screening pipeline. Not clinically validated.
"""

from pathlib import Path
import joblib
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder, StandardScaler

BASE_DIR = Path(__file__).resolve().parent
DATA_DIR = BASE_DIR / "data"
MODELS_DIR = BASE_DIR / "models"

RAW_DATA_FILE = DATA_DIR / "synthetic_personnel_wellness.csv"
PROCESSED_DATA_FILE = DATA_DIR / "processed_data.joblib"

PREPROCESSOR_FILE = MODELS_DIR / "preprocessor.joblib"
LABEL_ENCODER_FILE = MODELS_DIR / "label_encoder.joblib"

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
TARGET_COLUMN = "target"
CLASS_NAMES = ["LOW", "MODERATE", "HIGH"]


def load_raw_data() -> pd.DataFrame:
    if not RAW_DATA_FILE.exists():
        from generate_dataset import main as generate_main
        generate_main()
    return pd.read_csv(RAW_DATA_FILE)


def preprocess_data(test_size: float = 0.2, random_state: int = 42):
    MODELS_DIR.mkdir(parents=True, exist_ok=True)
    DATA_DIR.mkdir(parents=True, exist_ok=True)

    df = load_raw_data()
    X = df[FEATURE_COLUMNS]
    y = df[TARGET_COLUMN]

    label_encoder = LabelEncoder()
    label_encoder.fit(CLASS_NAMES)
    y_encoded = label_encoder.transform(y)

    X_train, X_test, y_train, y_test = train_test_split(
        X, y_encoded, test_size=test_size, random_state=random_state, stratify=y_encoded
    )

    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)

    # Save artifacts
    joblib.dump(scaler, PREPROCESSOR_FILE)
    joblib.dump(label_encoder, LABEL_ENCODER_FILE)

    processed_payload = {
        "X_train": X_train,
        "X_test": X_test,
        "X_train_scaled": X_train_scaled,
        "X_test_scaled": X_test_scaled,
        "y_train": y_train,
        "y_test": y_test,
        "feature_names": FEATURE_COLUMNS,
        "class_names": CLASS_NAMES,
    }
    joblib.dump(processed_payload, PROCESSED_DATA_FILE)

    print(f"[Preprocess] Scaler saved to {PREPROCESSOR_FILE}")
    print(f"[Preprocess] LabelEncoder saved to {LABEL_ENCODER_FILE}")
    print(f"[Preprocess] Train set shape: {X_train_scaled.shape}, Test set shape: {X_test_scaled.shape}")

    return processed_payload


if __name__ == "__main__":
    preprocess_data()
