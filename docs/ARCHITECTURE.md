# Architecture

Map of the code: where things live, which way the dependencies point, and where to add new work. Domain words are defined in [`CONTEXT.md`](CONTEXT.md). The planned data model is in [`diagrams/er-diagram.md`](diagrams/er-diagram.md).

## System

| Part | Path | Stack |
|---|---|---|
| Backend | `backend/` | Java 21, Spring Boot, Maven. A modular monolith exposing a REST API under `/api/v1`. |
| Web UI | `frontend/` | React 18, TypeScript, Vite, React Router. Served by nginx in Docker, which proxies `/api` to the backend. |
| Identity provider | external | Keycloak (OpenID Connect). Issues the JWTs whose roles the backend checks. |
| Database | planned for I3 | PostgreSQL. Until then the repositories are in memory and data resets on restart. |

`docker-compose.yml` runs the backend and the web UI. Telemetry arrives through a port; today the source is an in-process simulator.

## Backend modules

Each module is a package `pt.ua.tqs.barkito.<module>` with three layers:

```
<module>/
├── domain/        entities, value objects, enums, ports (repository interfaces), domain exceptions
├── application/   services (use cases), application exceptions, results returned to adapters
└── adapter/
    ├── web/          REST controllers, request/response records, @RestControllerAdvice
    ├── persistence/  implementations of the domain's repository ports
    └── <other>/      other adapters, e.g. telemetry/adapter/simulator, accounts/adapter/security
```

| Module | Holds | Status |
|---|---|---|
| `accounts` | `Role`, JWT security, the rules for who can reach each endpoint | in code |
| `fleet` | boats, docks, berths, boat lifecycle | in code |
| `telemetry` | parses telemetry readings and applies them to boats | in code |
| `trips`, `payments`, `maintenance`, `rules`, `extensions` | see the ER diagram's "Tables by module" | planned |
| `config` | cross-cutting Spring config (OpenAPI, dev data seeding) | in code |

### Dependency rules

[`ArchitectureTest`](../backend/src/test/java/pt/ua/tqs/barkito/ArchitectureTest.java) (ArchUnit) enforces these, and the build fails when one is broken:

- `domain` does not depend on its own `application` or `adapter`, or on Spring. It may use other modules' `domain` types.
- `application` depends on no `adapter`.
- A module's adapters do not depend on another module's adapters.
- There are no dependency cycles between modules.

A module uses another module through its `application` services or `domain` types. For example, `TelemetryService` calls `fleet.application.BoatService.recordStatus`, and `TelemetryReading` carries a `fleet.domain.BoatStatus`.

### Conventions

- **Domain objects** are immutable: records, or final classes whose transitions return a new instance (`Boat.activate()` returns a new `Boat`). Factories are named after the domain action (`Boat.register`, `Dock.register`). `restore` rebuilds an object for persistence and tests.
- **Business rule constants** live on the domain type they constrain, as named constants that cite the rule ID from [`business-rules.md`](business-rules.md) (`Boat.MIN_BOOKABLE_BATTERY_PERCENT`, BR5).
- **Ports** are interfaces in `domain` (`BoatRepository`, `TelemetrySource`). Adapters implement them as Spring beans. Services receive the ports through their constructor.
- **Services** in `application` are `@Service` classes, one per aggregate or use case group (`BoatService`, `DockService`). They throw application exceptions such as `DockNotFoundException` or `DuplicateDockNameException`. The domain throws its own, such as `BoatTransitionRefusedException`.
- **Controllers** map `/api/v1/<resource>`. They take `@Valid` request records and return response records built with `XResponse.from(...)`, never domain objects. They are documented with springdoc `@Operation`/`@ApiResponse`. An action on a resource is a `POST` to a sub-resource noun (`POST /boats/{id}/activation`).
- **Errors** are RFC 9457 Problem Details. Each module has one `@RestControllerAdvice` (for example `FleetExceptionHandler`) that maps its exceptions: not found → 404, conflict with current state → 409, a broken business rule → 422, bean validation → 400.
- **Security**: `SecurityConfig` grants every endpoint to exactly one role and refuses everything else (`denyAll`). Roles come from the JWT claim `realm_access.roles`. The `dev` profile (`DevJwtConfig`) accepts the tokens in `dev-tokens.env`.
- **Dev profile**: `@Profile("dev")` beans seed data (`DevDockSeeder`) and configure the dev JWT decoder. Deployed environments must not run it.

## Backend tests

Tests mirror the main package. Each test runs at the narrowest level that proves the behaviour:

| What | Where | How |
|---|---|---|
| Domain and services | `<module>/domain`, `<module>/application` | Plain JUnit 5 and Mockito, no Spring. |
| Controllers | `<module>/adapter/web/*ControllerTest` | `@WebMvcTest` with `SecuredWebMvcTestConfig` and `TestRoles.as(Role)`. The service is a `@MockitoBean`. |
| Endpoint authorization | `accounts/adapter/security/EndpointAuthorizationTest` | One row per endpoint and role. Every new endpoint adds a row. |
| Acceptance (Gherkin) | `src/test/resources/features/<module>/*.feature`, steps in `bdd/` | Cucumber with Spring. |
| Architecture | `ArchitectureTest` | ArchUnit. |
| Persistence and API integration | planned with PostgreSQL | Testcontainers, run separately from unit tests. |

## Web UI

```
frontend/src/
├── api/         http.ts (fetch wrapper, ApiError from Problem Details), store.ts (app state), mockData.ts
├── auth/        session.tsx: dev sign-in by persona until OpenID Connect (US-01)
├── domain/      types.ts mirrors the API types; rules.ts and pricing.ts are client-side checks
├── pages/       one folder per role: user/, operator/, technician/, client/, admin/
├── components/  AppShell, RequireRole, nav, shared UI (ui.tsx)
├── lib/         formatting and hooks with no domain meaning
└── styles/      tokens.css (design tokens), base.css, components.css
```

- **`store.ts` mixes the real API and mocks.** Docks and berths call the backend through `http`. Every other action is an in-memory stand-in with the same signature and the same `ApiError` status codes, so a screen moves to the real API by changing only the store function. When a backend endpoint arrives, replace the matching mock in `store.ts`.
- **The backend is the source of truth for business rules.** `domain/rules.ts` and `domain/pricing.ts` exist for the mocks and for immediate feedback in forms. A rule that the backend enforces must not live only in the frontend.
- **Routes** are declared in the `AREAS` table in `App.tsx`, each with the one role that may open it. `RequireRole` guards them.
- **Styling** uses the CSS variables in `styles/tokens.css`. Shared components are in `components/ui.tsx`.

## Where to add

**A new endpoint**
1. Add the use case to the module's service in `application`, and the logic it needs to `domain`.
2. Add the controller method, the request/response records and the springdoc annotations in `adapter/web`.
3. Map any new exception in the module's `@RestControllerAdvice`.
4. Add the role rule in `SecurityConfig` and a row in `EndpointAuthorizationTest`.
5. Tests: domain/service unit tests, a `@WebMvcTest`, and a Cucumber scenario if it is a core story.

**A new entity or aggregate**
1. Add the domain type and its repository port in `<module>/domain`.
2. Add the adapter in `<module>/adapter/persistence`. Use the table and column names in the ER diagram.

**A new module**
1. Create `pt.ua.tqs.barkito.<module>` with `domain`, `application` and `adapter`. `ArchitectureTest` covers it automatically.
2. Take the module name and its tables from the ER diagram's "Tables by module".

**A new screen**
1. Add the page under `pages/<role>/` and a row in `AREAS` in `App.tsx`. Add it to `components/nav.ts` if it belongs in the menu.
2. Read and write through `api/store.ts`, never with `fetch` directly.
