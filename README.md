# Hermes

> A multi-channel notification service built with Kotlin and Quarkus, following Domain-Driven Design, CQRS, and Event Sourcing principles.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin)](https://kotlinlang.org/)
[![Quarkus](https://img.shields.io/badge/Quarkus-3.30-4695EB?logo=quarkus)](https://quarkus.io/)
[![Java](https://img.shields.io/badge/JVM-21-ED8B00?logo=openjdk)](https://openjdk.org/)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

Hermes is a backend notification service that handles the creation, persistence, and delivery tracking of notifications across multiple channels — Email, SMS, and WhatsApp. It exposes a REST API and uses Amazon DynamoDB as its primary data store with a full event-sourcing model.

## Features

- **Multi-channel notifications** — Email, SMS, and WhatsApp (Push planned)
- **Event Sourcing** — Full audit trail of aggregate state changes via an append-only DynamoDB event store
- **CQRS** — Commands and queries are strictly separated; handlers are thin and composable
- **Functional error handling** — No exceptions thrown in domain or application code; all errors flow through `Either<BaseError, T>` via Arrow-kt
- **Hexagonal Architecture** — Domain layer has zero infrastructure dependencies; ports and adapters keep I/O at the edges
- **OpenAPI** — Swagger UI available out of the box at `/swagger-ui`

## Architecture

Hermes follows a **Modular Monolith** architecture built on **Domain-Driven Design (DDD)** principles. The system is organized into distinct, highly cohesive **Bounded Contexts** (`notification`, `template`, and `shared`), each encapsulating its own **Hexagonal Architecture** layers.

### System Design

- **Notification Context** — Manages creation, persistence, and delivery tracking of notifications.
- **Template Context** — Handles message templates and rendering logic.
- **Shared Kernel** — Contains generic building blocks (BaseEntity, DomainEvent, CQRS interfaces) used across all contexts.

### Key design decisions

| Concern | Approach |
|---|---|
| Architecture | Modular Monolith with Hexagonal Layers |
| Error handling | `Either<BaseError, T>` with Arrow-kt; `zipOrAccumulate` for parallel validation |
| Persistence | Amazon DynamoDB (Write/Event Store) + MongoDB (Read/View Store) |
| Messaging | Apache Kafka for internal domain event propagation |
| CQRS | Strict separation of commands and queries via dedicated handlers |

### Package structure

```
src/main/kotlin/br/com/olympus/hermes/
├── notification/               # Notification Bounded Context
│   ├── application/            # Command/Query handlers, Projectors
│   ├── domain/                 # Entities, Events, Repository Ports
│   └── infrastructure/         # Controllers, Kafka Consumers, DB Adapters
├── template/                   # Template Bounded Context
│   ├── application/            # Template rendering and management
│   ├── domain/                 # Template entities and ports
│   └── infrastructure/         # MongoDB adapters and REST endpoints
└── shared/                     # Shared Kernel
    ├── domain/                 # BaseEntity, DomainEvent, BaseError
    ├── application/            # CQRS interfaces, Global ports
    └── infrastructure/         # Global config, Exception mappers
```

## Prerequisites

- JDK 21+
- Maven 3.9+ (or use the included `./mvnw` wrapper)
- An AWS account or a local DynamoDB emulator (e.g., [LocalStack](https://localstack.cloud/))
- AWS credentials configured in your environment

## Getting started

### 1. Configure DynamoDB

Set the required properties in `src/main/resources/application.properties` or via environment variables:

```properties
dynamodb.table-name=hermes-notifications
dynamodb.event-store-table-name=hermes-event-store
quarkus.dynamodb.endpoint-override=http://localhost:4566   # LocalStack
quarkus.dynamodb.aws.region=us-east-1
quarkus.dynamodb.aws.credentials.type=static
quarkus.dynamodb.aws.credentials.static-provider.access-key-id=test-key
quarkus.dynamodb.aws.credentials.static-provider.secret-access-key=test-secret
```

> [!NOTE]
> Both DynamoDB tables must exist before starting the service. Hermes does not create them automatically. Use your preferred IaC tool (CDK, Terraform, AWS CLI) to provision them.

### 2. Run in development mode

```bash
./mvnw quarkus:dev
```

The service starts on port `8080` with live reload enabled. The Swagger UI is available at:

```
http://localhost:8080/swagger-ui
```

### 3. Run tests

```bash
./mvnw test
```

Run integration tests (requires a running DynamoDB endpoint):

```bash
./mvnw verify -DskipITs=false
```

## Building

### JVM build

```bash
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

### Native build (GraalVM required)

```bash
./mvnw package -Pnative
./target/hermeskt-1.0.0-SNAPSHOT-runner
```

## Notification channels

| Channel | Status | Input type |
|---|---|---|
| Email | Available | `CreateNotificationCommand.Email` |
| SMS | Available | `CreateNotificationCommand.Sms` |
| WhatsApp | Available | `CreateNotificationCommand.WhatsApp` |
| Push | Planned | — |

## Error handling

All domain and application errors extend the sealed `BaseError` interface:

- `ClientError` — validation failures, invalid inputs (maps to 4xx)
- `ServerError` — infrastructure/persistence failures (maps to 5xx)

Errors are composed without throwing exceptions using Arrow-kt's `either { }` builder and `zipOrAccumulate` for parallel field validation.

## Code quality

Format code with ktlint before committing:

```bash
./mvnw ktlint:format
```

Lint check runs automatically during `verify`:

```bash
./mvnw verify
```

## Tech stack

| | |
|---|---|
| Language | Kotlin 2.2 / JVM 21 |
| Framework | Quarkus 3.30 |
| Build | Maven (Quarkus Maven Plugin) |
| Database | Amazon DynamoDB (Enhanced Client) |
| Functional | Arrow-kt 2.2 |
| Serialization | Jackson |
| DI | Jakarta CDI (Quarkus Arc) |
| Testing | JUnit 5, MockK, REST-Assured, JavaFaker |
