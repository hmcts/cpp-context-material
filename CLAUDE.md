# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

The **material** context — an HMCTS CPP digital service that stores case material (documents/files) and structured forms. It is a CQRS + event-sourced microservice built on the `uk.gov.justice` *microservices framework* (parent `uk.gov.moj.cpp.common:service-parent-pom`). Java 17, packaged as a WildFly WAR (`material-service`), with file storage in Alfresco and large-file handling in Azure Blob Storage.

## Build & test

```bash
mvn clean install                       # full multi-module build
mvn -pl material-command/material-command-handler test        # build/test one module
mvn -pl material-command/material-command-handler test -Dtest=MaterialCommandHandlerTest   # single test class
mvn -pl material-command/material-command-handler test -Dtest=MaterialCommandHandlerTest#methodName
```

Unit tests run under surefire; integration tests (`material-integration-test`) run under failsafe (`*IT.java`) and require the full Docker stack — they do **not** run from a plain `mvn test`.

### Integration tests
`./runIntegrationTests.sh` builds WARs, runs Liquibase across all stores, deploys WireMock + WARs, runs healthchecks, then the IT suite. It requires `CPP_DOCKER_DIR` exported and pointing at a local checkout of `hmcts/cpp-developers-docker`, plus the Docker/vagrant stack running. Note it runs Liquibase against **multiple** databases: event log, event-log aggregate snapshot, event buffer, viewstore, system, event tracking, job store, and file service.

### System commands
`./runSystemCommand.sh` wraps `framework-jmx-command-client` (auto-downloads the jar) to run framework JMX system commands against a running instance (e.g. `./runSystemCommand.sh CATCHUP`). Run with no args to list commands.

## Architecture — how a module maps to CQRS/event-sourcing

The `material-*` modules are the framework's standard slice layout. Data flows command → aggregate → events → event store → event listeners → view store → query.

- **material-command** — write side. `*-api` (RAML), `*-controller`, `*-handler`. Command handlers (`MaterialCommandHandler`, `StructuredFormCommandHandler`) load aggregates, apply commands, and emit events. Also contains the Alfresco upload service and Azure archive blob client.
- **material-domain** — `*-aggregate` (`CaseAggregate`, `Material`, `StructuredFormAggregate`: pure domain logic that turns commands into events and replays events), `*-event` (event POJOs), `*-transformations`.
- **material-event** — `*-listener` projects events into the view store (`MaterialEventListener`, `StructuredFormEventListener` + converters); `*-processor` handles event-driven side effects, including Alfresco upload via the framework job store.
- **material-event-sources** — `src/yaml/event-sources.yaml` declares the event-source streams (`material.event`, `public.event.source`) and their JMS/REST URIs and datasources. The build runs `messaging-adapter-generator-plugin` / `messaging-client-generator-plugin` against this.
- **material-query** — read side. `*-api` (RAML), `*-service`, `*-view` (view objects + JSON response DTOs).
- **material-viewstore** — `*-liquibase` (read-model schema; changesets under `.../material-view-store-db-changesets/`, registered in `material-view-store-db-changelog.xml`) and `*-persistence` (JPA entities/repositories).
- **material-service** — the deployable WAR; aggregates command/query/event/healthchecks plus Alfresco (`file-alfresco`) and Azure dependencies. This is what is built and deployed.
- **material-azure-functions** — standalone Azure Functions jar (`host.json` / `local.settings.json`); `LargeFilesCleaner` cleans up the large-file blob store.
- **material-client** — JAX-RS/RESTEasy client library for other contexts to call material.
- **material-datatypes-common** — shared datatypes; runs `catalog-generation-plugin`.
- **material-healthchecks**, **material-integration-test**, **material-performance-test** (JMeter; see its README).

### API definitions are RAML
Endpoints and message mappings live in `.raml` files, not annotations:
- `material-command-api.raml`, `material-query-api.raml` — HTTP APIs.
- `*.messaging.raml` (controller/handler) — message/command name → media-type mappings (e.g. `application/vnd.material.command.add-material+json`). Add or change a command/query by editing the RAML alongside the handler.

## CI / branching

- CI is Azure DevOps (`azure-pipelines.yaml`) using shared `hmcts/cpp-azure-devops-templates`: on PR → `context-verify`, on CI build → `context-validation`. SonarQube project `uk.gov.moj.cpp.material:material-parent`.
- Uses jgitflow; the develop branch is `main`. Release branches are `dev/release-*` (excluded from CI triggers). Module versions are managed in the parent `pom.xml` `<properties>` and CPP common BOMs.

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan
<!-- SPECKIT END -->
