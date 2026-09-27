"""
Synthetic Personnel Wellness Dataset Generator
===============================================
Generates a realistic, synthetic, anonymized personnel wellness dataset
for prototype demonstration and screening model training.

DISCLAIMER:
This dataset is synthetic and generated for prototype evaluation only.
It is NOT clinically validated and is intended for welfare-risk screening demonstration.
"""

from pathlib import Path
import numpy as np
import pandas as pd

DATA_DIR = Path(__file__).resolve().parent / "data"
OUTPUT_FILE = DATA_DIR / "synthetic_personnel_wellness.csv"


def generate_synthetic_data(num_samples: int = 2000, random_seed: int = 42) -> pd.DataFrame:
    np.random.seed(random_seed)

    duty_hours_day = np.round(np.random.normal(loc=9.0, scale=2.5, size=num_samples).clip(4.0, 16.0), 1)
    weekly_duty_hours = np.round((duty_hours_day * 5.0 + np.random.uniform(0, 15, size=num_samples)).clip(30.0, 95.0), 1)
    deployment_days = np.random.exponential(scale=35.0, size=num_samples).astype(int).clip(0, 180)
    night_shifts = np.random.poisson(lam=4.0, size=num_samples).clip(0, 16)
    workload_score = np.round(np.random.uniform(1.0, 10.0, size=num_samples), 1)
    training_load = np.round(np.random.uniform(1.0, 10.0, size=num_samples), 1)
    transfer_frequency = np.random.choice([0, 1, 2, 3, 4, 5], p=[0.3, 0.35, 0.2, 0.1, 0.03, 0.02], size=num_samples)

    sleep_hours = np.round(np.random.normal(loc=6.5, scale=1.3, size=num_samples).clip(3.0, 9.5), 1)
    leave_days = np.random.randint(0, 31, size=num_samples)
    rest_days = np.random.randint(0, 12, size=num_samples)

    self_reported_stress = np.round(np.random.uniform(1.0, 10.0, size=num_samples), 1)
    fatigue_score = np.round((0.4 * self_reported_stress + 0.3 * (10.0 - sleep_hours) + 0.3 * workload_score + np.random.normal(0, 0.5, num_samples)).clip(1.0, 10.0), 1)
    mood_score = np.round((10.0 - 0.5 * self_reported_stress - 0.3 * fatigue_score + np.random.normal(0, 0.5, num_samples)).clip(1.0, 10.0), 1)
    social_support_score = np.round(np.random.uniform(1.0, 10.0, size=num_samples), 1)

    high_risk = (
        ((duty_hours_day >= 11.0) & (sleep_hours <= 5.5)) |
        ((fatigue_score >= 7.5) & (self_reported_stress >= 7.0)) |
        ((deployment_days >= 60) & (night_shifts >= 6) & (rest_days <= 3))
    )
    low_risk = (
        ((sleep_hours >= 7.0) & (self_reported_stress <= 4.0) & (workload_score <= 5.0)) |
        ((rest_days >= 5) & (social_support_score >= 6.5) & (fatigue_score <= 4.5))
    )

    target = []
    for h, l in zip(high_risk, low_risk):
        if h:
            target.append("HIGH")
        elif l:
            target.append("LOW")
        else:
            target.append("MODERATE")

    df = pd.DataFrame(
        {
            "duty_hours_day": duty_hours_day,
            "weekly_duty_hours": weekly_duty_hours,
            "deployment_days": deployment_days,
            "night_shifts": night_shifts,
            "workload_score": workload_score,
            "training_load": training_load,
            "transfer_frequency": transfer_frequency,
            "sleep_hours": sleep_hours,
            "leave_days": leave_days,
            "rest_days": rest_days,
            "self_reported_stress": self_reported_stress,
            "fatigue_score": fatigue_score,
            "mood_score": mood_score,
            "social_support_score": social_support_score,
            "target": target,
        }
    )

    return df


def main():
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    df = generate_synthetic_data(num_samples=2000, random_seed=42)
    df.to_csv(OUTPUT_FILE, index=False)
    print(f"[Dataset Generator] Saved synthetic dataset to {OUTPUT_FILE}")


if __name__ == "__main__":
    main()

