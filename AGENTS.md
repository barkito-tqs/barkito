# Agent instructions

Instructions for AI agents working in this repository. Must stay consistent with the QA Manual (`docs/`).

## Stack

Java 21, Spring Boot (stable), Maven.

## Build commands

```bash
cd backend && ./mvnw spotless:apply verify        # format, build, tests, coverage
cd frontend && npm run format && npm run lint && npm test
```

## Reference docs

- Before implementing a story: read it in Jira (project `BRK`) by the key in the branch name, and look up every BRx it cites in [`docs/business-rules.md`](docs/business-rules.md).
- [`docs/CONTEXT.md`](docs/CONTEXT.md): domain glossary. Read it before naming a class, field, endpoint, test or UI label.
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md): modules, layers and dependency rules. Read it before adding an endpoint, entity, module or screen.

## Code style

Follow the Google Java Style Guide and avoid magic numbers. Details and the review checklist are in [`CONTRIBUTING.md`](CONTRIBUTING.md).

## Workflow rules

- Branch from `develop` as `feature/BRK-<n>-<slug>` or `fix/BRK-<n>-<slug>`. The PR title starts with the Jira key.
- Never push directly to `main` or `develop`. All changes go through a pull request.
- A human must approve every PR. Agents cannot approve or merge.

## Definition of Done

See [`docs/qa/development-workflow.md`](docs/qa/development-workflow.md). In short: acceptance criteria met, unit tests for new business logic, coverage on new code ≥ 80%, green CI and quality gate, approved PR, story linked in Xray.

## Test rules

- Unit tests for business logic.
- Integration tests (Testcontainers) for API and persistence, run separately from unit tests.
- Gherkin/Cucumber for core stories; end-to-end UI tests only for the main use cases.
- Do not weaken or delete tests to make the build pass.
