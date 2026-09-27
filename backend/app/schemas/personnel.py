from datetime import date

from app.schemas.common import ORMModel


class PersonnelResponse(ORMModel):
    id: int
    employee_id: str
    full_name: str
    department: str | None
    designation: str | None
    date_of_birth: date | None
    phone: str | None
