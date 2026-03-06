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

### Modular Monolith & Hexagonal Architecture

- **Modular Monolith**: The system is organized into distinct, highly cohesive Bounded Contexts (`notification`, `template`, and `shared` kernel).
- **Domain layer** (`{context}/domain/`) contains entities, value objects, events, errors, factories, and repository **port interfaces**. It has **zero infrastructure dependencies**.
- **Application layer** (`{context}/application/`) contains CQRS command/query handlers, event handlers (projectors), and output port interfaces (e.g., `DomainEventPublisher`).
- **Infrastructure layer** (`{context}/infrastructure/`) contains DynamoDB adapters (write/event store), MongoDB adapters (read/view store), and Kafka/REST adapters (messaging/controllers).
- **Shared Kernel** (`shared/`) contains core building blocks like `BaseEntity`, `DomainEvent`, global CQRS interfaces, and common utilities that all contexts can use.

### Package Structure

```text
src/main/kotlin/br/com/olympus/hermes/
├── notification/
│   ├── application/                # Command handlers, projectors, queries for Notification
│   ├── domain/                     # Entities, events, value objects, ports for Notification
│   └── infrastructure/             # Controllers, Kafka consumers, DB adapters for Notification
├── shared/
│   ├── application/                # CQRS interfaces, generic ports
│   ├── config/                     # CDI producers, global config
│   ├── domain/                     # BaseEntity, DomainEvent interfaces, BaseErrors
│   └── infrastructure/             # Global exception mappers, shared Kafka wrappers
├── template/
│   ├── application/                # Command handlers, queries for Template
│   ├── domain/                     # Entities, value objects, domain services for Template
│   └── infrastructure/             # Controllers, DB adapters for Template
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
- In the REST infrastructure layer, use `.getOrThrowDomain()` on `Either` to unwrap the value or throw a JAX-RS `DomainException`.
- A global `DomainExceptionMapper` handles these exceptions and translates them into standardized HTTP `ErrorResponse`s.

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

#### Controllers (REST)

- Controllers should **not** contain business logic or manual error mapping (e.g., `fold` or explicit `ifLeft()`).
- Call command/query handlers and unwrap the result using `.getOrThrowDomain()`.
- Let the `DomainExceptionMapper` globally handle mapping the underlying `BaseError` to the appropriate HTTP status code (400, 404, 409, 422, 500).

#### Write Side (Commands)

- Commands implement marker `interface Command`.
- Handlers implement `CommandHandler<C : Command>` with `fun handle(command: C): Either<BaseError, Unit>`. **Commands never return data** (CQS pure).
- The caller (controller) pre-assigns the aggregate ID on the command (e.g. `id: String = UUID.randomUUID().toString()`). This allows the controller to query the read model after the command without needing a return value.
- After persisting events to DynamoDB, the handler calls `aggregate.commit()`, which triggers Kafka publishing via `DomainEventPublisher`.
- Name files as `{Action}{Entity}Command.kt` and `{Action}{Entity}Handler.kt`.
- Annotate handlers with `@WithSpan("{aggregate}.command.{action}")` and set key span attributes (`notification.id`, `notification.type`) at the top of `handle()`. Use `Log.info` on entry and on success.

#### Read Side (Queries)

- Queries implement marker `interface Query<R>`.
- Handlers implement `QueryHandler<Q : Query<R>, R>` with `fun handle(query: Q): Either<BaseError, R>`.
- Queries read exclusively from MongoDB view collections (`*View` documents). **Never** read from DynamoDB in a query handler.
- After a command, the controller performs a `GetXxxQuery` using the pre-assigned command ID to retrieve the newly created resource for the HTTP response.
- Name files as `{Criteria}{Entity}Query.kt` and `{Criteria}{Entity}QueryHandler.kt`.
- Annotate handlers with `@WithSpan("{aggregate}.query.{criteria}")` and set `{aggregate}.id` + `{aggregate}.found` attributes. Use `Log.debug` for query entry.

#### Event Handlers (Projectors)

- Projectors are **split into two layers**:
    - **Application projector** (`{context}/application/projectors/`): pure business logic. Implements `EventHandler<E : DomainEvent>` with `fun handle(event: E): Either<BaseError, Unit>`. No Kafka or infrastructure dependencies.
    - **Infrastructure consumer** (`{context}/infrastructure/messaging/consumers/`): thin `@Incoming` adapter that deserialises the Kafka message and delegates to the projector. Annotated with `@Blocking`.
- Each projector is responsible for creating/updating a single `*View` document type.
- Name projectors as `{Entity}{EventType}Projector.kt` (e.g. `NotificationCreatedProjector`).
- Name consumers as `{Entity}{EventType}Consumer.kt` (e.g. `NotificationCreatedConsumer`).
- Projectors must be **idempotent** — re-processing the same event produces the same state. Use the `event.aggregateId` as the MongoDB document ID.
- Annotate projectors with `@WithSpan("{aggregate}.projector.apply")` and set `{aggregate}.id`, `{aggregate}.type`, and `{aggregate}.view.upserted` attributes. Use `Log.info` for projection entry and success.

#### View Models (Read Model)

- View models (e.g., `NotificationView`, `TemplateView`) are MongoDB documents (`@MongoEntity`) living in `{context}/infrastructure/readmodel/`.
- They are **denormalized, flat projections** optimised for fast reads — no business logic, no domain rules.
- Fields should mirror what the API/UI needs directly; avoid joins or lazy-loaded relations.
- Use a dedicated MongoDB collection per aggregate type (e.g., `notifications`, `templates`).

### Infrastructure / DynamoDB (Write / Event Store)

- Use **DynamoDB Enhanced Client** with `@DynamoDbBean` annotated record classes.
- Single-table design with `PK`/`SK` pattern.
- Use custom CDI qualifiers (e.g., `@NotificationTable`, `@EventStoreTable`) to distinguish table bindings.
- Record classes are mutable Java-bean-style (required by DynamoDB Enhanced) and live in `{context}/infrastructure/persistence/`.

### Infrastructure / MongoDB (Read Store)

- Use **Quarkus MongoDB with Panache** (`quarkus-mongodb-panache-kotlin`).
- View documents extend `PanacheMongoEntityBase` (or use `PanacheMongoRepository` pattern).
- Place view document classes under `{context}/infrastructure/readmodel/`.
- Use `@MongoEntity(collection = "<name>")` to bind to the correct collection.
- Repository interfaces for the read model live in `{context}/domain/repositories/` as read-model ports; implementations live in `{context}/infrastructure/readmodel/`.

### Infrastructure / Kafka (Messaging)

- Use **SmallRye Reactive Messaging** (`quarkus-smallrye-reactive-messaging-kafka`).
- `DomainEventPublisher` port and `KafkaDomainEventPublisher` implementation are located in `shared/infrastructure/messaging/` for reuse across contexts.
- After `aggregate.commit()` the command handler calls `DomainEventPublisher.publish(events)`; the publisher emits each event to the appropriate Kafka topic.
- Event handler consumers use `@Incoming("<channel>")` and deserialise payloads via Jackson.
- Topic naming convention: `hermes.{aggregate}.{event-type}` (kebab-case, all lowercase).
- **Channel naming**: producer and consumer channels must have **distinct logical names** (e.g. `hermes-domain-events` for outgoing, `hermes-notification-created` for incoming) even when they map to the same Kafka topic. Using the same name for both directions causes SmallRye to wire them as an in-memory loop, bypassing the broker.
- Channel-to-topic mapping is defined exclusively in `application.properties` under `mp.messaging.outgoing.<channel>.*` and `mp.messaging.incoming.<channel>.*`.
- **Serialization**: `DomainEvent`s must **never** be serialized directly to Kafka. Use `KafkaEventWrapper`, which wraps the event as a flat `Map<String, Any?>` payload, extracting all `@JvmInline value class` properties to their primitive values. Provide a `toMap()` extension on `DomainEvent` for serialization and a `toXxxEvent()` extension on `KafkaEventWrapper.Companion` for deserialization, manually reconstructing value objects via their companion factory methods.
- **Producer** (`KafkaDomainEventPublisher`): use `emitter.send(json).toCompletableFuture().get()` to block until the broker acknowledges the record, ensuring failures are captured as `Either.Left`. Annotate the `Emitter` with `@OnOverflow(BUFFER, 256)` for explicit backpressure control.
- **Consumer** (`@Incoming` methods): always annotate with `@Blocking` when performing any blocking I/O (Jackson deserialization, MongoDB writes). Wrap deserialization in `try/catch` to skip malformed messages gracefully. Propagate `Either.Left` results from `handle()` as a `RuntimeException` so the framework performs a nack — never silently discard errors.
- **Failure strategy**: configure `mp.messaging.incoming.<channel>.failure-strategy=dead-letter-queue` for all projector consumers so that processing failures are recorded and do not halt the consumer.

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

1. **New notification/template type**: create entity in `{context}/domain/entities/`, factory in `factories/`, created-event in `events/`, register in `NotificationFactoryRegistry.init` (if applicable), add record converter in `infrastructure/persistence/`, add view document + projector in `infrastructure/readmodel/` + `application/projectors/`, add Kafka consumer in `infrastructure/messaging/consumers/`.
2. **New command**: create `Command` data class (with `id: String = UUID.randomUUID().toString()`) + `CommandHandler<C>` implementation returning `Either<BaseError, Unit>` in `{context}/application/commands/`; handler must call `aggregate.commit()` after persisting to DynamoDB. Annotate with `@WithSpan`.
3. **New query**: create `Query<R>` data class + `QueryHandler` implementation in `{context}/application/queries/`; read exclusively from MongoDB. Annotate with `@WithSpan`.
4. **New projector**: create `EventHandler` in `{context}/application/projectors/` (no Kafka deps); create thin `@Incoming` consumer in `{context}/infrastructure/messaging/consumers/` that delegates to the projector; update the corresponding `*View` document in MongoDB; use `event.aggregateId` as document ID; ensure idempotency. Annotate projector with `@WithSpan`.
5. **New value object**: use `@JvmInline value class` with private constructor, companion factory returning `Either`, and corresponding error type in `BaseError.kt`.
6. **New repository port**: define interface in `domain/repositories/`, implement in `infrastructure/persistence/` (write) or `infrastructure/readmodel/` (read).
7. **New error type**: add as `data class` implementing `ClientError` or `ServerError` in `BaseError.kt`, in the appropriate section.
8. **Never** add infrastructure dependencies (AWS SDK, MongoDB, Kafka, Quarkus, Jakarta) to domain layer code.
9. **Always** run `./mvnw compile` to verify changes compile before considering a task done.
10. **Always** run `./mvnw ktlint:format` to format code before considering a task done.
11. **Clarification**: You can use "clarification" whenever necessary in cases of doubts, divergences, or possibilities for improvement.
