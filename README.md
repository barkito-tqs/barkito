# Barkito

[![CI](https://github.com/barkito-tqs/barkito/actions/workflows/ci.yml/badge.svg)](https://github.com/barkito-tqs/barkito/actions/workflows/ci.yml)

## Project abstract

**Barkito** is a light-mobility fleet-as-a-service platform for shared electric boats on city canals. An operator runs a fleet of small electric boats docked at hubs along the canals of a (fictional) city, on behalf of an institutional client. End users find available boats by dock, book them for a period or start an immediate use, and return them at any dock. The client sets the rules of use (zones, maximum duration, capacity per dock) and follows the service through KPIs and SLA compliance.

> Concretization details (city, institutional client, rules of use) to be finalized after approval at checkpoint I1.

## Run with Docker Compose

Needs Docker with the Compose plugin; no Java or Node installation.

```bash
docker compose up --build -d   # build the images and start the system
docker compose ps              # both services should be running, the backend "healthy"
docker compose down            # stop and remove the containers
```

| Service | URL |
|---|---|
| Web UI | <http://localhost:3000> (calls to `/api` are proxied to the backend) |
| Backend API | <http://localhost:8080> (Swagger UI at `/swagger-ui.html`, health at `/actuator/health`) |

The repositories are still in memory, so data resets when the backend container restarts. A database service joins the Compose file when persistence arrives.

## Project team

| Name | Student no. | Role |
|------|-------------|------|
| Claudino Martins | 127368 | Team Coordinator |
| Gonçalo Vila | 125990 | Product Owner |
| Rodrigo Simões | 125514 | QA Engineer |
| Simão Pinto | 126099 | DevOps Master |

All members are developers.

## Project bookmarks

- Jira backlog: <https://barkito-tqs.atlassian.net/jira/software/projects/BRK/boards/1/backlog>
- Related repositories: none
- API documentation: Swagger UI at `/swagger-ui.html` on the running backend (locally <http://localhost:8080/swagger-ui.html>); OpenAPI document at `/v3/api-docs`
- Static analysis dashboard: TBD
- Observability dashboard: TBD
- Deployed application: TBD
