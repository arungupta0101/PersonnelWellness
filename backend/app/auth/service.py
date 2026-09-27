from datetime import date
from sqlalchemy.orm import Session

from app.auth.security import hash_password
from app.models import Consent, DutyRecord, Personnel, User, WellnessCheckin

SEED_USERS = (
    {
        "username": "demo",
        "email": "personnel.demo@example.com",
        "role": "personnel",
        "full_name": "Alex Morgan",
        "employee_id": "DEMO-P-001",
        "department": "Operations",
        "designation": "Personnel Specialist",
        "date_of_birth": date(1992, 5, 15),
        "phone": "+1-555-0101",
    },
    {
        "username": "welfare.officer",
        "email": "welfare.officer@example.com",
        "role": "welfare_officer",
        "full_name": "Jordan Lee",
        "employee_id": "DEMO-W-001",
        "department": "Personnel Welfare",
        "designation": "Welfare Officer",
        "date_of_birth": date(1988, 9, 22),
        "phone": "+1-555-0102",
    },
    {
        "username": "commander",
        "email": "commander@example.com",
        "role": "commander",
        "full_name": "Taylor Singh",
        "employee_id": "DEMO-C-001",
        "department": "Command",
        "designation": "Unit Commander",
        "date_of_birth": date(1984, 11, 30),
        "phone": "+1-555-0103",
    },
    {
        "username": "admin",
        "email": "admin@example.com",
        "role": "admin",
        "full_name": "Casey Rivera",
        "employee_id": "DEMO-A-001",
        "department": "Administration",
        "designation": "System Administrator",
        "date_of_birth": date(1990, 3, 10),
        "phone": "+1-555-0104",
    },
)
SEED_PASSWORD = "DemoPass123!"


def ensure_seed_data(db: Session) -> None:
    for seed in SEED_USERS:
        user = db.query(User).filter(User.username == seed["username"]).first()
        if not user:
            user = User(
                username=seed["username"],
                email=seed["email"],
                hashed_password=hash_password(SEED_PASSWORD),
                role=seed["role"],
            )
            db.add(user)
            db.flush()
        else:
            # Ensure existing user has valid bcrypt hashed password
            if not user.hashed_password or not user.hashed_password.startswith("$2"):
                user.hashed_password = hash_password(SEED_PASSWORD)
                db.add(user)
                db.flush()

        personnel = db.query(Personnel).filter(Personnel.user_id == user.id).first()
        if not personnel:
            db.add(
                Personnel(
                    user_id=user.id,
                    employee_id=seed["employee_id"],
                    full_name=seed["full_name"],
                    department=seed["department"],
                    designation=seed["designation"],
                    date_of_birth=seed.get("date_of_birth"),
                    phone=seed.get("phone"),
                )
            )

        if seed["role"] == "personnel":
            existing_checkin = db.query(WellnessCheckin).filter(WellnessCheckin.user_id == user.id).first()
            if not existing_checkin:
                db.add(
                    WellnessCheckin(
                        user_id=user.id,
                        mood=8,
                        stress_level=3,
                        sleep_hours=7.5,
                        notes="Synthetic demo check-in: Feeling focused and well-rested.",
                    )
                )

            existing_consent = db.query(Consent).filter(Consent.user_id == user.id).first()
            if not existing_consent:
                db.add(
                    Consent(
                        user_id=user.id,
                        consent_type="data_sharing_wellness",
                        granted=True,
                    )
                )

            existing_duty = db.query(DutyRecord).filter(DutyRecord.user_id == user.id).first()
            if not existing_duty:
                db.add(
                    DutyRecord(
                        user_id=user.id,
                        duty_date=date.today(),
                        duty_type="Standard Patrol",
                        location="Sector 4 Headquarters",
                        hours=8.0,
                        status="completed",
                        notes="Synthetic demo duty record completed without incidents.",
                    )
                )

    db.commit()


def ensure_demo_user(db: Session) -> None:
    """Backward-compatible initializer for callers that only need demo data."""
    ensure_seed_data(db)
