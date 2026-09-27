# Personnel Wellness Backend

FastAPI backend for SIH26186. It uses SQLite by default for local development and accepts any SQLAlchemy-compatible PostgreSQL URL through `DATABASE_URL`.

## Run locally

```powershell
.\venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --reload
```

Open Swagger at http://127.0.0.1:8000/docs.

A demo account is seeded on first startup:

- Username: `demo`
- Password: `demo1234`

Use `POST /auth/login`, then click **Authorize** in Swagger and enter the returned bearer token.

## Configuration

Copy `.env.example` to `.env` and replace `JWT_SECRET_KEY` before deployment. Set `DATABASE_URL` to a PostgreSQL URL such as `postgresql+psycopg://user:password@localhost/personnel_wellness` in non-local environments.

ML prediction is intentionally not implemented yet; assessment submission stores answers and leaves the score unset.
