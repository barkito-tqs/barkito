# Barkito: user story split

Who implements each user story of the backlog (`docs/barkito-backlog.md`). Each story is assigned to its owner in Jira; the key in brackets is the Jira story (BRK-<n>). Stories are grouped by module, so each member owns one area of the code and the stories that depend on each other stay with the same person. Every story still needs a review by another member before it is merged (QA Manual 4.1).

The backlog has 39 stories and 100 points; each member gets between 22 and 28.

## Split by member

| Member | Area | Stories | Points |
|---|---|---|---|
| Rodrigo Simões | Finding and reserving boats; user dashboard; points and promotions | US-09 (BRK-46), US-10 (BRK-50), US-11 (BRK-51), US-29 (BRK-71), US-33 (BRK-75), US-34 (BRK-76), US-35 (BRK-77), US-36 (BRK-78) | 22 |
| Simão Pinto | Accounts and roles; telemetry; trip end and its reactions to telemetry; technical metrics; GBFS | US-01 (BRK-47), US-02 (BRK-41), US-07 (BRK-45), US-08 (BRK-49), US-13 (BRK-53), US-14 (BRK-63), US-15 (BRK-64), US-32 (BRK-74), US-37 (BRK-79), US-38 (BRK-61) | 26 |
| Gonçalo Vila | Payments; unlocking a boat; client backoffice; business KPIs | US-04 (BRK-43), US-12 (BRK-52), US-16 (BRK-54), US-17 (BRK-55), US-18 (BRK-56), US-19 (BRK-57), US-20 (BRK-65), US-26 (BRK-68), US-27 (BRK-69), US-28 (BRK-70), US-31 (BRK-73) | 28 |
| Claudino Martins | Fleet lifecycle; maintenance tickets; operator dashboard | US-03 (BRK-42), US-05 (BRK-44), US-06 (BRK-48), US-21 (BRK-58), US-22 (BRK-59), US-23 (BRK-60), US-24 (BRK-66), US-25 (BRK-67), US-30 (BRK-72), US-39 (BRK-62) | 24 |

## Split by iteration

| Iteration | Rodrigo | Simão | Gonçalo | Claudino |
|---|---|---|---|---|
| I2 | US-09 | US-02, US-07 | US-04 | US-03, US-05 |
| I3 | US-10, US-11 | US-01, US-08, US-13, US-38 | US-12, US-16, US-17, US-18, US-19 | US-06, US-21, US-22, US-23, US-39 |
| I4 | US-29 | US-14, US-15, US-32 | US-20, US-26, US-27, US-28, US-31 | US-24, US-25, US-30 |
| I5 (extensions) | US-33, US-34, US-35, US-36 | US-37 | — | — |

## Dependencies between members

- US-09 and US-10 (Rodrigo) need boats that can be activated (US-05, Claudino) and telemetry (US-07, Simão).
- US-12 (Gonçalo) unlocks the boats reserved in US-10 and US-11 (Rodrigo).
- US-13 (Simão) settles the charges (US-17, US-18, Gonçalo).
- US-21 (Claudino) is opened by the state changes of US-13, US-14, US-15 and US-38 (Simão).
- US-35 and US-36 (Rodrigo) apply discounts to the trip price (US-18, Gonçalo).

These pairs agree on the interface (port or REST endpoint) at the start of the iteration, so neither has to wait for the other to finish.

## Rules

- One branch per story, `feature/BRK-<n>-<slug>`, created from `develop` (QA Manual 4.1).
- The owner writes the code, the tests and the Gherkin scenarios of the story, and links them in Xray.
- If a member is overloaded, a story moves to another member by agreement in the stand-up, and Jira is updated.
