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
- **Database**: Amazon DynamoDB (Enhanced Client, single-table design)
- **Functional**: Arrow-kt (`Either`, `zipOrAccumulate`, `raise`/`either` builders)
- **DI**: Jakarta CDI (`@ApplicationScoped`, `@Inject`, `@Produces`)
- **Testing**: JUnit 5 (`quarkus-junit5`), REST-Assured, MockK, JavaFaker
- **Serialization**: Jackson

## Architecture

### Hexagonal / Ports & Adapters

- **Domain layer** (`shared/domain/`) contains entities, value objects, events, errors, factories, and repository **port interfaces**. It has **zero infrastructure dependencies**.
- **Application layer** (`shared/application/`, `core/application/`) contains CQRS command/query handlers and output port interfaces (e.g., `DomainEventPublisher`).
- **Infrastructure layer** (`shared/infrastructure/`) contains DynamoDB adapters implementing domain ports.
- **Config layer** (`shared/config/`) contains Quarkus CDI producers and custom qualifiers.

### Package Structure

```
src/main/kotlin/br/com/olympus/hermes/
├── core/
│   └── application/commands/       # Feature-specific CQRS handlers
├── shared/
│   ├── application/
│   │   ├── cqrs/                   # Command / CommandHandler interfaces
│   │   └── ports/                  # Output port interfaces
│   ├── config/                     # CDI producers, qualifiers
│   ├── domain/
│   │   ├── entities/               # BaseEntity, AggregateRoot, Notification hierarchy
│   │   ├── events/                 # Sealed DomainEvent hierarchy
│   │   ├── exceptions/             # Sealed BaseError hierarchy (ClientError / ServerError)
│   │   ├── factories/              # NotificationFactory + Registry (Factory pattern)
│   │   ├── repositories/           # Port interfaces (NotificationRepository, EventStore)
│   │   └── valueobjects/           # Value Objects (EntityId, Email, BrazilianPhone, EmailSubject)
│   └── infrastructure/
│       └── persistence/            # DynamoDB implementations, record models, serde
```

## Coding Conventions — FOLLOW STRICTLY

### Kotlin Style

- **Indentation**: 4 spaces (no tabs).
- **Final newline**: always insert a final newline at end of file.
- **Line endings**: LF.
- Write **idiomatic Kotlin**: prefer `when` expressions, data classes, sealed interfaces, extension functions, and Kotlin-specific patterns.
- Use `lateinit var` with `@Inject` for Quarkus CDI field injection.
- Constructor injection is used in non-CDI classes (e.g., handlers, factories).

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

- Commands implement marker `interface Command`.
- Handlers implement `CommandHandler<C : Command, R>` with `fun handle(command: C): Either<BaseError, R>`.
- Name files as `{Action}{Entity}Command.kt` and `{Action}{Entity}Handler.kt`.

### Infrastructure / DynamoDB

- Use **DynamoDB Enhanced Client** with `@DynamoDbBean` annotated record classes.
- Single-table design with `PK`/`SK` pattern.
- Use custom CDI qualifiers (e.g., `@NotificationTable`, `@EventStoreTable`) to distinguish table bindings.
- Record classes are mutable Java-bean-style (required by DynamoDB Enhanced).

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

1. **New notification type**: create entity in `entities/`, factory in `factories/`, created-event in `events/`, register in `NotificationFactoryRegistry.init`, add record converter in `persistence/`.
2. **New command**: create `Command` data class + `CommandHandler` implementation in `core/application/commands/`.
3. **New value object**: use `@JvmInline value class` with private constructor, companion factory returning `Either`, and corresponding error type in `BaseError.kt`.
4. **New repository port**: define interface in `domain/repositories/`, implement in `infrastructure/persistence/`.
5. **New error type**: add as `data class` implementing `ClientError` or `ServerError` in `BaseError.kt`, in the appropriate section.
6. **Never** add infrastructure dependencies (AWS SDK, Quarkus, Jakarta) to domain layer code.
7. **Always** run `./mvnw compile` to verify changes compile before considering a task done.
8. **Always** run `./mvnw ktlint:format` to format code before considering a task done.
