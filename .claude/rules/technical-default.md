# Service Identity

- **Service:** cpp-context-material
- **Description:** Stores case material (documents/files) and structured forms. Ingests material via a two-stage command path (controller → handler), persists it as an event-sourced aggregate, projects to a read-model viewstore, and drives asynchronous side effects — Alfresco document upload, Azure Blob large-file storage, and zip/bundle generation — from the event processor via the framework job store.
- **Bounded context:** `material` (one of many CPP contexts).
- **Programme:** Crime Common Platform (CPP).
- **Organisation:** HMCTS / Ministry of Justice.

## Technology Stack

| Component         | Value                                                                |
|-------------------|----------------------------------------------------------------------|
| Build tool        | Maven (multi-module reactor; root `pom.xml`, `material-parent`)       |
| Language          | Java 17 (CI demand `centos8-j17`)                                     |
| Framework         | CPP `service-parent-pom:17.103.x` (JEE/CDI-style)                     |
| Packaging         | WAR (`material-service`) → WildFly via Docker; `material-azure-functions` as a separate Azure Functions jar |
| Annotations       | `@ServiceComponent`, `@Handles`, `@ApplicationScoped`                 |
| Persistence       | Liquibase changelogs (event-store, aggregate-snapshot, viewstore, event-buffer) |
| File storage      | Alfresco (documents) + Azure Blob Storage (large files)              |
| Messaging         | ActiveMQ (Docker for ITs); JMS topics + queues                        |
| Tests             | JUnit + Mockito (unit, surefire); framework's IT harness (`runIntegrationTests.sh`, failsafe); JMeter (`material-performance-test`) |
| CI                | Azure DevOps Pipelines (`azure-pipelines.yaml` + `hmcts/cpp-azure-devops-templates`) |
| Quality gate      | SonarQube in CI (project `uk.gov.moj.cpp.material:material-parent`)   |
| Java packaging    | Root namespace `uk.gov.moj.cpp.material.*`                            |

## Constraints

- Maven is the current build tool. Future migration to Gradle is allowed but requires coordinating constitution + rule files + CI pipeline together (see Constitution Principle V).
- Java 17 only — prefer explicit types in public APIs
- Use the CPP framework's `@ServiceComponent` + `@Handles` for command/event handling — NOT hand-rolled JMS listeners
- Aggregate state mutation must go through the aggregate's `apply(event)` replay (`CaseAggregate` / `Material` / `StructuredFormAggregate`)
- Event listeners and processors must use converter classes in `converter/` packages — NOT inline mapping in the listener/processor body
- Long-running side effects (Alfresco / Azure / zip-bundle) go via the framework job store — never blocking inline in a processor
- Contracts (RAML, JSON schemas, `subscriptions-descriptor.yaml`, `event-sources.yaml`) update FIRST, Java second (Constitution Principle I)
- Schema additions / removals / renames update both `subscriptions-descriptor.yaml` AND JSON schema in lockstep, with the correct `schema_uri` namespace (Constitution Principle VI)
- Logging via SLF4J only — no `System.out` / `System.err` (Constitution Principle VII)
- Test-Driven Development is mandatory (Constitution Principle VIII)

## Build & Test Commands

```bash
# Full build + unit tests
mvn clean install

# Build, no tests
mvn clean install -DskipTests

# Unit tests only
mvn test

# Single module with deps
mvn -pl material-command/material-command-handler -am clean install

# Single unit test
mvn -pl material-command/material-command-handler test -Dtest=MaterialCommandHandlerTest#methodName

# Integration tests (requires Dockerised env up; CPP_DOCKER_DIR must be set)
./runIntegrationTests.sh

# Single IT against running env
mvn -pl material-integration-test test -Dit.test=ClassNameIT

# Framework JMX commands
./runSystemCommand.sh           # help / list
./runSystemCommand.sh CATCHUP   # run one
```

## Key version pins (`pom.xml`)

- Parent: `uk.gov.moj.cpp.common:service-parent-pom:17.103.x` (currently 17.103.3); artifact `material-parent` (currently `17.0.84-SNAPSHOT`), groupId `uk.gov.moj.cpp.material`
- Cross-context / notable pins to keep aligned: `coredomain`, `usersgroups`, `stream-transformation-tool`, `stream-transformation-tool-anonymise`, `mireportdata.cc.dependency`, `material-azure`, `material-azure-storage`
- When bumping any of these, also check the matching schema/RAML classifier dep is on the same version
