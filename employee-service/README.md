# Employee Manager — Spring Boot + JavaScript

A full-stack employee management system with role-based login, department relationships,
pagination, caching, scheduled jobs, AOP logging, file uploads, CSV export, and API documentation.

## Default accounts
| Role | Username | Password | Can do |
|---|---|---|---|
| Admin | `admin` | `admin123` | Everything, including delete employees + add departments |
| Staff | `manager` | `manager123` | View, add, edit (no delete, no department management) |

Passwords are stored as BCrypt hashes in the `app_users` table, never as plain text.

## Run
    mvn spring-boot:run

Open http://localhost:8080 — redirects to the login page automatically.

## New: application & review workflow, email, and an assistant
- **Public application form** — `/apply.html`, no login needed. Anyone can submit their
  details and a resume to apply.
- **Manager review queue** — `/review.html` (Admin-only link, shown on the dashboard).
  The manager sees every pending application, can **correct** any field before deciding,
  then **Approve** (creates the real `Employee` record) or **Reject** (with a reason).
- **Automatic email on the decision** — the applicant gets an email when approved or
  rejected. Off by default (logged to the console instead) so the project runs with zero
  setup — see `app.mail.enabled` in `application.properties` to send real emails.
- **Rule-based assistant** — a floating chat widget (bottom-right, every page) answers
  common questions (how to apply, what "pending" means, how to search, etc.) by keyword
  matching, not a real LLM. It's built with the same request/response shape a real AI API
  call would use, so swapping one in later is a small change, not a rewrite.

## Everything included
- **Spring Boot 3 + Spring Data JPA + Hibernate** — core backend and ORM
- **Entity relationship** — `Employee` → `Department` (many-to-one), not just a flat string
- **DTOs + mapper** — the API's JSON shape is decoupled from the database structure
- **Pagination & sorting** — `GET /api/employees?page=0&size=10&sort=salary,desc`
- **Dynamic search (JPA Specifications)** — one flexible query instead of many fixed ones
- **Spring Security with roles** — session login, BCrypt passwords, `@PreAuthorize("hasRole('ADMIN')")`
- **Soft delete** — deleted employees are flagged, not erased, so email addresses are safely reusable
- **Auditing** — `createdAt` / `updatedAt` are set automatically by Spring Data JPA
- **Caching** — repeated identical searches are served from memory (`@Cacheable` / `@CacheEvict`)
- **AOP logging** — an `@Aspect` times every service method without touching their code
- **Scheduled job** — logs an employee count summary every 60 seconds (`@Scheduled`)
- **File upload** — profile photos saved to disk and served back as URLs
- **CSV export** — `GET /api/employees/export/csv`
- **API docs (Swagger UI)** — http://localhost:8080/swagger-ui/index.html
- **Actuator health check** — http://localhost:8080/actuator/health
- **Multiple environment profiles** — `application-dev.properties` (H2) vs `application-prod.properties` (template for MySQL/Postgres)
- **Two kinds of automated tests** — `EmployeeApiTest` (MockMvc integration test, with roles) and `EmployeeServiceTest` (Mockito unit test, no Spring context)

## REST API

| Method | URL | Notes |
|---|---|---|
| GET | `/api/employees?q=&departmentId=&page=&size=&sort=` | Paginated list/search/filter |
| GET | `/api/employees/{id}` | Single employee |
| POST | `/api/employees` | Create |
| PUT | `/api/employees/{id}` | Update |
| DELETE | `/api/employees/{id}` | Soft delete — **admin only** |
| POST | `/api/employees/{id}/photo` | Upload a profile photo (multipart) |
| GET | `/api/employees/export/csv` | Download all employees as CSV |
| GET | `/api/departments` | List departments |
| POST | `/api/departments` | Create a department — **admin only** |
| POST | `/api/auth/register` | Create a staff-level login |
| GET | `/api/auth/me` | Current user + role |
| POST | `/api/applications` | Submit a job application — **public, no login** |
| GET | `/api/applications?status=PENDING` | List applications by status — **admin only** |
| PUT | `/api/applications/{id}` | Correct a submission's details — **admin only** |
| POST | `/api/applications/{id}/approve` | Approve — creates the Employee + sends email — **admin only** |
| POST | `/api/applications/{id}/reject` | Reject — sends email with reason — **admin only** |
| POST | `/api/chatbot/ask` | Ask the rule-based assistant a question |

## Project structure
    model/            Entities: Employee, Department, User
    repository/        Spring Data JPA + Specification support
    specification/      Dynamic search query building blocks
    dto/ + mapper/      API-facing shapes, decoupled from entities
    service/          Business logic: caching, soft delete, photo upload, CSV export
    controller/        REST endpoints
    security/          Login, roles, password hashing
    aspect/            AOP logging around the service layer
    scheduler/          Background job
    config/            Static file serving for uploaded photos
    exception/         Centralized 400/404/409 JSON error handling

## Known simplifications (worth mentioning if asked)
- **CSRF protection is disabled** to keep the plain-JavaScript frontend simple. A production app
  would enable it and send the token with each request instead.
- **The scheduled job runs every 60 seconds** for demo visibility; a real version would use a cron
  expression like `0 0 8 * * *` (daily at 8 AM) — shown commented in the code.
- **The `prod` profile is a template**, not a connected database, since this project ships with H2
  for easy local setup.
