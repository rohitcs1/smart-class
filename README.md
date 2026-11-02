# ELNEB

ELNEB is a smart classroom monitoring platform with an independently deployable Spring Boot backend, React dashboard, MySQL schema migrations, and ESP32 classroom firmware.

## Local development

1. Copy `backend/.env.example` to `backend/.env` and set the database and JWT values.
2. Start MySQL and create the `elneb` database (or allow the configured URL to create it).
3. Run backend: `cd backend; mvn spring-boot:run`
4. Run frontend: `cd frontend; npm install; npm run dev`

The API health endpoint is `GET http://localhost:8080/api/health`.

The backend targets Java 25, the current LTS release. The supported platform roles are
institution admin, reception, HOD, teacher, student, and device admin; institution
administrators can assign HOD and operational roles.
