# Barkito: entity-relationship diagram

Logical data model for the PostgreSQL database, derived from the conceptual model of the Product Specification (3.1), the business rules (3.2) and the boat lifecycle (3.3). Table and column names are the ones the JPA entities and the Flyway/SQL scripts should use.

Conventions:

- Primary keys are `UUID`, except for reference tables keyed by their code (`boat_type`, `sla_policy`).
- Money is stored in cents (`*_cents`, integer) so prices never suffer rounding errors.
- Timestamps are `TIMESTAMPTZ` (UTC); domain code reads the time from an injected clock.
- Each module owns its tables (section 4.3). References between modules are plain foreign keys to the other module's `id`, never joins into its internals.
- Accounts with no domain data (operators, client managers, admins) live only in the identity provider and have no table.

```mermaid
erDiagram
    %% ---------- Accounts ----------
    CUSTOMER {
        uuid id PK
        string idp_subject UK "subject of the OpenID Connect token"
        string name
        string email UK
        date birth_date "BR1 minimum age"
        string payment_method_id "Stripe payment method, null until saved (BR2)"
        int debt_cents "BR2: > 0 blocks reserve and unlock"
        int points_balance "BR25, BR26"
        timestamptz created_at "BR21, BR22 account age"
    }

    %% ---------- Fleet ----------
    BOAT_TYPE {
        string code PK "STANDARD, XL"
        int capacity "BR4: 5 / 10"
        int min_driver_age "BR1: 18 / 21"
        int unlock_fee_cents "BR11"
        int price_per_minute_cents "BR11"
        int overtime_per_minute_cents "BR12"
        int hold_fee_per_minute_cents "BR8"
        int deposit_cents "BR13"
        int recovery_fee_cents "BR15"
    }
    DOCK {
        uuid id PK
        string name UK
        string district
        decimal latitude
        decimal longitude
        int berths_in_service "BR14, set by the municipality (US-27)"
    }
    BERTH {
        uuid id PK
        uuid dock_id FK
        int number "unique per dock, from 1"
        boolean has_charger
    }
    BOAT {
        uuid id PK
        string code UK "hull marking, e.g. B-16 (US-03)"
        string type_code FK
        string state "one of the 10 lifecycle states (3.3)"
        uuid home_dock_id FK
        uuid berth_id FK "UK, null on the canal; one boat per berth (BR14)"
        string unlock_code UK "printed below the QR code (US-12)"
        int battery_level "from telemetry, BR5 BR7"
        boolean plugged_in
        decimal latitude
        decimal longitude
        timestamptz last_reading_at
        bigint version "optimistic lock: a boat is never reserved twice (SLO4)"
    }

    %% ---------- Telemetry ----------
    TELEMETRY_READING {
        uuid id PK
        uuid boat_id FK
        timestamptz recorded_at
        int battery_level
        decimal latitude
        decimal longitude
        uuid berth_id FK "null when not moored"
        boolean locked
        boolean plugged_in
        string faults "null when none"
    }

    %% ---------- Trips ----------
    RESERVATION {
        uuid id PK
        uuid customer_id FK
        uuid boat_id FK
        string status "ACTIVE, CANCELLED, EXPIRED, CONVERTED"
        timestamptz created_at
        timestamptz expires_at "created_at + 15 min (BR8)"
        timestamptz ended_at
        int hold_fee_cents "BR8"
    }
    TRIP {
        uuid id PK
        uuid customer_id FK
        uuid boat_id FK
        uuid reservation_id FK "UK, null for an immediate trip"
        uuid start_dock_id FK
        uuid end_dock_id FK "null while in progress"
        string status "IN_PROGRESS, ENDED"
        string end_reason "AT_DOCK, ABANDONED, EMPTY_BATTERY, FAULT (BR15, BR16)"
        int passengers "BR4"
        timestamptz started_at
        timestamptz ended_at
        int max_duration_minutes "rules in force at the start (US-26)"
        int unlock_fee_cents "BR11"
        int normal_minutes
        int overtime_minutes "BR12"
        int hold_fee_cents "BR8"
        int discount_cents "BR23, BR24: one discount per trip"
        int total_cents
        uuid promotion_id FK "null, or the one discount applied"
    }
    PROBLEM_REPORT {
        uuid id PK
        uuid trip_id FK
        string category "ENGINE, HULL, BATTERY, OTHER (US-38)"
        string note
        timestamptz reported_at
    }

    %% ---------- Payments ----------
    PAYMENT {
        uuid id PK
        uuid customer_id FK
        uuid trip_id FK "null for a reservation fee"
        uuid reservation_id FK "null for a trip"
        string kind "DEPOSIT, CAPTURE, RELEASE, HOLD_FEE, RECOVERY_FEE"
        string status "AUTHORISED, CAPTURED, RELEASED, FAILED"
        int amount_cents
        string gateway_reference "Stripe payment intent id"
        timestamptz created_at
    }

    %% ---------- Maintenance ----------
    FIELD_TECHNICIAN {
        uuid id PK
        string idp_subject UK
        string name
    }
    TECHNICIAN_DOCK {
        uuid technician_id PK, FK
        uuid dock_id PK, FK
    }
    SLA_POLICY {
        string ticket_type PK "CHARGE, REPAIR, TOW, RETURN"
        int deadline_minutes "BR18"
        int on_site_minutes "TOW only: 30 (BR18)"
        boolean clock_runs_outside_hours "true only for TOW (BR18)"
    }
    TICKET {
        uuid id PK
        uuid boat_id FK
        uuid technician_id FK
        string type FK
        string status "OPEN, CLOSED"
        timestamptz opened_at
        timestamptz due_at "from SLA_POLICY (BR18)"
        timestamptz on_site_due_at "TOW only"
        timestamptz arrived_at "TOW only (US-25)"
        timestamptz closed_at "time-to-repair KPI"
        uuid problem_report_id FK "null unless opened by a user report"
    }
    TICKET_NOTE {
        uuid id PK
        uuid ticket_id FK
        uuid technician_id FK
        string text
        timestamptz created_at
    }

    %% ---------- Rules ----------
    USAGE_RULES {
        uuid id PK "single row, set by the municipality"
        int max_trip_minutes "BR12: 90"
        time opens_at "BR6: 08:00"
        time closes_at "BR6: 22:00"
        int reservation_hold_minutes "BR8: 15"
        int min_battery_percent "BR5: 30"
        int user_warn_battery_percent "BR7: 20"
        int operator_alert_battery_percent "BR7: 10"
        int max_open_tickets_per_technician "BR17"
        timestamptz updated_at "new values apply to the next trips"
    }

    %% ---------- Extensions ----------
    PROMOTION {
        uuid id PK
        string kind "NEW_CUSTOMER, SEASONAL"
        string name
        int discount_percent "BR21, BR22"
        timestamptz starts_at
        timestamptz ends_at
    }
    POINTS_ENTRY {
        uuid id PK
        uuid customer_id FK
        uuid trip_id FK "null for a redemption"
        int points "+ earned (BR25), - redeemed (BR26)"
        string reason "PLUGGED_IN, FREE_UNLOCK, FREE_MINUTES"
        timestamptz created_at
    }

    %% ---------- Relationships ----------
    CUSTOMER ||--o{ RESERVATION : makes
    CUSTOMER ||--o{ TRIP : takes
    CUSTOMER ||--o{ PAYMENT : pays
    CUSTOMER ||--o{ POINTS_ENTRY : earns

    BOAT_TYPE ||--o{ BOAT : "classifies"
    DOCK ||--|{ BERTH : "is composed of"
    DOCK ||--o{ BOAT : "is home of"
    BERTH |o--o| BOAT : "moors"
    BOAT ||--o{ TELEMETRY_READING : reports
    BERTH |o--o{ TELEMETRY_READING : "seen at"

    BOAT ||--o{ RESERVATION : "is held by"
    RESERVATION |o--o| TRIP : "becomes"
    BOAT ||--o{ TRIP : "is used in"
    DOCK ||--o{ TRIP : "starts at"
    DOCK |o--o{ TRIP : "ends at"
    TRIP ||--o{ PROBLEM_REPORT : "may report"

    TRIP |o--o{ PAYMENT : "is settled by"
    RESERVATION |o--o{ PAYMENT : "is charged by"
    PROMOTION |o--o{ TRIP : "discounts"
    TRIP |o--o{ POINTS_ENTRY : "earns"

    BOAT ||--o{ TICKET : "needs"
    FIELD_TECHNICIAN ||--o{ TICKET : "is assigned"
    SLA_POLICY ||--o{ TICKET : "sets the deadline of"
    PROBLEM_REPORT |o--o| TICKET : "opens"
    TICKET ||--o{ TICKET_NOTE : has
    FIELD_TECHNICIAN ||--o{ TICKET_NOTE : writes
    FIELD_TECHNICIAN ||--o{ TECHNICIAN_DOCK : covers
    DOCK ||--o{ TECHNICIAN_DOCK : "is covered by"
```

## Tables by module

| Module | Tables | Stories |
|---|---|---|
| Accounts | `customer` | US-01, US-02 |
| Fleet | `boat_type`, `dock`, `berth`, `boat` | US-03 to US-06, US-27 |
| Telemetry | `telemetry_reading` | US-07, US-08 |
| Trips | `reservation`, `trip`, `problem_report` | US-09 to US-15, US-38 |
| Payments | `payment` (debt in `customer.debt_cents`) | US-16 to US-20 |
| Maintenance | `field_technician`, `technician_dock`, `sla_policy`, `ticket`, `ticket_note` | US-21 to US-25, US-28, US-39 |
| Rules | `usage_rules` (age and prices per type in `boat_type`) | US-26 |
| Extensions | `promotion`, `points_entry` (balance in `customer.points_balance`) | US-33 to US-36 |

## Constraints the database must guard

- `boat.code` and `boat.unlock_code` are unique (US-03, US-12).
- `boat.berth_id` is unique: one boat per berth (BR14).
- `berth (dock_id, number)` is unique.
- `trip.reservation_id` is unique: a reservation becomes at most one trip.
- At most one active boat per customer (BR3): a partial unique index on `reservation (customer_id) WHERE status = 'ACTIVE'` and on `trip (customer_id) WHERE status = 'IN_PROGRESS'`, checked together with the boat in one transaction.
- `boat.version` is the optimistic lock that lets exactly one of many simultaneous reservations of the same boat succeed (SLO4).
