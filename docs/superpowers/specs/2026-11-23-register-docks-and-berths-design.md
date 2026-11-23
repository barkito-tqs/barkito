# US-04 (BRK-43) Register docks and berths: design

## Intent

The operator registers docks with their location and berths so that boats have places where trips start and end. The story also makes the web UI's first real call to the REST API: the docks page loads and registers docks through the backend.

## Decisions

| Topic | Decision |
|---|---|
| Scope | Backend and frontend wiring (list and register docks). No UI for adding berths; API only. |
| Location | Latitude [-90, 90] and longitude [-180, 180] required; district optional. |
| Berths | A total count; the dock gets berths 1..N, all in service. **1 to 10 inclusive**: a new rule, not in the BR table of the Product Specification yet. |
| Name | Unique ignoring case and surrounding spaces; stored trimmed; duplicate → 409. Name and district ≤ 64 characters. |
| Endpoints | `POST /api/v1/docks` (201 + Location), `GET /api/v1/docks` (by name), `GET /api/v1/docks/{id}`, `POST /api/v1/docks/{id}/berths` `{count}` (201). |
| Adding berths | Numbering continues after the last berth; new berths in service; total > 10 or count < 1 → 422; unknown dock → 404. |
| Errors | The 1–10 rule gives 422 both when registering and when adding; 400 is for missing or malformed fields. |
| Map | The backend checks only geographic ranges. The web UI turns latitude and longitude into map units with a fixed box around the canals and keeps docks outside it on the nearest edge. |
| Access | Endpoints are open, like the boat endpoints; roles come with US-02. |
| Seed | The `dev` profile seeds the six docks of the web UI with fixed UUIDs (`DevDockSeeder`); the web UI's mock data uses the same ids. Docker Compose turns the profile on. |

## Approaches

1. **`DockService` with separate repositories (chosen).** It follows `BoatService`. `Dock` gains district and position, berths stay in the existing `BerthRepository` that US-09 already reads, and a `synchronized` service keeps the name unique and the numbering free of races.
2. Dock as an aggregate that owns its berths. This gives a richer domain, but it would mean reworking `BerthRepository`, `AvailableBoatService` and their tests.

## Calls left to the builder (and taken)

- `Dock.MAX_BERTHS` and `Dock.MIN_BERTHS` are named constants in the domain, like `Boat.MIN_BOOKABLE_BATTERY_PERCENT`.
- The seed keeps the berths in service of the mock data (Station 5 of 6, Arts District 3 of 4).
- The web UI starts with the seed docks and replaces them with the API's. If the API cannot be reached, the docks page warns and keeps them.

## Follow-ups

- Add the 1–10 berths rule to the Product Specification (Google Doc).
- Re-estimate BRK-43 in Jira: the scope grew past 2 points.
- Xray Test keys for the new scenarios (`register_dock.feature`, `register_berth.feature`).
- The ER diagram PNG still needs regenerating from `er-diagram.dbml`.
