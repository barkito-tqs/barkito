# Contributing to Barkito

Short contributor guidelines (Q5). The full workflow, Definition of Done and quality gate are in [`docs/qa/development-workflow.md`](docs/qa/development-workflow.md).

## Workflow in short

1. Pick a story from the Jira iteration board (project `BRK`).
2. Branch from `develop`: `feature/BRK-<n>-<slug>` (or `fix/BRK-<n>-<slug>`).
3. Commit often. Starting commit messages with the story key is recommended (e.g. `BRK-12 add dock availability endpoint`).
4. Open a PR to `develop`. The title **must** start with the story key (`BRK-12 <title>`). Fill in the PR template.
5. Wait for green CI and the quality gate, and for approval by another member.
6. Squash merge. The branch is deleted.

Build and test locally before pushing:

```bash
mvn verify
```

## Code style

- Java 21, Spring Boot. Follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).
- No magic numbers: use named constants or configuration, and explain non-obvious values.
- No secrets in the repository. Use environment variables or GitHub secrets.
- Names describe intent; prefer small methods with a single responsibility.
- Business logic has unit tests; API and persistence have integration tests (Testcontainers), kept separate from unit tests.

## Review checklist

The reviewer confirms that:

- [ ] The acceptance criteria of the story are met.
- [ ] Tests cover the new behavior.
- [ ] There are no secrets and no unexplained magic numbers.
- [ ] Names are clear and the code is readable.
- [ ] CI and the quality gate passed.
- [ ] AI review findings, if any, are recorded in the PR as accepted or rejected, with a reason.

The author cannot approve their own PR. An approval by an AI agent does not replace the human approval.

## Further references

- Definition of Done, branching and merge policy: [`docs/qa/development-workflow.md`](docs/qa/development-workflow.md)
- Instructions for AI agents: [`AGENTS.md`](AGENTS.md)
