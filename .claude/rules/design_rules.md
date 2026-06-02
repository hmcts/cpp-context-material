# Architecture & Domain Rules

## Three Layers (CQRS / Event-Sourced)

```
1. Command side (controller → handler → aggregate → domain event)
       ↓ writes to event store (java:/app/material-service/DS.eventstore)

2. Event listener (projects events → viewstore tables)
       ↓ projects to java:/DS.material

3. Event processor (consumes domain events → side effects + public events)
       ↓ Alfresco upload / Azure Blob / zip+bundle (via job store); public.event for other contexts
```

Every change touching events MUST be reasoned about across **all three layers**. Breaking one without the others produces silent data drift.

- **Command side** — RAML-declared commands arrive on `material.controller.command` (controller, stage 1), forward to `material.handler.command` (handler, stage 2). `@Handles`-annotated handler classes (`MaterialCommandHandler`, `StructuredFormCommandHandler`) ask the aggregate to perform the command; the aggregate (`CaseAggregate` / `Material` / `StructuredFormAggregate`) emits domain events. State is rebuilt by replaying events via `apply(...)`.
- **Event listener** — projects domain events into the viewstore DB (`DS.material`). Lives under `material-event/material-event-listener`. Heavy use of converters mapping events → JPA entities.
- **Event processor** — consumes domain events to drive side effects (Alfresco document upload, Azure large-file blob, zip/bundle generation — offloaded to the framework job store) and to emit **public** events. Lives under `material-event/material-event-processor`. Heavy use of converters.

## Domain Concepts

| Concept                 | Description                                                                                                                              |
|-------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| Material / Case material| Documents/files attached to a case. Modelled by the `CaseAggregate` / `Material` aggregate.                                              |
| Structured form         | Structured form data, modelled by `StructuredFormAggregate` (create / update / update-for-defendant / publish / finalise).               |
| Domain event            | Internal event written to the event store. Examples: `material-added`, `file-uploaded`, `file-uploaded-as-pdf`, `material-deleted`, `material-bundle-details-recorded`, `structured-form-created/updated/published/finalised`, `cloud-blob-file-uploaded`. |
| Public event            | Cross-context event emitted on `public.event`, with its own JSON schema.                                                                 |
| Command                 | Inbound request via `material.controller.command` then `material.handler.command`. Declared in RAML, dispatched by `@Handles`. Examples: add-material, upload-file, upload-file-as-pdf, delete-material, create-material-bundle, record-bundle-details, zip-material, create/update/publish/finalise-structured-form. |
| Listener                | Read-side projection — `*Listener` class extending the framework's listener base; projects events → viewstore JPA entities via converters. |
| Processor               | Side-effect + public-event emitter — `*Processor` class extending the framework's processor base; offloads Alfresco/Azure/zip work to the job store and maps events → public-event payloads. |
| Viewstore               | Read-model database `DS.material`, populated by listeners. Schema managed by `material-viewstore-liquibase` (`material-view-store-db-changelog.xml`).        |
| Event store             | Append-only log `DS.eventstore`. Source of truth for aggregate state. Schema managed by `event-repository-liquibase`.                     |

## Authoritative Routing Files (always re-read before reasoning about a flow)

- `material-event-sources/src/yaml/event-sources.yaml` — event-source streams (`material`, `public.event.source`) + JMS/REST URIs and datasources.
- `material-event/material-event-listener/src/yaml/subscriptions-descriptor.yaml` — listener subscriptions.
- `material-event/material-event-processor/src/yaml/subscriptions-descriptor.yaml` — processor subscriptions.
- `material-command/material-command-controller/src/raml/material-command-controller.messaging.raml` — controller (stage 1) command → media-type mapping.
- `material-command/material-command-handler/src/raml/material-command-handler.messaging.raml` — handler (stage 2) command → media-type mapping.
- `material-command/material-command-api/src/raml/material-command-api.raml` and `material-query/material-query-api/src/raml/material-query-api.raml` — HTTP APIs.
- `material-service/src/main/descriptors/resource-descriptor.yml` — datasources, JMS queues/topics, service mapping (`/material-[^/]+`).
- Per-command/per-event JSON schemas referenced by `schema_uri` (two namespaces — see Gotchas).

## Module Layout

- `material-datatypes-common` — shared datatypes/value objects (runs `catalog-generation-plugin`)
- `material-command/material-command-api` — RAML + schemas + access control + command-side rules
- `material-command/material-command-controller` — controller (stage 1 intake)
- `material-command/material-command-handler` — `@Handles` handlers; Alfresco upload service + Azure archive blob client
- `material-domain/material-domain-aggregate` — `CaseAggregate`, `Material`, `StructuredFormAggregate`
- `material-domain/material-domain-event` — event POJOs / event schemas
- `material-domain/material-domain-transformations` — transformations (incl. anonymisation via stream-transformation-tool)
- `material-event/material-event-listener` — listeners + converters → viewstore
- `material-event/material-event-processor` — processors + converters → side effects / public events
- `material-event-sources` — `event-sources.yaml`
- `material-query/material-query-api` — RAML for query side
- `material-query/material-query-service`, `material-query/material-query-view` — read services + response DTOs over the viewstore
- `material-viewstore/material-viewstore-liquibase` — read-model schema; `material-viewstore/material-viewstore-persistence` — JPA entities/repositories
- `material-service` — packaging WAR; `resource-descriptor.yml` wires datasources / queues / topics
- `material-client` — JAX-RS/RESTEasy client library for other contexts to call material
- `material-azure-functions` — Azure Functions jar (`LargeFilesCleaner` cleans the large-file blob store)
- `material-healthchecks`, `material-integration-test` (`*IT.java` via `runIntegrationTests.sh`), `material-performance-test` (JMeter)

## Adding a New Command

1. **RAML first.** Add the command operation to the controller and handler messaging RAML (`*.messaging.raml`) with the right media type (e.g. `application/vnd.material.command.<name>+json`).
2. **JSON schema.** Add the command payload schema referenced by the RAML, in the correct namespace.
3. **Controller + Handler.** Add `@Handles("<command-name>")` methods on the `COMMAND_CONTROLLER` and `COMMAND_HANDLER` classes. Methods take `Envelope<CommandPayload>` / `JsonEnvelope`.
4. **Aggregate.** If the command mutates state, the handler asks the aggregate to perform it; the aggregate emits a domain event and rebuilds state via `apply(event)`.
5. **Listener.** If the new event is consumed by the listener: subscription entry + JSON schema + listener method + converter.
6. **Processor.** If the new event triggers a side effect (Alfresco/Azure/zip via job store) or a public event: subscription entry + JSON schema + processor method + converter (+ public-event JSON schema).
7. **Tests.** Failing unit tests for controller/handler, aggregate, listener (if touched), processor (if touched), converters (if touched). Then production code. Then IT exercising the end-to-end flow.

## Adding a New Domain Event

- Add the event's JSON schema under the owning module's `json/schema/` path, using the correct `schema_uri` namespace.
- Update the listener AND/OR processor `subscriptions-descriptor.yaml` with the new event entry (the two components subscribe to overlapping but not identical sets — wire it to the component(s) that actually consume it; document any that are unaffected).
- Update `event-sources.yaml` if a new internal topic is introduced.
- Add the listener/processor method + converter, and the failing-then-passing tests.

## Adding a Public-Event Subscription (incoming from another context)

1. **Subscription entry.** Add to listener and/or processor `subscriptions-descriptor.yaml` for the `public.event.source`.
2. **JSON schema.** Add the public-event schema (matches the upstream context's contract version).
3. **Listener / processor method.** With `@Handles("<public-event-name>")` and `Envelope<PayloadType>`.
4. **Converter.** Map the public-event payload → either a viewstore entity (listener) or a domain command (if it triggers a state change).
5. **Tests.** Unit tests for the listener/processor + converter. IT simulating the public-event arrival.

## Out-of-Scope (do not add)

- Hand-rolled JMS listeners — use the framework's `@Handles`
- Hand-rolled JDBC — use Liquibase changelogs and JPA repositories
- Hand-rolled Alfresco / Azure storage clients — use the framework's `file-alfresco` / file-service wiring and the Azure blob client
- Blocking long-running I/O inside an event processor — offload to the framework job store
- Ad-hoc `ObjectMapper` instances — use the framework's configured mapper
- Manual JSON schema validation — the framework validates incoming envelopes against subscription-declared schemas
- Spring annotations (`@Autowired`, `@Component`, `@Service`, `@RequiredArgsConstructor`) — this service does not use Spring
- Cross-context coupling beyond declared public events / the `material-client` contract

## Common Gotchas

1. **Schema-subscription drift** — adding a `subscriptions-descriptor.yaml` entry without the matching JSON schema produces a runtime 500 on dispatch. Constitution Principle VI makes this a review-blocker.
2. **Wrong `schema_uri` namespace** — material publishes schemas under BOTH `http://cpp.moj.gov.uk/material/json/schemas/event/...` and `http://justice.gov.uk/cps/material/events/...`. A copy-pasted `schema_uri` in the wrong namespace fails dispatch.
3. **Three-layer drift** — modifying a domain event without updating the listener AND processor (where each consumes it) is the most common silent-data-drift bug. Constitution Principle II makes this a review-blocker.
4. **Liquibase registration** — adding a changelog file without registering it in the right registry (event-store / aggregate-snapshot / viewstore / event-buffer) means it never applies in CI's IT setup.
5. **Blocking the processor** — doing Alfresco/Azure/zip work inline in a processor method instead of via the job store blocks event throughput and risks redelivery storms.
6. **Wrong `@ServiceComponent` value** — `COMMAND_CONTROLLER` vs `COMMAND_HANDLER` vs `EVENT_LISTENER` vs `EVENT_PROCESSOR` are NOT interchangeable; the framework dispatches based on the value.
7. **Cross-context pin drift** — bumping `coredomain` / `usersgroups` / `stream-transformation-tool(-anonymise)` / `material-azure(-storage)` versions in `pom.xml` requires bumping the matching schema/RAML classifier dep to the same version.
