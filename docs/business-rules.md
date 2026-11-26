# Business rules

Every rule Barkito enforces, whatever the interface, and the boat lifecycle. Stories, Gherkin tags (`@BR5`), Xray and code comments cite rules by ID; look the ID up here. Use the parameters as named constants, never as literals.

Source: Product Specification, sections 3.2 and 3.3. This file is the reference for implementation; change a rule here and in the specification together.

- The values are defaults. The client sets the rules of use and the agreed SLAs (BR1, BR6, BR12, BR14, BR18) in the client backoffice. The operator sets every other parameter.
- Durations are charged per started minute.
- BR21–BR26 belong to extensions planned for later iterations.
- Not yet in this table: a dock has 1 to 10 berths (agreed for US-04, `Dock.MIN_BERTHS` / `Dock.MAX_BERTHS`).

## Rules

| ID | Rule | Parameters and boundaries | Covered by |
|---|---|---|---|
| BR1 | The driver must meet the minimum age for the boat type | Standard ≥ 18, XL ≥ 21: 17 refused / 18 accepted; XL 20 refused / 21 accepted | Unit; pairwise; @BR1 |
| BR2 | An account can reserve or unlock a boat only with a valid payment method and no pending debt | Debt 0.00 € accepted / 0.01 € refused | Unit; @BR2 |
| BR3 | An account can have at most one active boat (reserved or in use) | A 2nd reserve or unlock is refused while one is active; accepted after it ends | Unit; concurrency integration test; @BR3 |
| BR4 | The declared number of passengers cannot exceed the boat capacity | Standard 1–5 (5 accepted / 6 refused); XL 1–10 (10 accepted / 11 refused) | Unit; pairwise; @BR4 |
| BR5 | A boat can be reserved or unlocked only if it is available and its battery is at or above the minimum | Minimum 30 %: 29 % refused / 30 % accepted | Unit; telemetry integration; @BR5 |
| BR6 | Boats can be unlocked only within operating hours, and reserved only until one reservation hold (BR8) before closing time | Default 08:00–22:00: unlock at 07:59 refused / 08:00 accepted, 21:59 accepted / 22:00 refused; reserve at 21:45 accepted / 21:46 refused (closing time minus 15 min); a trip in progress at 22:00 continues | Unit; @BR6 |
| BR7 | Low battery during a trip triggers alerts | ≤ 20 %: user warned; ≤ 10 %: operator alerted (21 % / 20 %, 11 % / 10 %) | Unit; telemetry integration; @BR7 |
| BR8 | A reservation holds a boat for up to 15 minutes and is charged per started minute of hold | Standard 0.25 €/min (max 3.75 €), XL 0.50 €/min (max 7.50 €). Cancel: pays elapsed minutes; unlock: hold fee added to the trip; no unlock after 15 min 00 s of hold (14:59 unlocks / 15:00 expires) → EXPIRED, pays the maximum | Unit; @BR8 |
| BR9 | Two expired reservations within 24 hours block new reservations until the end of the day | 1 expired: can reserve; 2 expired: blocked until 00:00; unlocking without a reservation is still allowed | Unit; @BR9 |
| BR10 | If a reserved boat reports a fault, the reservation is cancelled with no fee | Hold fee 0.00 € | Unit; telemetry integration; @BR10 |
| BR11 | Trip price = unlock fee + price per started minute | Standard 1 € + 0.50 €/min; XL 2 € + 1.00 €/min. Example: Standard, 12 min 20 s → 13 min → 7.50 € | Unit; pairwise; @BR11 |
| BR12 | A trip has a maximum duration; extra minutes are charged as overtime | Maximum 90 min; from minute 91: Standard 0.75 €/min, XL 1.50 €/min; warnings at 80 and 90 min | Unit; @BR12 |
| BR13 | A pre-authorisation (deposit) is held when the boat is unlocked and released at the end minus the amounts due | Standard 30 €, XL 60 €; any unpaid remainder becomes debt (see BR2) | Unit; payment adapter integration; @BR13 |
| BR14 | A trip can end only when telemetry reports the boat moored in a free berth of a dock | Not moored in a berth → end refused, and the app shows the nearest dock with a free berth. Berths in service are set by the municipality and each berth holds one boat, so two boats cannot end a trip in the same berth | Unit; telemetry integration; @BR14 |
| BR15 | A boat stationary during an active trip is treated as abandoned | 5 min: user warned; 10 min: trip ends, charged until the boat stopped. Outside a dock: boat → RETURN_REQUIRED, recovery fee Standard 20 €, XL 40 €, taken from the deposit | Unit; telemetry integration; @BR15 |
| BR16 | If the battery reaches 0 % or a fault immobilises the boat outside a dock, the trip ends at that moment | Charged until that moment, no towing fee; boat → TOW_REQUIRED | Unit; telemetry integration; @BR16 |
| BR17 | Each ticket is assigned automatically to the Field Technician who covers the boat's dock, or to the technician of the nearest covered dock; a technician with too many open tickets is skipped | A technician covers one or more docks and a dock may have none. A dock with no technician, or a boat outside a dock → technician of the nearest covered dock. A technician at the maximum of open tickets set by the operator gets no new ticket: it goes to the technician with the fewest open tickets (nearest on a tie). The operator can reassign a ticket | Unit; @BR17 |
| BR18 | Each ticket type has an SLA deadline; the clock runs only within operating hours, except for towing | TOW: on site in 30 min, closed in 2 h (always counts); CHARGE 2 h; RETURN 4 h; REPAIR 24 h. Closed at 2:00:00 met / 2:00:01 breached → overdue + operator alert | Unit; @BR18 |
| BR19 | A boat can be activated only when moored in a berth and plugged in, and deactivated only when available or charging | Activation: not moored or not plugged in → refused; on activation the boat becomes CHARGING, and AVAILABLE once its battery is ≥ 30 % (29 % stays CHARGING / 30 % → AVAILABLE). Deactivating an IN_USE or RESERVED boat → refused | Unit; telemetry integration; @BR19 |
| BR20 | When a trip ends at a dock, or a boat returns to service at a dock after a repair, a tow or a recovery, its state follows faults, the charger and the battery (see 3.3) | Fault or reported problem → REPAIR_REQUIRED (takes priority); otherwise plugged in → CHARGING (then AVAILABLE at ≥ 30 %); not plugged in: 29 % → CHARGE_REQUIRED / 30 % → AVAILABLE | Unit; telemetry integration; @BR20 |
| BR21 | New end users get 10 % off their first completed trip and 10 % off the next trip if it starts within 24 hours | First trip within 30 days of sign-up (day 30 accepted / day 31 refused); 2nd trip at 24:00:00 accepted / 24:00:01 refused | Unit; @BR21 |
| BR22 | A seasonal campaign gives a one-time discount to long-standing end users on one trip started within the campaign period | e.g. 25 %; account older than 30 days and ≥ 3 completed trips (30 days or 2 trips refused); the trip start time decides | Unit; pairwise; @BR22 |
| BR23 | Discounts apply only to the unlock fee and normal minutes | Never to overtime, hold fees, expired reservations or recovery fees | Unit; @BR23 |
| BR24 | Only one discount applies per trip: the largest available; the others are not consumed | Promotion vs reward: the larger is applied, the other stays available | Unit; @BR24 |
| BR25 | Points are earned only when a trip ends at a dock and the boat is plugged in | Standard 1 pt/min, XL 2 pt/min; overtime and hold minutes earn nothing; not plugged in → 0 points | Unit; telemetry integration; @BR25 |
| BR26 | Points can be exchanged for trip rewards and do not expire | Free unlock: Standard 20 pts, XL 40 pts; 10 free minutes: Standard 100 pts, XL 200 pts | Unit; @BR26 |

## Boat lifecycle

A boat is always in exactly one of ten states. At a dock it is INACTIVE, AVAILABLE, RESERVED, CHARGING, CHARGE_REQUIRED or REPAIR_REQUIRED; on the canal it is IN_USE, TOW_REQUIRED or RETURN_REQUIRED; RETIRED is final. Battery level, charging state and faults come from telemetry and act as guards on the transitions (in square brackets). Every boat that arrives at a dock plugged in passes through CHARGING, which becomes AVAILABLE as soon as the battery is at or above 30 %. INACTIVE means the operator has taken a boat in good condition out of service; the states ending in _REQUIRED wait for a Field Technician.

Code comments cite transitions by number ("lifecycle transition 15").

| # | From → To | Trigger [guard] | Rules |
|---|---|---|---|
| 1 | ● (initial) → INACTIVE | Boat registered by the operator | — |
| 2 | INACTIVE → CHARGING | Activated by the operator [moored in a berth, plugged in] | BR19 |
| 3 | AVAILABLE → INACTIVE | Deactivated by the operator | BR19 |
| 4 | CHARGING → INACTIVE | Deactivated by the operator | BR19 |
| 5 | AVAILABLE → RESERVED | Reservation | BR1, BR2, BR3, BR5, BR6, BR8 |
| 6 | RESERVED → AVAILABLE | Reservation cancelled or expired | BR8, BR9 |
| 7 | RESERVED → IN_USE | Unlocked by the user who reserved it | BR4, BR8, BR13 |
| 8 | AVAILABLE → IN_USE | Unlocked without a reservation | BR1–BR6, BR13 |
| 9 | IN_USE → ◇ (decision) | Trip ends at a dock with a free berth | BR11, BR12, BR14 |
| 10 | ◇ → CHARGING | [plugged in] | BR20, BR25 |
| 11 | ◇ → AVAILABLE | [not plugged in, battery ≥ 30 %] | BR20 |
| 12 | ◇ → CHARGE_REQUIRED | [not plugged in, battery < 30 %] | BR20 |
| 13 | ◇ → REPAIR_REQUIRED | [fault or reported problem] | BR20 |
| 14 | CHARGE_REQUIRED → CHARGING | Plugged in by the Field Technician | BR17, BR18 |
| 15 | CHARGING → AVAILABLE | [battery ≥ 30 %] | BR5, BR20 |
| 16 | AVAILABLE → REPAIR_REQUIRED | Fault reported by telemetry | BR17 |
| 17 | RESERVED → REPAIR_REQUIRED | Fault reported by telemetry; reservation cancelled with no fee | BR10 |
| 18 | CHARGING → REPAIR_REQUIRED | Fault reported by telemetry | BR17 |
| 19 | REPAIR_REQUIRED → ◇ (decision) | Repaired; ticket closed by the Field Technician | BR18, BR20 |
| 20 | IN_USE → RETURN_REQUIRED | Abandoned outside a dock (stationary for 10 min) | BR15 |
| 21 | IN_USE → TOW_REQUIRED | Battery at 0 % or immobilising fault outside a dock | BR16 |
| 22 | RETURN_REQUIRED → TOW_REQUIRED | Battery runs out while waiting | BR16 |
| 23 | RETURN_REQUIRED → ◇ (decision) | Brought back to a dock by the Field Technician | BR17, BR18, BR20 |
| 24 | TOW_REQUIRED → ◇ (decision) | Towed to a dock by the Field Technician | BR17, BR18, BR20 |
| 25 | INACTIVE → RETIRED | Retired by the operator | — |
| 26 | REPAIR_REQUIRED → RETIRED | Irreparable | — |
| 27 | TOW_REQUIRED → RETIRED | Lost or sunk | — |
| 28 | RETURN_REQUIRED → RETIRED | Lost or stolen | — |
| 29 | RETIRED → ◉ (final) | — | — |
