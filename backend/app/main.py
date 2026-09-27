from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from starlette.middleware.base import BaseHTTPMiddleware
from sqlalchemy import text

from app.auth.service import ensure_seed_data
from app.config import settings
from app.database import Base, SessionLocal, engine
from app.models import (  # noqa: F401
    Assessment,
    AuditLog,
    Consent,
    DutyRecord,
    Intervention,
    Personnel,
    Prediction,
    Recommendation,
    User,
    WellnessCheckin,
)
from app.routes import assessment, auth, consent, personnel, prediction, support, wellness
from app.schemas.common import HealthResponse


class SecurityHeadersMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        response = await call_next(request)
        response.headers["X-Content-Type-Options"] = "nosniff"
        response.headers["X-Frame-Options"] = "DENY"
        response.headers["Strict-Transport-Security"] = "max-age=31536000; includeSubDomains"
        response.headers["X-XSS-Protection"] = "1; mode=block"
        return response


@asynccontextmanager
async def lifespan(_: FastAPI):
    """
    Resilient startup lifespan. Attempts database migration and seed data creation.
    If database is temporarily unreachable, logs warning without crashing Uvicorn.
    """
    try:
        Base.metadata.create_all(bind=engine)
        with SessionLocal() as db:
            ensure_seed_data(db)
        print("[STARTUP SUCCESS] Database tables created and seed data verified.")
    except Exception as e:
        print(f"[STARTUP WARNING] Database initialization failed during startup: {e}")
    yield


app = FastAPI(
    title=settings.app_name,
    description="Backend API for personnel wellness check-ins, assessments, and support requests.",
    version="0.1.0",
    lifespan=lifespan,
)

app.add_middleware(SecurityHeadersMiddleware)
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth.router)
app.include_router(consent.router)
app.include_router(wellness.router)
app.include_router(assessment.router)
app.include_router(personnel.router)
app.include_router(support.router)
app.include_router(prediction.router)


@app.get("/health", response_model=HealthResponse, tags=["System"])
def health() -> HealthResponse:
    db_status = "connected"
    try:
        with SessionLocal() as db:
            db.execute(text("SELECT 1"))
    except Exception as e:
        db_status = f"error: {str(e)}"

    return HealthResponse(status="ok", database=db_status)
