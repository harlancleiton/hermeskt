# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Development
./mvnw quarkus:dev          # Start dev mode with live reload (port 8080)

# Build
./mvnw package              # JVM build → target/quarkus-app/quarkus-run.jar
./mvnw package -Pnative     # Native build (GraalVM required)

# Test
./mvnw test                 # Unit tests only
./mvnw verify -DskipITs=false  # Unit + integration tests (requires DynamoDB endpoint)

# Code quality (run before marking any task done)
./mvnw ktlint:format        # Auto-format Kotlin code
./mvnw verify               # Runs lint check + tests

# Run single test class
./mvnw test -Dtest=ClassName

# Compile check
./mvnw compile
```

After any code change, always run `./mvnw compile` then `./mvnw ktlint:format` before considering a task done.

## Architecture Overview

Hermes is a multi-channel notification service (Email, SMS, WhatsApp). It follows a Modular Monolith with DDD, CQRS, Event Sourcing, and Hexagonal Architecture.

### Bounded Contexts

- **`notification/`** — Core context: creates and tracks notifications. Has full event sourcing.
- **`template/`** — Manages notification templates (stored in MongoDB only; no event sourcing).
- **`shared/`** — Kernel: base classes, CQRS interfaces, global error types, CDI config.

### Layer Rules

- **`domain/`** — Zero infrastructure dependencies (no Quarkus, AWS SDK, MongoDB, Kafka imports).
- **`application/`** — Business logic handlers and projectors. No HTTP/Kafka infrastructure.
- **`infrastructure/`** — Adapters: REST controllers, Kafka consumers, DynamoDB/MongoDB repositories.

### Persistence Split

- **DynamoDB** (write/event store): append-only `EventStore` per aggregate. Single-table design — `PK` = aggregateId, `SK` = zero-padded version. Only the `notification` context uses event sourcing.
- **MongoDB** (read model): denormalized `*View` documents updated by projectors. All query handlers read exclusively from MongoDB, never from DynamoDB.

### Event Flow

```
REST Controller
  → CommandHandler (creates aggregate, raises events)
  → DynamoDB EventStore (persists events)
  → aggregate.commit() triggers KafkaDomainEventPublisher
  → Kafka topic
  → @Incoming Consumer (thin adapter, @Blocking)
  → Application Projector (pure business logic, updates MongoDB)
```

### Kafka Channel Naming

Producer and consumer channels must have **distinct names** even when they map to the same topic — using the same name on both sides wires an in-memory loop and bypasses the broker. Mappings live exclusively in `application.properties` under `mp.messaging.outgoing.*` / `mp.messaging.incoming.*`.

`DomainEvent`s are never serialized directly. They are wrapped in `KafkaEventWrapper` (a flat `Map<String, Any?>`) before emission.

### Error Handling

Never throw exceptions in domain or application code. Use `Either<BaseError, T>` throughout:

- `either { }` builder with `.bind()` for sequential composition
- `zipOrAccumulate` in factories for parallel field validation
- In REST controllers, unwrap with `.getOrThrowDomain()` — the global `DomainExceptionMapper` maps `BaseError` subtypes to HTTP status codes (400/404/409/422/500)
- New error types go in `shared/domain/exceptions/BaseError.kt` following the existing grouped pattern

### Value Objects

All `@JvmInline value class` wrappers use a private constructor and a `companion object` factory method (`from` or `create`) returning `Either<SpecificError, ValueObject>`.

### CQRS Conventions

- Commands: `{Action}{Entity}Command.kt` + `{Action}{Entity}Handler.kt`. Return `Either<BaseError, Unit>`. Controller pre-assigns the aggregate ID (`id: String = UUID.randomUUID().toString()`); after the command it runs a query to build the response.
- Queries: `{Criteria}{Entity}Query.kt` + `{Criteria}{Entity}QueryHandler.kt`. Read from MongoDB only.
- Projectors: split into application projector (`EventHandler<E>`) and infrastructure consumer (`@Incoming` + `@Blocking`). Must be idempotent using `event.aggregateId` as document ID.
- Annotate all handlers with `@WithSpan("aggregate.command/query.action")`.

### DI Convention

Use constructor injection for all CDI beans. The only exception is `@Channel`-annotated `Emitter` fields (SmallRye limitation) — these use `@Inject lateinit var`. Never use `lateinit var` for regular dependencies.

### DynamoDB Record Classes

Use `@DynamoDbBean` mutable Java-bean-style classes for DynamoDB Enhanced Client. They are isolated to `infrastructure/persistence/` and accessed through domain repository ports. New notification types require a new `*RecordConverter` registered in `NotificationRecordConverterRegistry`.
