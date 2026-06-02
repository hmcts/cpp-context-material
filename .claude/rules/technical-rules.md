# Coding Conventions — MOJ / CPP Standard (this service)

## Dependency Injection / Component Wiring

- Use the CPP framework's component model — `@ApplicationScoped` for framework-managed singletons, `@Inject` (CDI) for collaborator injection where the framework expects it
- For command controllers: `@ServiceComponent(COMMAND_CONTROLLER)` on the class + `@Handles("<command-name>")` on the method (intake stage 1, `material.controller.command`)
- For command handlers: `@ServiceComponent(COMMAND_HANDLER)` on the class + `@Handles("<command-name>")` on the method (intake stage 2, `material.handler.command`)
- For event listeners: framework listener base + `@Handles("<event-name>")` on listener methods, `@ServiceComponent(EVENT_LISTENER)` on the class
- For event processors: framework processor base + `@ServiceComponent(EVENT_PROCESSOR)` on the class
- Do NOT use Spring annotations (`@Autowired`, `@Component`, `@Service`, `@RequiredArgsConstructor`) — this service does not use Spring
- Do NOT roll your own JMS listener / JDBC connection / ObjectMapper / Alfresco / Azure storage client

## Envelope / Payload Handling

- Handler/listener method signatures take `Envelope<PayloadType>` / `JsonEnvelope`, never the raw payload type
- Read the payload via `envelope.payload()`; metadata via `envelope.metadata()`
- Correlation context (correlation id, user id, etc.) lives in `envelope.metadata()` and should be propagated into MDC for SLF4J
- Treat the payload as immutable — do not mutate fields after reading

## Aggregate State Mutation

- All aggregate state mutation goes through the aggregate's `apply(event)` event-replay mechanism (`CaseAggregate`, `Material`, `StructuredFormAggregate` under `material-domain/material-domain-aggregate`)
- The handler asks the aggregate to perform a command; the aggregate emits domain events; state is rebuilt by replaying those events via `apply(...)`
- Do NOT write events directly to the event store, and do NOT mutate read-model state from the command side

## Long-Running Work (Alfresco / Azure / Bundles)

- File upload to Alfresco, large-file upload to Azure Blob, and zip/bundle generation are **side effects driven by the event processor**
- Offload them to the framework **job store** — do NOT perform blocking I/O inline in a processor method
- Use the framework's `file-alfresco` / file-service wiring and the Azure blob client (`command.handler.alfresco`, `command.handler.azure.service` packages) — never hand-rolled clients
- `material-azure-functions` (`LargeFilesCleaner`) cleans up the large-file blob store; keep its expectations in sync when changing the large-file path

## Converters (Listener and Processor)

- Listener converters live under `material-event-listener/.../event/listener/converter/` and map domain events → JPA viewstore entities
- Processor converters live under the processor module and map domain events → public-event payloads (or job inputs)
- Each converter is a single-purpose class (one event → one target shape); composition happens at the listener/processor level

## Error Handling

- Custom exceptions extend `RuntimeException` (or framework-specific bases like `EventStreamException`)
- NEVER swallow exceptions silently — always log or rethrow
- Listener / processor methods can let framework exceptions propagate; the framework handles redelivery and dead-letter routing
- Invalid envelope payloads should fail loudly with a meaningful message — the framework re-delivers, so a silent skip leaks broken state
- Alfresco / Azure failures should emit the appropriate failure event (e.g. `failed-to-add-material`, `material-bundling-failed`) rather than being swallowed

## Logging

- SLF4J with the framework's logger configuration
- Use `private static final Logger LOGGER = LoggerFactory.getLogger(...)` (or framework-specific equivalent if the codebase has one)
- MDC keys: include correlation id and other relevant fields from `envelope.metadata()`
- NEVER use `System.out.println`, `System.err.println`, or `Throwable#printStackTrace()` (Constitution Principle VII)
- NEVER log sensitive data (case/defendant identifiers in plain text without masking, file content, tokens, passwords, PII)

## Imports

- NEVER use wildcard imports (`import java.util.*`) — always use explicit imports for each class

## Naming Conventions

| Component        | Pattern                  | Example                                  |
|------------------|--------------------------|------------------------------------------|
| Command handler  | `*Handler`               | `MaterialCommandHandler`, `StructuredFormCommandHandler` |
| Command controller | `*Controller`          | (under `command.controller`)             |
| Event listener   | `*Listener`              | `MaterialEventListener`, `StructuredFormEventListener` |
| Event processor  | `*Processor`             | (under the processor module)             |
| Converter        | `*Converter` or `*To*Converter` | (under `converter/`)              |
| Aggregate        | (singular noun)          | `CaseAggregate`, `StructuredFormAggregate`, `Material` |
| Domain event     | (past tense)             | `MaterialAdded`, `FileUploaded`, `StructuredFormCreated` |
| Service / view   | `*Service`               | (Alfresco upload service, query services) |
| Test             | `*Test` / `*IT`          | `MaterialCommandHandlerTest`, `*IT`      |

## Testing Conventions

- JUnit + Mockito for unit tests (surefire)
- Use `@ExtendWith(MockitoExtension.class)` (or the codebase's preferred Mockito wiring — check existing tests)
- Use `@Nested` classes with `@DisplayName` for grouped scenarios
- Method naming: `{action}_{scenario}_should_{expectation}`
- Use AssertJ (`assertThat(...)`) where the codebase already does; otherwise plain JUnit assertions are fine
- Integration tests live in `material-integration-test` (failsafe, `*IT.java`) and run via `./runIntegrationTests.sh`
- Test commands:
  - `mvn test` — unit tests only
  - `./runIntegrationTests.sh` — full Dockerised IT run
  - `mvn -pl material-integration-test test -Dit.test=ClassNameIT` — single IT against running env
- TDD: write the failing test first, see it fail for the right reason, then implement (Constitution Principle VIII)
- Logging in tests: SLF4J only (Constitution Principle VII)

## RAML / JSON Schema

- RAML files: `src/raml/...` per module that owns commands or queries
- JSON schemas: referenced by `schema_uri` — under `src/raml/json/schema/` or `src/main/resources/.../json/schema/`
- Material uses TWO schema namespaces — `http://cpp.moj.gov.uk/material/json/schemas/event/...` and `http://justice.gov.uk/cps/material/events/...`. Match the namespace the sibling schema is published under
- Every command in RAML has a matching `@Handles` method; every event in `subscriptions-descriptor.yaml` has a matching listener / processor method
- Every event has a JSON schema; every JSON schema is referenced from at least one contract artefact
- When adding a new event:
  1. JSON schema first (correct namespace)
  2. `subscriptions-descriptor.yaml` entry (listener and/or processor)
  3. `event-sources.yaml` if a new topic is involved
  4. Java listener / processor method last
