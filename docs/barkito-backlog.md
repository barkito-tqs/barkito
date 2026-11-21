# Barkito: product backlog (proposal)

Epics and user stories derived from the Product Specification report (v2026-11-17): the persona scenarios (2.2), the roadmap (2.3), the business rules (3.2) and the boat lifecycle (3.3). This is a proposal to be reviewed by the Product Owner before it goes into Jira.

Conventions, following the QA Manual (2.2):

- Stories use the format "As a <role>, I want <goal>, so that <benefit>".
- Acceptance criteria are tagged with business rule IDs (BRx) and give the boundary values from section 3.2. Gherkin scenarios are written later, in the repository's `.feature` files.
- Story points use the 1–5 scale, with "Register a boat" (2 points) as the reference. Stories of 5 points are split before entering a sprint.
- All stories start in **To Do**: apart from the walking skeleton, no feature is implemented yet.

## Summary

| ID | Epic | Story | Rules | Pts | It. |
|---|---|---|---|---|---|
| US-01 | Accounts and roles | Register and sign in | BR1 | 3 | I2 |
| US-02 | Accounts and roles | Role-based areas | — | 2 | I2 |
| US-03 | Fleet and docks | Register a boat | — | 2 | I2 |
| US-04 | Fleet and docks | Register docks and berths | — | 2 | I2 |
| US-05 | Fleet and docks | Activate and deactivate a boat | BR5, BR19 | 3 | I2 |
| US-06 | Fleet and docks | Retire a boat | — | 2 | I3 |
| US-07 | Telemetry | Boat state updated from telemetry | BR5 | 3 | I2 |
| US-08 | Telemetry | Low-battery alerts during a trip | BR7 | 2 | I3 |
| US-09 | Booking and trips | Find available boats by dock and type | BR5 | 3 | I2 |
| US-10 | Booking and trips | Reserve a boat | BR1–BR3, BR5, BR6, BR8 | 3 | I3 |
| US-11 | Booking and trips | Cancel or expire a reservation | BR8, BR9, BR10 | 3 | I3 |
| US-12 | Booking and trips | Unlock a boat | BR1–BR6, BR8, BR13 | 3 | I3 |
| US-13 | Booking and trips | End a trip at a dock | BR6, BR14, BR20 | 3 | I3 |
| US-14 | Booking and trips | Abandoned boat ends the trip | BR15 | 3 | I4 |
| US-15 | Booking and trips | Empty battery or fault ends the trip | BR16 | 2 | I4 |
| US-16 | Payments | Save a payment method | BR2 | 3 | I3 |
| US-17 | Payments | Deposit held and settled | BR2, BR13 | 3 | I3 |
| US-18 | Payments | Trip price | BR8, BR11 | 2 | I3 |
| US-19 | Payments | Hold fee for cancelled or expired reservations | BR8 | 2 | I3 |
| US-20 | Payments | Overtime | BR12 | 3 | I4 |
| US-21 | Maintenance | Tickets opened and assigned automatically | BR17, BR18 | 3 | I3 |
| US-22 | Maintenance | Technician works on own tickets | BR18 | 3 | I3 |
| US-23 | Maintenance | Operator reassigns a ticket | BR17 | 1 | I3 |
| US-24 | Maintenance | Overdue tickets alert the operator | BR18 | 2 | I4 |
| US-25 | Maintenance | Towing and recovery | BR16, BR17, BR18 | 3 | I4 |
| US-26 | Client backoffice | Set the rules of use | BR1, BR6, BR12 | 3 | I4 |
| US-27 | Client backoffice | Set berths in service per dock | BR14 | 2 | I4 |
| US-28 | Client backoffice | Set SLA deadlines | BR18 | 2 | I4 |
| US-29 | Dashboards and KPIs | User dashboard | BR11, BR12 | 3 | I4 |
| US-30 | Dashboards and KPIs | Operator dashboard | — | 3 | I4 |
| US-31 | Dashboards and KPIs | Client business KPIs | — | 3 | I4 |
| US-32 | Dashboards and KPIs | Client technical metrics | — | 3 | I4 |
| US-33 | Extensions | Earn points for plugging the boat in | BR25 | 2 | I5 |
| US-34 | Extensions | Exchange points for rewards | BR24, BR26 | 2 | I5 |
| US-35 | Extensions | New-customer promotion | BR21, BR23, BR24 | 3 | I5 |
| US-36 | Extensions | Seasonal campaign | BR22, BR23, BR24 | 3 | I5 |
| US-37 | Extensions | Public GBFS feed | — | 3 | I5 |

Iteration 2 total: 7 stories, 18 points.

---

## Epic: Accounts and roles

### US-01 Register and sign in
**As an** end user, **I want** to create an account and sign in, **so that** I can reserve and unlock boats.

- Sign-up and sign-in go through the identity provider (OpenID Connect); on first sign-in, Barkito creates a local profile with name, email and date of birth.
- The date of birth is stored so that the minimum age can be checked later (BR1).
- A new account gets the end-user role.
- A request to a protected endpoint without a valid token is refused with 401.

**Points:** 3 · **Iteration:** I2

### US-02 Role-based areas
**As a** user with a role, **I want** to see only the area of my role, **so that** each actor can do only what they are allowed to.

- The roles are end user, operator, technician, client manager and admin, as set in the identity provider.
- A request to an endpoint of another role is refused with 403.
- The web UI shows only the menu and pages of the user's role.

**Points:** 2 · **Iteration:** I2

## Epic: Fleet and docks

### US-03 Register a boat (reference story)
**As an** operator, **I want** to register a boat with its code, type and home dock, **so that** it joins the fleet.

- The type is Standard or XL.
- The code is unique: a duplicate code is refused with 409.
- The home dock must exist.
- A new boat starts INACTIVE (lifecycle transition 1) and appears in the operator's fleet list.

**Points:** 2 · **Iteration:** I2

### US-04 Register docks and berths
**As an** operator, **I want** to register a dock with its location and berths, **so that** boats have places where trips start and end.

- A dock has a name, a location and at least one berth.
- Berths are numbered from 1 and are in service by default; the municipality changes the berths in service later (US-27).

**Points:** 2 · **Iteration:** I2

### US-05 Activate and deactivate a boat
**As an** operator, **I want** to activate and deactivate a boat, **so that** I control which boats are in service.

- An INACTIVE boat can be activated only when telemetry reports it moored in a berth and plugged in; otherwise activation is refused with 409 (BR19).
- On activation the boat becomes CHARGING, and AVAILABLE once its battery is ≥ 30 %: 29 % stays CHARGING, 30 % becomes AVAILABLE (BR19, BR5).
- An AVAILABLE or CHARGING boat can be deactivated and becomes INACTIVE.
- Deactivating an IN_USE or RESERVED boat is refused with 409 (BR19).
- An INACTIVE boat is not shown to end users.

**Points:** 3 · **Iteration:** I2

### US-06 Retire a boat
**As an** operator, **I want** to retire a boat with a reason, **so that** boats that are lost or beyond repair leave the fleet for good.

- A boat can be retired from INACTIVE, REPAIR_REQUIRED (irreparable), TOW_REQUIRED (lost or sunk) or RETURN_REQUIRED (lost or stolen); from any other state the request is refused.
- RETIRED is final: no transition leaves it.
- Retired boats are not shown to end users.

**Points:** 2 · **Iteration:** I3

## Epic: Telemetry and fleet simulator

### US-07 Boat state updated from telemetry
**As an** operator, **I want** each boat's battery, position, charging state and faults to be updated from telemetry, **so that** the fleet state in Barkito matches the real boats.

- Readings arrive through the TelemetrySource port and are validated against the shared JSON Schema; invalid readings are rejected and logged.
- The latest battery level, position or berth, lock state, charging state and faults are stored for each boat.
- A CHARGING boat becomes AVAILABLE when its battery reaches 30 % (transition 15).
- A fault on an AVAILABLE or CHARGING boat moves it to REPAIR_REQUIRED (transitions 16 and 18).
- A reading for an unknown boat is ignored and logged.

**Points:** 3 · **Iteration:** I2

### US-08 Low-battery alerts during a trip
**As an** end user and as an operator, **I want** to be warned when a boat's battery runs low during a trip, **so that** the user can return to a dock in time and the operator can act.

- At 20 % or less the user is warned; at 21 % there is no warning (BR7).
- At 10 % or less the operator is alerted; at 11 % there is no alert (BR7).
- Each alert is sent once per trip, over WebSocket.

**Points:** 2 · **Iteration:** I3

## Epic: Booking and trips

### US-09 Find available boats by dock and type (specification-driven, AI4)
**As an** end user, **I want** to see the boats available at each dock, by type, **so that** I know where to go before I walk to a dock.

- The list can be filtered by dock and by type (Standard or XL).
- Only AVAILABLE boats with battery at or above 30 % can be booked: 29 % is not bookable, 30 % is (BR5).
- Each boat shows its type and battery level.
- A dock with no bookable boats shows none.
- Performance target: SLO1.

**Points:** 3 · **Iteration:** I2

### US-10 Reserve a boat
**As an** end user, **I want** to reserve a boat for up to 15 minutes, **so that** it is still there when I reach the dock.

- Minimum age: Standard 17 refused / 18 accepted; XL 20 refused / 21 accepted (BR1).
- A valid payment method and no debt are required: debt 0.00 € accepted / 0.01 € refused (BR2).
- An account can hold only one active boat; a second reservation is refused while one is active (BR3).
- The boat must be AVAILABLE with battery ≥ 30 % (BR5).
- Reservations are accepted until 21:45; at 21:46 they are refused (BR6).
- The boat becomes RESERVED and is held for 15 minutes (BR8).
- When several requests for the same boat arrive at once, exactly one succeeds and the others get 409 (SLO4).

**Points:** 3 · **Iteration:** I3

### US-11 Cancel or expire a reservation
**As an** end user, **I want** to cancel a reservation I no longer need, **so that** I pay only for the minutes the boat was held.

- On cancellation, the user pays the started minutes of hold: Standard 0.25 €/min, XL 0.50 €/min (BR8).
- A reservation not unlocked by minute 15:00 becomes EXPIRED and pays the maximum: Standard 3.75 €, XL 7.50 € (BR8).
- After cancellation or expiry, the boat returns to AVAILABLE.
- After two expired reservations within 24 hours, new reservations are blocked until 00:00; one expired reservation does not block. Unlocking without a reservation is still allowed (BR9).
- If the reserved boat reports a fault, the reservation is cancelled with a 0.00 € fee and the boat goes to REPAIR_REQUIRED (BR10).

**Points:** 3 · **Iteration:** I3

### US-12 Unlock a boat
**As an** end user, **I want** to unlock a boat, reserved or not, **so that** I can start my trip.

- A reserved boat can be unlocked only by the user who reserved it, and the hold fee is added to the trip (BR8).
- Without a reservation, the checks of BR1, BR2, BR3, BR5 and BR6 apply.
- The number of passengers cannot exceed the capacity: Standard 5 accepted / 6 refused; XL 10 accepted / 11 refused (BR4).
- Unlocking is accepted until 21:59; at 22:00 it is refused (BR6).
- The deposit is authorised before the boat is unlocked (BR13, US-17).
- An unlock command is sent through the BoatControl port, and the boat becomes IN_USE.

**Points:** 3 · **Iteration:** I3

### US-13 End a trip at a dock
**As an** end user, **I want** to end my trip at any dock with a free berth, **so that** I stop paying and leave the boat where others can use it.

- A trip ends only when telemetry reports the boat moored in a free berth in service; otherwise the request is refused with 409 and the app shows the nearest dock with a free berth (BR14).
- Two boats cannot end a trip in the same berth (BR14).
- A lock command is sent, and the charges are settled (US-17, US-18).
- After the trip, the boat state follows BR20: a fault or reported problem gives REPAIR_REQUIRED (takes priority); otherwise, plugged in gives CHARGING (AVAILABLE once at 30 %); not plugged in gives CHARGE_REQUIRED at 29 % and AVAILABLE at 30 %.
- A trip in progress at 22:00 continues and can be ended after closing time (BR6).

**Points:** 3 · **Iteration:** I3

### US-14 Abandoned boat ends the trip
**As an** operator, **I want** a boat left stationary during a trip to be treated as abandoned, **so that** the user stops being charged and the boat can be recovered.

- After 5 minutes stationary, the user is warned (BR15).
- After 10 minutes, the trip ends and is charged until the moment the boat stopped (BR15).
- If the boat is outside a dock, it goes to RETURN_REQUIRED and a recovery fee is taken from the deposit: Standard 20 €, XL 40 € (BR15).

**Points:** 3 · **Iteration:** I4

### US-15 Empty battery or fault ends the trip
**As an** end user, **I want** my trip to end when the boat runs out of battery or breaks down on the canal, **so that** I am not charged for time I cannot use the boat.

- When the battery reaches 0 % or a fault immobilises the boat outside a dock, the trip ends at that moment and is charged until then, with no towing fee (BR16).
- The boat goes to TOW_REQUIRED (BR16).
- A RETURN_REQUIRED boat whose battery runs out also goes to TOW_REQUIRED (transition 22).

**Points:** 2 · **Iteration:** I4

## Epic: Payments

### US-16 Save a payment method
**As an** end user, **I want** to save a card in my account, **so that** I can reserve and unlock boats without typing it every time.

- The backend creates a setup intent and the web UI confirms it with Stripe.js; card details never reach the backend.
- The backend stores only the resulting payment method ID.
- Without a saved payment method, reserving and unlocking are refused (BR2).

**Points:** 3 · **Iteration:** I3

### US-17 Deposit held and settled
**As an** operator, **I want** a deposit to be held on the user's card during each trip, **so that** the trip is paid even if something goes wrong.

- On unlock, a deposit is authorised with manual capture: Standard 30 €, XL 60 € (BR13).
- At the end of the trip, the amount due is captured up to the deposit and the rest is released (BR13).
- Any amount above the deposit becomes debt, which blocks new reservations and unlocks (BR2, BR13).

**Points:** 3 · **Iteration:** I3

### US-18 Trip price
**As an** end user, **I want** to pay an unlock fee plus a price per started minute, **so that** the price matches how long I used the boat.

- Price = unlock fee + price per started minute: Standard 1 € + 0.50 €/min; XL 2 € + 1.00 €/min (BR11).
- Example: Standard, 12 min 20 s counts as 13 min and costs 7.50 € (BR11).
- The hold fee of a reservation is added to the trip (BR8).

**Points:** 2 · **Iteration:** I3

### US-19 Hold fee for cancelled or expired reservations
**As an** operator, **I want** the hold fee of cancelled and expired reservations to be charged to the saved payment method, **so that** holding a boat is paid even when no trip follows.

- The fee from US-11 is charged off-session to the saved payment method, since no deposit exists yet (BR8).
- A failed charge becomes debt (BR2).

**Points:** 2 · **Iteration:** I3

### US-20 Overtime
**As an** end user, **I want** to be warned before my trip reaches the maximum duration, **so that** I can avoid paying overtime.

- The maximum trip duration is 90 minutes by default, set by the municipality (BR12).
- The user is warned at 80 and at 90 minutes (BR12).
- From minute 91, minutes are charged as overtime: Standard 0.75 €/min, XL 1.50 €/min (BR12).
- The trip shows unlock fee, normal minutes and overtime separately.

**Points:** 3 · **Iteration:** I4

## Epic: Maintenance

### US-21 Tickets opened and assigned automatically
**As an** operator, **I want** a ticket to be opened and assigned automatically whenever a boat needs a technician, **so that** every problem has an owner and a deadline.

- A ticket of type CHARGE, REPAIR, TOW or RETURN is opened when a boat enters CHARGE_REQUIRED, REPAIR_REQUIRED, TOW_REQUIRED or RETURN_REQUIRED.
- The ticket is assigned to the Field Technician of the boat's dock, or of the nearest dock when the boat is outside one (BR17).
- The ticket gets an SLA deadline from the SLA policy for its type (BR18).

**Points:** 3 · **Iteration:** I3

### US-22 Technician works on own tickets
**As a** Field Technician, **I want** to see my tickets ordered by SLA deadline and record what I did, **so that** I handle the most urgent problems first without going back to an office.

- A technician sees only the tickets assigned to them, ordered by SLA deadline.
- A technician can add notes and leave a ticket open, for example when a repair needs the workshop.
- Closing a CHARGE ticket (boat plugged in) moves the boat to CHARGING (transition 14); closing a REPAIR ticket moves it to CHARGING (transition 19).
- The closing time is recorded for the time-to-repair KPI.

**Points:** 3 · **Iteration:** I3

### US-23 Operator reassigns a ticket
**As an** operator, **I want** to reassign a ticket to another technician, **so that** work continues when the assigned technician is unavailable.

- The operator can reassign any open ticket to another technician (BR17).
- The new technician sees it in their list.

**Points:** 1 · **Iteration:** I3

### US-24 Overdue tickets alert the operator
**As an** operator, **I want** to be alerted when a ticket misses its SLA deadline, **so that** I can act before the SLA with the municipality is broken.

- SLA deadlines: TOW on site in 30 min and closed in 2 h; CHARGE 2 h; RETURN 4 h; REPAIR 24 h (BR18).
- The SLA clock runs only within operating hours, except for TOW tickets, whose clock always runs (BR18).
- A ticket closed at 2:00:00 meets a 2-hour SLA; at 2:00:01 it is breached, marked overdue and the operator is alerted (BR18).

**Points:** 2 · **Iteration:** I4

### US-25 Towing and recovery
**As a** Field Technician, **I want** to record that I towed or brought back a boat left on the canal, **so that** it returns to service or goes to repair.

- A towed boat with no fault becomes CHARGING; with a fault, REPAIR_REQUIRED (transitions 24 and 25).
- A boat brought back from RETURN_REQUIRED becomes CHARGING (transition 23).
- Closing the ticket follows the SLA rules of US-24.

**Points:** 3 · **Iteration:** I4

## Epic: Client backoffice

### US-26 Set the rules of use
**As a** client manager, **I want** to set the maximum trip duration, the operating hours and the minimum age per boat type, **so that** the service follows the city's rules.

- The client manager can change the maximum duration (default 90 min), the operating hours (default 08:00–22:00) and the minimum age per type (default Standard 18, XL 21) (BR1, BR6, BR12).
- New values apply to the next trips; trips in progress keep the values they started with.

**Points:** 3 · **Iteration:** I4

### US-27 Set berths in service per dock
**As a** client manager, **I want** to set how many berths are in service at each dock, **so that** crowded docks do not fill up.

- The berths in service cannot exceed the dock's physical berths.
- Once the berths in service are full, no trip can end at that dock (BR14).

**Points:** 2 · **Iteration:** I4

### US-28 Set SLA deadlines
**As a** client manager, **I want** to set the SLA deadline for each ticket type, **so that** the operator works to the service levels agreed in the contract.

- The deadline of each ticket type (TOW, CHARGE, RETURN, REPAIR) can be changed (BR18).
- New deadlines apply to tickets opened afterwards.

**Points:** 2 · **Iteration:** I4

## Epic: Dashboards and KPIs

### US-29 User dashboard
**As an** end user, **I want** to see my active and past trips with their charges, **so that** I know what I paid and why.

- The dashboard shows the active trip, if any, with its running time.
- Past trips show unlock fee, normal minutes, overtime and hold fee separately (BR11, BR12).

**Points:** 3 · **Iteration:** I4

### US-30 Operator dashboard
**As an** operator, **I want** to see the state of the fleet and the open tickets at a glance, **so that** I can plan the day's work.

- Boats are shown by state and by dock.
- Open tickets are shown with their technician and SLA deadline; overdue tickets are highlighted.
- The dashboard updates live over WebSocket.

**Points:** 3 · **Iteration:** I4

### US-31 Client business KPIs
**As a** client manager, **I want** to see the business KPIs of the service, **so that** I can report to the city council with evidence.

- The KPIs are utilisation, availability against the SLA, trips per day and time to repair, for a chosen period.
- The figures can be exported.

**Points:** 3 · **Iteration:** I4

### US-32 Client technical metrics
**As a** client manager, **I want** to see the platform's technical metrics, **so that** I can check that the service is reliable.

- The metrics are API latency, error rate and telemetry lag, read from the metrics store through the MetricsSource port.

**Points:** 3 · **Iteration:** I4

## Epic: Extensions (only after the MVP core is accepted)

### US-33 Earn points for plugging the boat in
**As an** end user, **I want** to earn points when I plug the boat in at the end of a trip, **so that** I am rewarded for helping charge the fleet.

- Standard 1 point/min, XL 2 points/min (BR25).
- Overtime and hold minutes earn no points; a boat not plugged in earns 0 points (BR25).

**Points:** 2 · **Iteration:** I5

### US-34 Exchange points for rewards
**As an** end user, **I want** to exchange my points for a free unlock or free minutes, **so that** my points have value.

- Free unlock: Standard 20 points, XL 40 points; 10 free minutes: Standard 100 points, XL 200 points (BR26).
- Points do not expire (BR26).
- Only one discount applies per trip, the largest; the others are not consumed (BR24).

**Points:** 2 · **Iteration:** I5

### US-35 New-customer promotion
**As an** operator, **I want** new customers to get a discount on their first trips, **so that** they try the service.

- 10 % off the first completed trip within 30 days of sign-up: day 30 accepted / day 31 refused (BR21).
- 10 % off the next trip if it starts within 24 hours: 24:00:00 accepted / 24:00:01 refused (BR21).
- Discounts apply only to the unlock fee and normal minutes (BR23); only one discount per trip (BR24).

**Points:** 3 · **Iteration:** I5

### US-36 Seasonal campaign
**As an** operator, **I want** to run a seasonal campaign for long-standing customers, **so that** they keep using the service.

- One-time discount (e.g. 25 %) on one trip started within the campaign period (BR22).
- Eligible accounts are older than 30 days with at least 3 completed trips; 30 days or 2 trips are refused (BR22).
- Discounts apply only to the unlock fee and normal minutes (BR23); only one discount per trip (BR24).

**Points:** 3 · **Iteration:** I5

### US-37 Public GBFS feed
**As an** open-data consumer, **I want** a public GBFS feed of the fleet, **so that** other apps can show Barkito boats.

- The feed follows GBFS v3.0 and is validated against the official JSON schema (contract test, practice T9).

**Points:** 3 · **Iteration:** I5

---

## Technical tasks for Iteration 2

These have no direct user value, so they are Tasks under the epic "Quality engineering and DevOps" (together with the existing tasks BRK-6 to BRK-22), except the first two, which belong to the Telemetry epic.

- Minimal fleet simulator in its own container, publishing telemetry over MQTT.
- MQTT telemetry consumer behind the TelemetrySource port, with the JSON Schema in the repository.
- SonarQube with JaCoCo coverage, and the quality gate as a required check on the protected branches.
- Xray set up, with the first tests linked to stories and CI results imported.
- Cucumber set up, with the Gherkin scenarios of US-09 merged before its generated pull request (AI4).
- AGENTS.md in the repository (AI2).
- QA Manual v1: compliance table, 2.3, 3.3, 3.4, 4.2, 5.1–5.4, 6.2–6.4.
- Repository tag `iter2` with both reports as PDF in `docs/`.

## Open points for the Product Owner

- **Reservation or booking:** the domain model and rules say Reservation, while section 4.3 and the QA Manual (3.1) say Booking. Choose one term for code, Jira and Gherkin.
- **Reservation closing time:** BR6 closes reservations 15 minutes before the end of operating hours (21:45). Decide whether this margin is fixed or set by the municipality with the operating hours (US-26).
