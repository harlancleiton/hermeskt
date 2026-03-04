---
trigger: always_on
---

# Hermeskt — Project Rules

## Project Overview

Hermes is a **notification service** (Email, SMS) built with **Kotlin + Quarkus**. It follows **DDD**, **CQRS**, **Event Sourcing**, and **Hexagonal Architecture** principles with **functional error handling** via Arrow-kt.

## Tech Stack

- **Language**: Kotlin 2.2 (JVM 21)
- **Framework**: Quarkus 3.30 (REST Jackson, SmallRye OpenAPI)
- **Build**: Maven (`./mvnw`)
- **Write Store**: Amazon DynamoDB (Enhanced Client, single-table design — EventStore)
- **Read Store**: MongoDB (Quarkus MongoDB with Panache — projections / views)
- **Messaging**: Apache Kafka (SmallRye Reactive Messaging — domain event bus)
- **Functional**: Arrow-kt (`Either`, `zipOrAccumulate`, `raise`/`either` builders)
- **DI**: Jakarta CDI (`@ApplicationScoped`, `@Inject`, `@Produces`)
- **Testing**: JUnit 5 (`quarkus-junit5`), REST-Assured, MockK, JavaFaker
- **Serialization**: Jackson

## Architecture

### Hexagonal / Ports & Adapters

- **Domain layer** (`shared/domain/`) contains entities, value objects, events, errors, factories, and repository **port interfaces**. It has **zero infrastructure dependencies**.
- **Application layer** (`shared/application/`, `core/application/`) contains CQRS command/query handlers, event handlers (projectors), and output port interfaces (e.g., `DomainEventPublisher`).
- **Infrastructure layer** (`shared/infrastructure/`) contains DynamoDB adapters (write/event store), MongoDB adapters (read/view store), and Kafka adapters (messaging).
- **Config layer** (`shared/config/`) contains Quarkus CDI producers and custom qualifiers.

### Package Structure

```
src/main/kotlin/br/com/olympus/hermes/
├── core/
│   └── application/
│       ├── commands/               # Feature-specific command handlers
│       ├── queries/                # Feature-specific query handlers
│       └── eventhandlers/          # Feature-specific event handlers (projectors)
├── shared/
│   ├── application/
│   │   ├── cqrs/                   # Command / CommandHandler / Query / QueryHandler interfaces
│   │   └── ports/                  # Output port interfaces (DomainEventPublisher, etc.)
│   ├── config/                     # CDI producers, qualifiers
│   ├── domain/
│   │   ├── entities/               # BaseEntity, AggregateRoot, Notification hierarchy
│   │   ├── events/                 # Sealed DomainEvent hierarchy
│   │   ├── exceptions/             # Sealed BaseError hierarchy (ClientError / ServerError)
│   │   ├── factories/              # NotificationFactory + Registry (Factory pattern)
│   │   ├── repositories/           # Port interfaces (NotificationRepository, EventStore)
│   │   └── valueobjects/           # Value Objects (EntityId, Email, BrazilianPhone, EmailSubject)
│   └── infrastructure/
│       ├── persistence/            # DynamoDB implementations, record models, serde (write/event store)
│       ├── readmodel/              # MongoDB implementations, view models (read store)
│       └── messaging/              # Kafka producers/consumers (SmallRye Reactive Messaging)
```

## Coding Conventions — FOLLOW STRICTLY

### Kotlin Style

- **Indentation**: 4 spaces (no tabs).
- **Final newline**: always insert a final newline at end of file.
- **Line endings**: LF.
- Write **idiomatic Kotlin**: prefer `when` expressions, data classes, sealed interfaces, extension functions, and Kotlin-specific patterns.
- Use **constructor injection** for all CDI beans (`@ApplicationScoped`, etc.). Annotate the constructor with `@Inject` when Quarkus requires it (e.g. when the class has multiple constructors or uses qualifiers).
- The **only exception** is `@Channel`-annotated `Emitter` fields (SmallRye Reactive Messaging limitation): these must remain `@Inject lateinit var`.
- Never use `@Inject lateinit var` for regular dependencies — always prefer constructor injection.

### Functional Error Handling (Arrow-kt)

- **NEVER throw exceptions** in domain or application code. Return `Either<BaseError, T>` instead.
- Use `either { }` builder blocks with `.bind()` for composing multiple `Either` calls.
- Use `zipOrAccumulate` in factories for **parallel validation** that accumulates all errors.
- Use `.mapLeft`, `.flatMap`, `.onRight`, `.onLeft` for `Either` transformations.
- All error types extend the `sealed interface BaseError` with `ClientError` or `ServerError` subtypes.
- When creating a new error type, add it to `BaseError.kt` following the existing grouped/sectioned pattern.

### Domain-Driven Design

- **Entities** extend `BaseEntity` (with `EntityId`, `createdAt`, `updatedAt`). Aggregate roots extend `AggregateRoot`.
- **Value Objects** use `@JvmInline value class` with a `private constructor` and a `companion object` factory method (`from` or `create`) returning `Either<SpecificError, ValueObject>`.
- **Domain Events** are `data class`es implementing the `sealed interface DomainEvent`. Group related events in `DomainEvent.kt`.
- **Factories** implement `NotificationFactory<T>` with `create` (from input) and `reconstitute` (from event history) methods.
- **Repositories** are port interfaces in the domain layer; implementations live in `infrastructure/persistence/`.

### Event Sourcing

- Aggregates track state changes via `applyChange(event)` which calls `apply(event)` + stores in `changes`.
- `apply(event)` uses `when` expression to pattern-match on sealed event types.
- `commit()` clears uncommitted changes; `loadFromHistory()` replays events.
- The `EventStore` is append-only with optimistic concurrency control via `expectedVersion`.
- DynamoDB key design: `PK` = aggregateId, `SK` = zero-padded version string.

### CQRS

#### Write Side (Commands)

- Commands implement marker `interface Command`.
- Handlers implement `CommandHandler<C : Command, R>` with `fun handle(command: C): Either<BaseError, R>`.
- After persisting events to DynamoDB, the handler calls `aggregate.commit()`, which triggers Kafka publishing via `DomainEventPublisher`.
- Name files as `{Action}{Entity}Command.kt` and `{Action}{Entity}Handler.kt`.

#### Read Side (Queries)

- Queries implement marker `interface Query<R>`.
- Handlers implement `QueryHandler<Q : Query<R>, R>` with `fun handle(query: Q): Either<BaseError, R>`.
- Queries read exclusively from MongoDB view collections (`*View` documents). **Never** read from DynamoDB in a query handler.
- Name files as `{Criteria}{Entity}Query.kt` and `{Criteria}{Entity}QueryHandler.kt`.

#### Event Handlers (Projectors)

- Event handlers listen to Kafka topics and project `DomainEvent`s into MongoDB read-model documents.
- Implement `EventHandler<E : DomainEvent>` with `fun handle(event: E): Either<BaseError, Unit>`.
- Each handler is responsible for creating/updating a single `*View` document type.
- Annotate Kafka consumer methods with `@Incoming("<topic>")` (SmallRye Reactive Messaging).
- Name files as `{Entity}{EventType}EventHandler.kt`, placed under `core/application/eventhandlers/`.
- Event handlers must be **idempotent** — re-processing the same event must produce the same state.

#### NotificationView (Read Model)

- `NotificationView` is a MongoDB document (`@MongoEntity`) living in `shared/infrastructure/readmodel/`.
- It is a **denormalized, flat projection** optimised for fast reads — no business logic, no domain rules.
- Fields should mirror what the API/UI needs directly; avoid joins or lazy-loaded relations.
- Use a dedicated MongoDB collection per aggregate type (e.g., `notifications`).

### Infrastructure / DynamoDB (Write / Event Store)

- Use **DynamoDB Enhanced Client** with `@DynamoDbBean` annotated record classes.
- Single-table design with `PK`/`SK` pattern.
- Use custom CDI qualifiers (e.g., `@NotificationTable`, `@EventStoreTable`) to distinguish table bindings.
- Record classes are mutable Java-bean-style (required by DynamoDB Enhanced).

### Infrastructure / MongoDB (Read Store)

- Use **Quarkus MongoDB with Panache** (`quarkus-mongodb-panache-kotlin`).
- View documents extend `PanacheMongoEntityBase` (or use `PanacheMongoRepository` pattern).
- Place view document classes under `shared/infrastructure/readmodel/`.
- Use `@MongoEntity(collection = "<name>")` to bind to the correct collection.
- Repository interfaces for the read model live in `shared/domain/repositories/` as read-model ports; implementations live in `shared/infrastructure/readmodel/`.

### Infrastructure / Kafka (Messaging)

- Use **SmallRye Reactive Messaging** (`quarkus-smallrye-reactive-messaging-kafka`).
- `DomainEventPublisher` port is implemented by a Kafka producer in `shared/infrastructure/messaging/`.
- After `aggregate.commit()` the command handler calls `DomainEventPublisher.publish(events)`; the publisher emits each event to the appropriate Kafka topic.
- Event handler consumers use `@Incoming("<topic>")` and deserialise payloads via Jackson.
- Topic naming convention: `hermes.{aggregate}.{event-type}` (kebab-case, all lowercase).

### Testing

- Use **MockK** for mocking (`mockk`, `every`, `verify`).
- Use **JavaFaker** for test data generation.
- Use **REST-Assured** (with Kotlin extensions) for integration tests.
- Test classes annotated with `@QuarkusTest` for integration tests.
- Place tests under `src/test/kotlin/` mirroring the main source structure.

### Documentation

- Add **KDoc** to all public interfaces, classes, and their public methods.
- Use `@param`, `@return` tags in KDoc.
- Keep inline comments minimal; code should be self-documenting.

## Generation Rules

1. **New notification type**: create entity in `entities/`, factory in `factories/`, created-event in `events/`, register in `NotificationFactoryRegistry.init`, add record converter in `persistence/`, add view document + projector in `readmodel/` + `eventhandlers/`.
2. **New command**: create `Command` data class + `CommandHandler` implementation in `core/application/commands/`; handler must call `aggregate.commit()` after persisting to DynamoDB.
3. **New query**: create `Query<R>` data class + `QueryHandler` implementation in `core/application/queries/`; read exclusively from MongoDB.
4. **New event handler**: create `EventHandler` in `core/application/eventhandlers/`; annotate Kafka consumer with `@Incoming`; update the corresponding `*View` document in MongoDB; ensure idempotency.
5. **New value object**: use `@JvmInline value class` with private constructor, companion factory returning `Either`, and corresponding error type in `BaseError.kt`.
6. **New repository port**: define interface in `domain/repositories/`, implement in `infrastructure/persistence/` (write) or `infrastructure/readmodel/` (read).
7. **New error type**: add as `data class` implementing `ClientError` or `ServerError` in `BaseError.kt`, in the appropriate section.
8. **Never** add infrastructure dependencies (AWS SDK, MongoDB, Kafka, Quarkus, Jakarta) to domain layer code.
9. **Always** run `./mvnw compile` to verify changes compile before considering a task done.
10. **Always** run `./mvnw ktlint:format` to format code before considering a task done.
