# Barkito

Shared electric boats on city canals: an operator runs the fleet on behalf of an institutional client, and end users book and ride the boats between docks. This file is the glossary: use these words in code, tests, UI text, commits and stories. The rules attached to each term (limits, prices, deadlines) are in [`business-rules.md`](business-rules.md).

## People and roles

**End user**:
A person with an account who finds, reserves, unlocks and rides boats.
_Avoid_: Customer, rider, renter, client

**Operator**:
The company that runs the fleet, and the role of its staff in Barkito.
_Avoid_: Admin, fleet manager

**Field Technician**:
Operator staff who handle the tickets of the docks they cover. Role name: `TECHNICIAN`.
_Avoid_: Mechanic, maintenance user

**Client**:
The institution (the municipality) that contracts the service and sets the rules of use.
_Avoid_: Customer, tenant

**Client manager**:
The client's staff who set the rules of use and follow the KPIs.
_Avoid_: Municipality user, city admin

**Admin**:
The role that administers the platform itself, not the fleet.

## Fleet

**Fleet**:
Every boat the operator runs.

**Boat**:
A small electric boat of the fleet.
_Avoid_: Vessel, vehicle, bike

**Boat code**:
The code painted on the hull that identifies a boat.
_Avoid_: Boat id, plate, name

**Unlock code**:
The code under a boat's QR code that an end user scans or types to unlock it. It is not the boat code.
_Avoid_: QR id, PIN

**Boat type**:
The class of a boat, `STANDARD` or `XL`. Capacity, minimum age and prices depend on it.
_Avoid_: Model, size, category

**Dock**:
A hub on the canal where trips start and end. It has a name, a location and berths.
_Avoid_: Station, hub, marina, port

**Home dock**:
The dock a boat is registered to.

**Berth**:
A numbered place at a dock where one boat moors and charges. Every berth has a charger.
_Avoid_: Slot, spot, mooring, bay

**Berth in service**:
A berth that can take a boat at the end of a trip. The client decides which berths are in service.
_Avoid_: Active berth, enabled berth

**Free berth**:
A berth in service with no boat moored in it.

**Plugged in**:
A moored boat connected to its berth's charger.
_Avoid_: Charging (that is a boat state)

## Boat state and telemetry

**Boat state**:
Where a boat is in its lifecycle: `INACTIVE`, `CHARGING`, `AVAILABLE`, `RESERVED`, `IN_USE`, `CHARGE_REQUIRED`, `REPAIR_REQUIRED`, `TOW_REQUIRED`, `RETURN_REQUIRED`, `RETIRED`. Barkito decides it.
_Avoid_: Status (that is the telemetry reading)

**Telemetry reading**:
What a boat reports about itself: battery level, berth, position, locked, plugged in, faults. The latest reading of a boat is its **boat status**.
_Avoid_: Event, ping, heartbeat

**Battery level**:
The charge of a boat's battery, in percent.
_Avoid_: Charge, autonomy, battery

**Fault**:
A problem the boat reports in a telemetry reading.
_Avoid_: Error, alarm

**Bookable**:
A boat that end users can see and reserve or unlock.
_Avoid_: Free, ready

**Activate / deactivate**:
The operator puts an inactive boat into service, or takes it out of service.
_Avoid_: Enable, disable

**Retire**:
To take a boat out of the fleet for good, with a reason.
_Avoid_: Delete, remove, decommission

## Reservations and trips

**Reservation**:
An end user holds a bookable boat for a short time before unlocking it.
_Avoid_: Booking, hold

**Hold fee**:
What the end user pays for the minutes a reservation held the boat.
_Avoid_: Reservation fee, cancellation fee

**Unlock**:
An end user opens a boat with its unlock code, which starts a trip.
_Avoid_: Start, check-out, rent

**Trip**:
The use of a boat from unlock to its end.
_Avoid_: Ride, rental, journey, session

**Passengers**:
The number of people on board for a trip, limited by the boat type.

**Abandoned boat**:
A boat left stationary during a trip, which ends the trip.

**Overtime**:
The minutes of a trip beyond the maximum trip duration.

**Problem report**:
What an end user reports about a boat when they end a trip.
_Avoid_: Complaint, issue

## Payments

**Payment method**:
The card an end user saves in their account.
_Avoid_: Wallet, card on file

**Unlock fee**:
The fixed part of a trip's price.
_Avoid_: Base fee, start fee

**Deposit**:
An amount authorised on the end user's card at unlock and settled when the trip ends.
_Avoid_: Pre-authorisation, caution, guarantee

**Debt**:
What an end user owes when a charge was not covered.
_Avoid_: Balance, arrears

## Maintenance

**Ticket**:
Work for a Field Technician on one boat, of type `CHARGE`, `REPAIR`, `TOW` or `RETURN`.
_Avoid_: Task, job, work order, issue

**SLA deadline**:
The time by which a ticket must be closed, set per ticket type.
_Avoid_: Due date, timeout

**Covered docks**:
The docks a Field Technician is assigned to.

## Client backoffice

**Rules of use**:
The parameters the client sets for the service: maximum trip duration, operating hours, minimum age per boat type.
_Avoid_: Settings, config, policy

**Operating hours**:
The daily period in which boats can be reserved and unlocked.

**KPI**:
A business or technical indicator the client manager follows.

## Extensions (after the MVP)

**Points**:
What an end user earns, for example by plugging the boat in, and exchanges for rewards.
_Avoid_: Credits, coins

**Promotion**:
A discount for new end users or a seasonal campaign.
_Avoid_: Coupon, voucher
