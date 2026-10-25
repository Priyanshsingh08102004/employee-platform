# Employee Platform — Java + .NET microservices on Docker, with CI/CD and Kubernetes

A polyglot backend: a **Java / Spring Boot** employee service and a **.NET 8 / ASP.NET Core**
payroll service that work together, packaged with Docker, automated by GitHub Actions, and
described for Kubernetes.

## Architecture

```
 Browser ──► employee-service (Java, Spring Boot)  :8080     <- the only public entry point
               • login, roles, employees, applications, chatbot, email
               • /api/payroll/**   admin-only GATEWAY  ───────────┐
               • /api/internal/**  employee data for payroll  ◄──┐│  X-API-KEY (shared secret)
                                                                 ││
                                payroll-service (.NET 8)  :5100  ◄┘│
                                 • calculates gross / PF / tax / net pay
                                 • own database (SQLite via EF Core)
                                 • calls Java for employee data  ──┘
```

**Why split it?** Payroll is a separate business capability with its own data and rules, so it
is its own service. The browser never talks to it directly: Java checks the user's session
(ADMIN only) and forwards the call server-to-server. Payroll therefore needs no login system and
is never exposed publicly. If payroll is down, Java answers `503` with a clear message and the rest
of the app keeps working.

## Tech stack
| Area | Used |
|---|---|
| Java service | Java 17, Spring Boot 3.3, Spring Data JPA/Hibernate, Spring Security, H2, AOP, caching, scheduling, Actuator, Swagger, Spring Mail, `RestClient` |
| .NET service | C#, .NET 8, ASP.NET Core minimal APIs, Entity Framework Core, SQLite, typed `HttpClient` with retry, custom middleware, Swashbuckle (Swagger) |
| Frontend | HTML5, CSS3, vanilla JavaScript (Fetch API) |
| Testing | JUnit 5, Mockito, MockMvc (Java) · xUnit (.NET) · Docker smoke test in CI |
| DevOps | Docker multi-stage builds, Docker Compose, Linux Bash scripts, GitHub Actions CI/CD, GitHub Container Registry, Kubernetes manifests |

## Ways to run it

### A) Java only (what you already had)
    cd employee-service
    mvn spring-boot:run
Open http://localhost:8080 — everything works; the **Payroll** page will say the payroll service is not running.

### B) Everything with Docker  (needs Docker Desktop)
    docker compose up --build        # or, on Linux/macOS/Git Bash:  ./scripts/deploy.sh
Open http://localhost:8080 · payroll Swagger (local machine only): http://localhost:5100/swagger

### C) Both services without Docker  (needs JDK 17+, Maven, and the .NET 8 SDK)
    # terminal 1
    cd employee-service && mvn spring-boot:run
    # terminal 2
    cd payroll-service && dotnet run

### Try the payroll flow
1. Sign in as `admin` / `admin123`  →  click **Payroll**
2. Pick a month → **Run payroll for this month** → payslips appear for every employee
3. Run the same month again → refused (`409`, one run per month)

Accounts: `admin / admin123` (ADMIN) and `manager / manager123` (USER, no payroll access).

## Security notes
- Service-to-service calls carry `X-API-KEY`; both sides compare it in **constant time**.
- The defaults (`dev-shared-secret`) are for local development. Set `INTERNAL_API_KEY` in `.env`
  (see `.env.example`) or a Kubernetes Secret anywhere real.
- Containers run as **non-root**; the payroll port is bound to `127.0.0.1` in Compose.
- CSRF protection is disabled to keep the plain-JS frontend simple — enable it for production.

## DevOps pieces
| Path | What it does |
|---|---|
| `employee-service/Dockerfile`, `payroll-service/Dockerfile` | Multi-stage builds: compile in an SDK image, ship only the runtime |
| `docker-compose.yml` | Runs both services, named volumes for data, health checks, start-up ordering |
| `scripts/deploy.sh` | One-command deploy: build, start, wait for health |
| `scripts/health-check.sh` | Polls both health endpoints with a timeout (also used by CI) |
| `scripts/backup.sh` | Timestamped `tar.gz` backups of all volumes, keeps the newest 14 |
| `scripts/scan-logs.sh` | Greps both services' recent logs for errors |
| `.github/workflows/ci.yml` | Java tests, .NET tests, Docker smoke test, then publish images to GHCR on `main` |
| `k8s/` | Namespace, Secret template, Deployments, Services, PVCs, probes, resource limits, NetworkPolicy, Ingress |

Run the scripts with Git Bash or WSL on Windows (`.gitattributes` keeps them on LF line endings).

## Kubernetes (local, e.g. minikube or kind)
    docker build -t employee-service:latest ./employee-service
    docker build -t payroll-service:latest ./payroll-service
    # load the images into your cluster (minikube image load ... / kind load docker-image ...)
    kubectl apply -f k8s/namespace.yaml
    kubectl -n employee-platform create secret generic platform-secrets \
        --from-literal=INTERNAL_API_KEY="$(openssl rand -hex 32)"
    kubectl apply -f k8s/employee-service.yaml -f k8s/payroll-service.yaml -f k8s/ingress.yaml

## Honest limitations (good to say out loud)
- Each service runs **one replica** because H2 and SQLite are single-writer file databases.
  Scaling out means moving to PostgreSQL/MySQL first.
- The .NET service uses `EnsureCreated()`; a larger project would use EF Core migrations.
- The tax slabs are **illustrative**, not real tax advice.
- Not deployed to a real cloud account. The images and manifests are what you would hand to
  AWS (ECS/EKS), Azure or GCP — that step is not built here.
- The chatbot is rule-based, not an LLM.
