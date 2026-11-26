# Development workflow and quality gates

Source of truth for QA Manual sections **3.3 Static analysis and quality gates** and **4.1 Development workflow**. Practices referenced: PM3, T1–T5, Q1–Q4, D1, D2, D4, AI3.

Stack: Java 21, Spring Boot (stable), Maven, GitHub Actions, SonarQube, JaCoCo, Testcontainers.

---

## 4.1 Development workflow

### Branching model

| Branch | Purpose | Rules |
|--------|---------|-------|
| `main` | Stable, deployable releases | Protected. Updated only by a PR from `develop` at the end of an iteration, and only if stable. |
| `develop` | Integration branch of the current iteration | Protected. Updated only by PRs from feature/fix/docs branches. |
| `feature/BRK-<n>-<slug>` | One branch per user story | Created from `develop`. Short-lived. |
| `fix/BRK-<n>-<slug>` | Bug fixes | Created from `develop`. Short-lived. |
| `docs/BRK-<n>-<slug>` | Changes to documentation only | Created from `develop`. Short-lived. |

Example: `feature/BRK-12-dock-availability`.

- `BRK-<n>` is the Jira key of the story (PM3). It is **mandatory** in the branch name and in the PR title; CI validates both.
- It is recommended, but not mandatory, in commit messages on the branch (e.g. `BRK-12 add dock availability endpoint`), because squash merge discards them.

### Merge policy

A pull request is merged only when **all** of the following hold:

1. At least one **human** member other than the author has approved it (Q4). An approval by an AI agent does not count (AI3).
2. CI is green, including the quality gate (see 3.3).
3. There are no merge conflicts.

Merge types:

- feature/fix/docs → `develop`: **squash merge**; the branch is deleted afterwards. The squash commit message is the PR title, so it carries the Jira key.
- `develop` → `main`: **merge commit** (no squash), so that `develop` does not diverge from `main` after each release.

Nobody pushes directly to `main` or `develop`, administrators included (D1).

Because `main` is updated once per iteration, continuous delivery (D4) deploys once per iteration (weekly).

### Definition of Done (per story)

Always:

- [ ] Acceptance criteria are met.
- [ ] Unit tests cover the new business logic.
- [ ] Coverage on new code is at least 80%.
- [ ] CI and the quality gate are green.
- [ ] The PR is approved by another member.
- [ ] The story is linked to its tests in Xray (PM5).
- [ ] Documentation and API docs are updated, if applicable.

Depending on the story:

- Touches API or persistence → integration tests (Testcontainers), run separately from unit tests (T3).
- Core story → acceptance criteria as Gherkin scenarios automated with Cucumber (T1); end-to-end UI tests only for the main use cases (T4).
- Load tests (k6, T5) run in the pipeline on critical flows and are **not** part of the per-story DoD.

### Review process

Reviewers use the checklist in [`CONTRIBUTING.md`](../../CONTRIBUTING.md). AI-assisted review findings are recorded in the PR as accepted or rejected (AI3).

### Calling the API locally

The `dev` profile is the default, so a backend started with `cd backend && ./mvnw spring-boot:run` accepts the dev tokens. `./mvnw` needs JDK 21.

The endpoints require a JWT. The repository root has `dev-tokens.env`, with one ready-made token per role (`TOKEN_END_USER`, `TOKEN_OPERATOR`, `TOKEN_TECHNICIAN`, `TOKEN_CLIENT_MANAGER`, `TOKEN_ADMIN`) and `TOKEN_ALL`, which has every role:

```bash
source dev-tokens.env
curl -H "Authorization: Bearer $TOKEN_OPERATOR" http://localhost:8080/api/v1/boats
```

- In Swagger UI (`/swagger-ui.html`), click *Authorize* and paste the token value, without `Bearer`.
- Use a one-role token to check that a request is refused with 403 for the other roles. Use `TOKEN_ALL` only to explore, because it passes every role check.
- The tokens are signed with the dev secret in `application-dev.properties`, so they are not secrets and are committed on purpose. They expire one year after generation.
- Deployed environments must never use the `dev` profile: the deploy must set `SPRING_PROFILES_ACTIVE` explicitly (in `backend/Dockerfile` / `docker-compose.yml`), because nothing else enforces it. With any other profile these tokens are worthless.

---

## 3.3 Static analysis and quality gates

### Tooling

- **SonarQube** for static analysis and dashboard (Q1).
- **JaCoCo** for code coverage, reported to Sonar (Q1).
- **OWASP Dependency-Check** (Maven plugin) on every build, plus **Dependabot** update PRs (Q3).

### Quality gate on new code (Q2)

The gate blocks the merge when it fails.

| Metric (new code) | Threshold | Justification |
|-------------------|-----------|---------------|
| Coverage | ≥ 80% | Default of the Sonar way quality gate. |
| Duplicated lines | ≤ 3% | Default of the Sonar way quality gate. |
| Reliability rating | A (no new bugs) | Default of the Sonar way quality gate. |
| Security rating | A (no new vulnerabilities) | Default of the Sonar way quality gate. |
| Security Hotspots reviewed | 100% | Default of the Sonar way quality gate. |

The values are industry defaults, not derived from Barkito data. They are **reviewed after iteration 2**, once real measurements exist.

### Dependency vulnerabilities (Q3)

| Tool | Role |
|------|------|
| OWASP Dependency-Check | Runs in CI on every build. Fails the build for any vulnerability with CVSS ≥ 9.0 (Critical band of the CVSS standard). Needs an NVD API key stored as a GitHub secret. |
| Dependabot | Opens update PRs. Helper only; not a gate. |

The CVSS threshold is also reviewed after iteration 2 (High, 7.0, is the stricter alternative).

### Continuous integration (D2)

GitHub Actions runs on every push and every pull request targeting `develop` or `main`:

1. Build (Maven, Java 21).
2. Unit tests.
3. Integration tests (run separately from unit tests).
4. JaCoCo coverage report.
5. Sonar analysis and quality gate.
6. Dependency-Check.

The author is notified of failures through GitHub's notifications, and the README shows a build status badge.

### Repository settings to apply (DevOps)

- Protect `main` and `develop`: require PR, 1 approval, status checks (CI + quality gate), no direct push, including administrators.
- Allow squash merge (feature → `develop`) and merge commit (`develop` → `main`).
- Delete head branches after merge.
