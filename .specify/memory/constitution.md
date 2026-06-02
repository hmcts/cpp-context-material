<!--
SYNC IMPACT REPORT
==================
Version change: (uninitialised template) → 1.0.0
Bump rationale: Initial ratification. All principles and sections are new; no
                prior principles to remove or redefine, so MAJOR is the correct
                starting point (1.0.0).

Modified principles: N/A (initial ratification).

Added sections:
  - Core Principles
      I.    RAML / JSON-Schema Contract First
      II.   CQRS Three-Layer Discipline (Command / Listener / Processor)
      III.  CPP Framework Idioms — No Manual Rolling
      IV.   Spec-Driven Build Loop
      V.    HMCTS CPP Standards Compliance
      VI.   Schema-Subscription Symmetry
      VII.  No System.out / System.err — SLF4J Only
      VIII. Test-Driven Development
  - Technology Stack & Deployment
  - Development Workflow & Quality Gates
  - Governance

Removed sections: None.

Templates requiring updates:
  - .specify/templates/plan-template.md       ✅ compatible — the "Constitution
      Check" block is filled per-feature by `/speckit-plan`. Plan authors MUST
      gate on Principles I–VIII.
  - .specify/templates/spec-template.md       ✅ compatible.
  - .specify/templates/tasks-template.md      ✅ compatible — task ordering
      already encodes "tests before implementation", aligning with VIII.
  - .specify/templates/checklist-template.md  ✅ compatible.
  - README.md / CLAUDE.md / docs/*            ✅ aligned — `.claude/rules/*.md`
      encodes these principles informally; this constitution is now the
      authoritative source.

Follow-up TODOs: None. All placeholders resolved.
-->

# cpp-context-material Constitution

## Core Principles

### I. RAML / JSON-Schema Contract First (NON-NEGOTIABLE)

The contracts of this service — commands it accepts, queries it answers, and
domain events it emits — are defined in **RAML files and JSON schemas under
`*/src/raml/...` and `*/src/main/resources/.../json/schema/...` directories**.
Those artefacts are the source of truth. Java handler signatures, listener
mappings, and processor mappings MUST follow the contracts; the contracts MUST
NOT be inferred from the Java code.

For every command/event change you MUST update:

1. The RAML file — `material-command-api.raml` (HTTP API),
   `material-command-controller.messaging.raml` and
   `material-command-handler.messaging.raml` (media-type → command mappings,
   e.g. `application/vnd.material.command.add-material+json`),
   `material-query-api.raml` (read API), and
   `subscriptions-descriptor.yaml` for listener / processor subscriptions.
2. The matching JSON schema referenced by the contract's `schema_uri`
   (under `src/raml/json/schema/` or `src/main/resources/.../json/schema/`).
3. The `event-sources.yaml` if a new internal/public topic is involved.
4. Then — and only then — the Java handler / listener / processor.

**Rationale**: the CPP framework dispatches commands and events by matching the
RAML/media-type contract against handler annotations and the
`subscriptions-descriptor.yaml` `schema_uri`. A drift between the contract and
the Java code produces a runtime 500 (no matching schema) or, worse, silent
message-loss with no logging. The contracts are also consumed by callers via
`material-client`; treating them as documentation rather than source-of-truth
produces cross-context incidents.

### II. CQRS Three-Layer Discipline (NON-NEGOTIABLE)

Every change touching events MUST be reasoned about across **all three
layers**:

```
Command side (controller → handler → aggregate → domain event)
    ↓ writes events to DS.eventstore
Event listener (projects events → viewstore tables)
    ↓ projects to DS.material
Event processor (consumes domain events → side effects + public events)
    ↓ Alfresco upload / Azure Blob / zip+bundle (via the job store), public.event
```

Material's intake is two-stage on the command side: REST/messaging lands on the
`material.controller.command` queue (controller), which forwards to the
`material.handler.command` queue (handler → `CaseAggregate` / `Material` /
`StructuredFormAggregate`). Adding or modifying a domain event WITHOUT updating
both the listener and the processor is a Principle II violation. Plan authors
MUST list which of the three layers a change touches and confirm the other two
are either unaffected (with reasoning) or carry a paired change in the same PR.

**Rationale**: the read-model in `DS.material` and the processor's async side
effects (Alfresco document upload, Azure large-file blobs, bundle/zip
generation) depend on the listener and processor staying in lockstep with the
command side. Breaking one without the others produces silent data drift — the
aggregate is correct, the read-model lies, and the document store diverges.

### III. CPP Framework Idioms — No Manual Rolling (NON-NEGOTIABLE)

This service is built on `uk.gov.moj.cpp.common:service-parent-pom`. Use the
framework's idioms rather than rolling your own:

- Command handlers: `@ServiceComponent(COMMAND_HANDLER)` / `COMMAND_CONTROLLER`
  + `@Handles(...)` on a method taking `Envelope<...>` / `JsonEnvelope`.
- Aggregate state: route mutations through the aggregate's `apply(...)` event
  replay (`CaseAggregate`, `StructuredFormAggregate`); never mutate read-model
  state from the command side.
- Event listeners: extend the framework's listener bases; map events → JPA
  entities via dedicated converter classes.
- Event processors: extend the framework's processor bases; offload long-running
  work (Alfresco upload, Azure blob, zip/bundle) to the **framework job store**,
  not inline blocking calls.
- File storage: use the `file-alfresco` / file-service wiring for documents and
  the Azure blob client for large files — do not hand-roll storage clients.
- Persistence: Liquibase changelogs only — never manual DDL.
- Outbound calls / client: use the framework REST client wiring;
  `material-client` is the generated JAX-RS/RESTEasy client other contexts use.

**Forbidden**: hand-rolled JMS listeners, hand-rolled JDBC, ad-hoc ObjectMapper
instances, manual schema validation, blocking I/O inside an event processor.
The framework already solves these and rolling your own diverges from the rest
of the CPP estate.

**Rationale**: every CPP service follows these idioms, so cross-service
maintenance and operability depend on consistency. A bespoke pattern in one
service makes the next maintainer reach for the wrong mental model.

### IV. Spec-Driven Build Loop (NON-NEGOTIABLE)

Every non-trivial change MUST flow through the cycle:

```
Spec → Write → Code Review → QA → Spec-Validate → Fix → Ship
```

The reviewer agents (`code-reviewer`, `qa`, `spec-validator`) report findings
only; they MUST NOT modify code. The primary agent or a human applies fixes,
then re-runs the loop until all three return PASS / COMPLIANT. The
`spec-validator` here checks that RAML and JSON-schema files are consistent with
both `subscriptions-descriptor.yaml` files, `event-sources.yaml`, and the Java
handler / listener / processor / converter mappings. Changes exempt from the
loop: markdown-only edits, whitespace or import-only edits, `.claude/rules/*`
and `CLAUDE.md` rule updates.

**Rationale**: keeps a human (or primary agent) as the decision point; prevents
conflicting auto-fixes; preserves auditable, reproducible review output.

### V. HMCTS CPP Standards Compliance (NON-NEGOTIABLE)

- **Build tool**: Maven (current). Module layout, version management, and CI all
  assume the Maven reactor; a future migration to Gradle is allowed but is
  itself a constitution-amendment-scale change and MUST update this section, the
  rule files, the agent docs, and the CI pipeline in lockstep.
- **Java**: 17.
- **Parent**: `uk.gov.moj.cpp.common:service-parent-pom:17.103.x` — pin updates
  require a coordinated cross-context check against the upstream pins managed in
  the root `pom.xml` `<properties>` (`coredomain`,
  `stream-transformation-tool`, and the CPP common BOMs).
- **Packaging**: WAR deployed to WildFly via Docker. The `material-service`
  module is the packaging WAR; `src/main/descriptors/resource-descriptor.yml`
  wires datasources / queues / topics / service mapping (matcher
  `/material-[^/]+`). `material-azure-functions` is a separate Azure Functions
  artefact (large-file cleanup), not part of the WAR.
- **Tests**: JUnit + Mockito for unit tests (surefire); integration tests in
  `material-integration-test` run under failsafe (`*IT.java`) orchestrated by
  `runIntegrationTests.sh` (Docker-based WildFly + Postgres + ActiveMQ +
  WireMock). ITs require `CPP_DOCKER_DIR` pointing at a local checkout of
  `hmcts/cpp-developers-docker`.
- **CI/CD**: Azure DevOps (`azure-pipelines.yaml`) using shared
  `hmcts/cpp-azure-devops-templates`: PR builds run `context-verify`; CI builds
  run `context-validation`. SonarQube project
  `uk.gov.moj.cpp.material:material-parent`. `main` is the develop branch;
  `dev/release-*` branches are excluded from CI triggers (jgitflow).
- **Quality gate**: SonarQube — coverage, duplication, smells. No local
  Checkstyle / PMD enforcement at build time.

**Rationale**: aligns this service with the rest of the CPP estate (naming,
build, deploy, test, observability conventions) so cross-team maintenance,
on-call rotation, and platform upgrades work uniformly.

### VI. Schema-Subscription Symmetry (NON-NEGOTIABLE)

When you add, remove, or rename a domain or public event you MUST update
**both**:

- The relevant `subscriptions-descriptor.yaml`
  (`material-event/material-event-listener/src/yaml/...` and
  `material-event/material-event-processor/src/yaml/...`).
- The matching JSON schema referenced by the `schema_uri`.

Two further material-specific traps a reviewer MUST check:

- **Two schema-URI namespaces are in use** — some events resolve under
  `http://cpp.moj.gov.uk/material/json/schemas/event/...` and others under
  `http://justice.gov.uk/cps/material/events/...`. A new event MUST use the
  same namespace its sibling schema file is actually published under; a
  copy-pasted `schema_uri` pointing at the wrong namespace fails dispatch.
- The **listener and processor subscribe to overlapping but not identical**
  event sets (the processor also handles failure/side-effect events such as
  `file-uploaded-as-pdf`, `material-bundle-requested`,
  `material-bundling-failed`). Confirm which component(s) actually consume a
  new event before wiring it.

A subscription without a matching schema produces a runtime 500 on dispatch. A
schema without a subscription is dead code that drifts silently as the event
evolves.

**Rationale**: this is the most common source of incidents on this service.
Encoding it as a NON-NEGOTIABLE principle (rather than a "common gotcha" in
CLAUDE.md) makes it a review-blocker.

### VII. No `System.out` / `System.err` — SLF4J Only (NON-NEGOTIABLE)

Code MUST NOT use `System.out.println`, `System.err.println`, or
`Throwable#printStackTrace()`. All diagnostic output goes through SLF4J
(`org.slf4j.Logger` via `LoggerFactory.getLogger(...)`). This applies to
production code AND tests.

**Rationale**: container logs are aggregated and structured; stdout prints
bypass the framework's MDC (correlation id propagation through the `Envelope`
metadata) and the platform log shipping. They vanish from operations and surface
as noise in CI.

### VIII. Test-Driven Development (NON-NEGOTIABLE)

Red → Green → Refactor for every behaviour change.

1. Write the failing test first. It MUST run and fail for the *correct* reason —
   the assertion, not a missing class or compilation error.
2. Write the minimum production code to make it pass.
3. Refactor with the test still green.

PRs MUST show that the test was authored at or before the production code
(commit history or paired-commit are both acceptable). The `qa` reviewer agent
gates on this — production code without an accompanying failing-then-passing
test is FAIL.

Exempt: pure mechanical refactors (rename, move, extract with no behaviour
change), formatting, comment-only edits.

**Rationale**: the regression surface of this service is wide — two aggregates
(`CaseAggregate`/`Material` and `StructuredFormAggregate`), file upload across
Alfresco and Azure blob, bundle/zip generation, dozens of converter classes,
and asynchronous processor side effects. Only fail-first tests catch the class
of bug where a converter silently drops a field or a processor uploads to the
wrong store.

## Technology Stack & Deployment

- **Java**: 17.
- **Build**: Maven. Multi-module reactor; modules listed in root `pom.xml`
  (`material-parent`, groupId `uk.gov.moj.cpp.material`).
- **Framework**: CPP `service-parent-pom:17.103.x`. JEE-style with
  `@ServiceComponent` / `@Handles` annotations.
- **Packaging**: WAR (`material-service`) → WildFly (Docker);
  `material-azure-functions` as a separate Azure Functions jar.
- **Persistence**: Liquibase changelogs (event store, aggregate snapshot,
  event buffer, viewstore). Read-model schema lives under
  `material-viewstore/material-viewstore-liquibase`
  (`material-view-store-db-changelog.xml`).
- **Messaging**: ActiveMQ (Docker for ITs); JMS topics + queues declared in
  `event-sources.yaml` and `resource-descriptor.yml`.
- **File storage**: Alfresco (`file-alfresco` / file-service) for documents;
  Azure Blob Storage for large files (cleaned up by
  `material-azure-functions`).
- **Data stores**:
  - `java:/app/material-service/DS.eventstore` — event store
    (event-repository-liquibase + aggregate-snapshot-repository-liquibase).
  - `java:/DS.material` — viewstore (event-buffer-liquibase +
    `material-viewstore-liquibase`).
- **JMS resources**:
  - `material.controller.command` — controller command queue (stage 1).
  - `material.handler.command` — handler command queue (stage 2).
  - `material.event` — internal event topic.
  - `public.event` — shared platform topic (cross-context traffic).
- **Tests**:
  - Unit: JUnit + Mockito (`mvn test`, surefire).
  - Integration: `runIntegrationTests.sh` orchestrates Docker WildFly +
    Postgres + ActiveMQ + WireMock; runs Liquibase across all stores, deploys
    WARs, executes `material-integration-test/*IT.java` (failsafe). A separate
    `material-performance-test` module runs JMeter.
- **Logging**: SLF4J + the framework's logger configuration; MDC keys carried
  through `Envelope` metadata.
- **CI/CD**: Azure DevOps via `azure-pipelines.yaml` + shared
  `hmcts/cpp-azure-devops-templates`. PR = `context-verify`. CI build =
  `context-validation`. SonarQube project
  `uk.gov.moj.cpp.material:material-parent`. `dev/release-*` branches excluded.
- **Quality gate**: SonarQube — coverage thresholds, duplication, smells
  enforced in CI; no local equivalent at build time.

## Development Workflow & Quality Gates

- **Contract files** (RAML, JSON schemas, both `subscriptions-descriptor.yaml`,
  `event-sources.yaml`) MUST be updated **before** the matching Java change
  (Principle I + VI).
- The build loop (Principle IV) repeats until `code-reviewer`, `qa`, and
  `spec-validator` each return PASS / COMPLIANT.
- TDD (Principle VIII) MUST be visible in commit history — the failing test
  commit precedes (or is paired with) the production code that satisfies it.
- Every feature built via spec-kit lives under `specs/<JIRA-ID>-slug/`
  (or `specs/NNN-slug/` if not Jira-tracked) containing at least `spec.md`,
  `plan.md`, and `tasks.md`. Flow:
  `/speckit-specify → /speckit-plan → /speckit-tasks → /speckit-implement
  → /speckit-analyze`.
- Required commands run cleanly before merge:
  - `mvn clean install` — full build + unit tests, green.
  - `./runIntegrationTests.sh` — Dockerised IT run, green (when changes touch
    handlers / listeners / processors / converters / schemas / file storage).
  - SonarQube quality gate in CI — passing.
- Commit style: Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`,
  `refactor:`).
- Pull requests: the description MUST state which principle(s) the change
  touches. Any deviation from a principle requires explicit written
  justification in the PR description and MUST be flagged in the plan's
  "Complexity Tracking" section.
- Branch naming: Jira-prefixed (`DD-XXXXX-feature-slug`) — the speckit
  `before_specify` hook auto-creates these via `/speckit-git-feature`.

## Governance

This constitution supersedes the informal conventions in `.claude/rules/`.
Where this document and those files disagree, this document wins; the rule files
are retained as quick-reference material and MUST be kept in sync.

**Amendment procedure**:

1. Propose the change in a feature spec under `specs/`.
2. Bump `Version` per semantic versioning:
   - **MAJOR** — a breaking principle change, removal, or redefinition that
     invalidates existing practice.
   - **MINOR** — a new principle, new section, or materially expanded guidance.
   - **PATCH** — clarifications, wording, typo fixes, or non-semantic
     refinements.
3. Re-run `/speckit-analyze` on every in-flight feature spec to verify it still
   aligns with the amended principles; update or waive as required.

**Compliance expectations**:

- All PRs MUST honour these principles.
- Deviations MUST be explicitly justified in the PR description and, where
  relevant, in the plan's "Complexity Tracking" table.
- Reviewers MUST block merges that silently violate a NON-NEGOTIABLE principle
  without a written waiver.

**Version**: 1.0.0 | **Ratified**: 2026-06-02 | **Last Amended**: 2026-06-02
