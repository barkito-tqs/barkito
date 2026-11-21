# Barkito: user story split

Who implements each user story of the backlog (`docs/barkito-backlog.md`). Stories are grouped by module, so each member owns one area of the code and the stories that depend on each other stay with the same person. Every story still needs a review by another member before it is merged (QA Manual 4.1).

The backlog has 39 stories and 100 points; each member gets about 25.

## Split by member

| Member | Area | Stories | Points |
|---|---|---|---|
| Rodrigo Simões | Finding and reserving boats; user dashboard; points and promotions | US-09, US-10, US-11, US-12, US-29, US-33, US-34, US-35, US-36 | 25 |
| Simão Pinto | Accounts and roles; telemetry; trip end and its reactions to telemetry; technical metrics; GBFS | US-01, US-02, US-07, US-08, US-13, US-14, US-15, US-32, US-37, US-38 | 26 |
| Gonçalo Vila | Payments; client backoffice; business KPIs | US-04, US-16, US-17, US-18, US-19, US-20, US-26, US-27, US-28, US-31 | 25 |
| Claudino Martins | Fleet lifecycle; maintenance tickets; operator dashboard | US-03, US-05, US-06, US-21, US-22, US-23, US-24, US-25, US-30, US-39 | 24 |

## Split by iteration

| Iteration | Rodrigo | Simão | Gonçalo | Claudino |
|---|---|---|---|---|
| I2 | US-09 | US-02, US-07 | US-04 | US-03, US-05 |
| I3 | US-10, US-11, US-12 | US-01, US-08, US-13, US-38 | US-16, US-17, US-18, US-19 | US-06, US-21, US-22, US-23, US-39 |
| I4 | US-29 | US-14, US-15, US-32 | US-20, US-26, US-27, US-28, US-31 | US-24, US-25, US-30 |
| I5 (extensions) | US-33, US-34, US-35, US-36 | US-37 | — | — |

## Dependencies between members

- US-09 and US-10 (Rodrigo) need boats that can be activated (US-05, Claudino) and telemetry (US-07, Simão).
- US-12 (Rodrigo) needs the deposit (US-17, Gonçalo).
- US-13 (Simão) settles the charges (US-17, US-18, Gonçalo).
- US-21 (Claudino) is opened by the state changes of US-13, US-14, US-15 and US-38 (Simão).
- US-35 and US-36 (Rodrigo) apply discounts to the trip price (US-18, Gonçalo).

These pairs agree on the interface (port or REST endpoint) at the start of the iteration, so neither has to wait for the other to finish.

## Rules

- One branch per story, `feature/BRK-<n>-<slug>`, created from `develop` (QA Manual 4.1).
- The owner writes the code, the tests and the Gherkin scenarios of the story, and links them in Xray.
- If a member is overloaded, a story moves to another member by agreement in the stand-up, and Jira is updated.
