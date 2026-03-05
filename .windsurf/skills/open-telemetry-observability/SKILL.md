---
name: open-telemetry-observability
description: You are an expert in enhancing observability in Quarkus applications using OpenTelemetry
---

# OpenTelemetry Observability Expert - Quarkus/Kotlin

You are an expert in enhancing observability in Quarkus applications using OpenTelemetry.
Your goal is to identify where manual instrumentation adds value beyond Quarkus's automatic
instrumentation, add meaningful logs, and create custom spans when appropriate.

## Context: Quarkus Auto-Instrumentation

Quarkus automatically instruments:

- REST endpoints (JAX-RS, RESTEasy, Quarkus REST)
- REST clients
- gRPC
- Database calls (JDBC, Reactive SQL, MongoDB)
- Messaging (Kafka, AMQP, RabbitMQ, Pulsar)
- Redis, GraphQL, WebSockets
- Scheduler jobs

**Your job**: Add observability where automatic instrumentation doesn't reach - business logic,
complex operations, async processing, and critical decision points.

## When to Add Custom Spans

### ✅ Add spans for:

- **Business operations** not covered by auto-instrumentation
- **Multi-step workflows** (e.g., order processing, payment flows)
- **Background processing** and scheduled tasks internals
- **Complex calculations** or data transformations
- **Cache operations** (when not auto-instrumented)
- **File I/O operations**
- **Third-party library calls** without auto-instrumentation

### ❌ Don't add spans for:

- REST endpoints (already traced)
- Database queries (already traced)
- HTTP client calls (already traced)
- Simple getters/setters/mappers
- Operations already wrapped by framework instrumentation

## When to Add Logs

- **Business decisions**: "Order approved", "Payment gateway selected", "Discount applied"
- **Critical data points**: amounts, user IDs, transaction IDs
- **Error conditions**: validation failures, business rule violations
- **State changes**: "Order status changed from PENDING to CONFIRMED"

## Kotlin/Quarkus Patterns

### 1. Using @WithSpan Annotation (Simplest)

```kotlin
import io.opentelemetry.instrumentation.annotations.WithSpan
import io.opentelemetry.instrumentation.annotations.SpanAttribute
import io.opentelemetry.api.trace.Span
import io.quarkus.logging.Log

@ApplicationScoped
class OrderService {

    @WithSpan("order.process")
    fun processOrder(
        @SpanAttribute("order.id") orderId: String,
        @SpanAttribute("user.id") userId: String
    ): OrderResult {
        Log.info("Processing order order_id=$orderId user_id=$userId")

        val order = validateOrder(orderId)
        val result = applyBusinessRules(order)

        // Add runtime attributes
        Span.current().apply {
            setAttribute("order.amount", result.amount)
            setAttribute("order.items_count", result.items.size.toLong())
        }

        Log.info("Order processed successfully order_id=$orderId status=${result.status}")
        return result
    }

    @WithSpan("order.validate")
    fun validateOrder(@SpanAttribute("order.id") orderId: String): Order {
        Log.debug("Validating order order_id=$orderId")
        // validation logic
        return order
    }
}
```

````

### 2. Manual Span for Complex Control Flow

```kotlin
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.api.trace.StatusCode
import jakarta.inject.Inject

@ApplicationScoped
class PaymentProcessor {

    @Inject
    lateinit var tracer: Tracer

    fun executePayment(payment: Payment): PaymentResult {
        val span = tracer.spanBuilder("payment.execute")
            .setAttribute("payment.amount", payment.amount)
            .setAttribute("payment.method", payment.method)
            .startSpan()

        return try {
            span.makeCurrent().use {
                Log.info("Executing payment amount=${payment.amount} method=${payment.method}")

                val gateway = selectGateway(payment)
                span.setAttribute("payment.gateway", gateway.name)

                val result = gateway.process(payment)

                span.setAttribute("payment.transaction_id", result.transactionId)
                span.setAttribute("payment.success", result.success)

                if (!result.success) {
                    span.setStatus(StatusCode.ERROR, "Payment failed")
                    Log.error("Payment failed reason=${result.failureReason}")
                } else {
                    Log.info("Payment successful transaction_id=${result.transactionId}")
                }

                result
            }
        } catch (e: Exception) {
            span.setStatus(StatusCode.ERROR, e.message ?: "Unknown error")
            span.recordException(e)
            Log.error("Payment processing error", e)
            throw e
        } finally {
            span.end()
        }
    }
}
```

### 3. Suspend Functions (Coroutines)

```kotlin
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.withContext

@ApplicationScoped
class ReportGenerator {

    @Inject
    lateinit var tracer: Tracer

    suspend fun generateReport(userId: String): Report {
        val span = tracer.spanBuilder("report.generate")
            .setAttribute("user.id", userId)
            .startSpan()

        return try {
            withContext(span.asContextElement()) {
                Log.info("Generating report user_id=$userId")

                val data = fetchData(userId)
                val processed = processData(data)

                span.setAttribute("report.records", processed.size.toLong())
                Log.info("Report generated user_id=$userId records=${processed.size}")

                Report(processed)
            }
        } catch (e: Exception) {
            span.recordException(e)
            span.setStatus(StatusCode.ERROR)
            Log.error("Report generation failed user_id=$userId", e)
            throw e
        } finally {
            span.end()
        }
    }
}
```

### 4. Adding Attributes to Auto-Instrumented Spans

```kotlin
import io.opentelemetry.api.trace.Span

@Path("/orders")
class OrderResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    fun createOrder(request: CreateOrderRequest): Response {
        // Enhance auto-instrumented HTTP span with business context
        Span.current().apply {
            setAttribute("order.type", request.type)
            setAttribute("order.priority", request.priority)
            setAttribute("customer.tier", request.customerTier)
        }

        Log.info("Creating order type=${request.type} priority=${request.priority}")

        val order = orderService.create(request)

        Span.current().setAttribute("order.id", order.id)
        Log.info("Order created successfully order_id=${order.id}")

        return Response.ok(order).build()
    }
}
```

### 5. Batch Processing with Nested Spans

```kotlin
@ApplicationScoped
class BatchProcessor {

    @Inject
    lateinit var tracer: Tracer

    @WithSpan("batch.process")
    fun processBatch(@SpanAttribute("batch.id") batchId: String, items: List<Item>) {
        Log.info("Processing batch batch_id=$batchId items_count=${items.size}")

        Span.current().setAttribute("batch.size", items.size.toLong())

        var successCount = 0
        var failureCount = 0

        items.forEach { item ->
            val itemSpan = tracer.spanBuilder("batch.process_item")
                .setAttribute("item.id", item.id)
                .startSpan()

            try {
                itemSpan.makeCurrent().use {
                    processItem(item)
                    successCount++
                }
            } catch (e: Exception) {
                itemSpan.recordException(e)
                itemSpan.setStatus(StatusCode.ERROR)
                failureCount++
                Log.warn("Item processing failed item_id=${item.id}", e)
            } finally {
                itemSpan.end()
            }
        }

        Span.current().apply {
            setAttribute("batch.success_count", successCount.toLong())
            setAttribute("batch.failure_count", failureCount.toLong())
        }

        Log.info("Batch completed batch_id=$batchId success=$successCount failures=$failureCount")
    }
}
```

### 6. JDBC Telemetry Activation

```properties
# Enable JDBC instrumentation for datasource
quarkus.datasource.jdbc.telemetry=true
```

### 7. Structured Logging with Quarkus Log

```kotlin
import io.quarkus.logging.Log

// Quarkus Log automatically includes trace_id and span_id when configured
Log.info("Order validated order_id=$orderId total_amount=$amount")
Log.error("Payment failed transaction_id=$txId reason=$reason")
Log.warn("Inventory low product_id=$productId remaining=$count")
```

## Hermes Project — Specific Patterns

This section captures the conventions established in the Hermes notification service
(Kotlin + Quarkus + CQRS + Event Sourcing + Arrow-kt).

### Auto-Instrumented in Hermes (DO NOT add spans)

| Layer | Auto-Instrumented By |
|---|---|
| JAX-RS controllers (`/notifications`, `/templates`) | Quarkus OTel |
| MongoDB reads/writes (`NotificationViewRepository`) | Quarkus OTel |
| DynamoDB reads/writes (`NotificationRepository`, `EventStore`) | Quarkus OTel |
| Kafka producer (`KafkaDomainEventPublisher`) | Quarkus OTel |
| Kafka consumer (`@Incoming` methods in `*Consumer`) | Quarkus OTel |

### Manual Instrumentation Required (ADD spans here)

| Layer | Why |
|---|---|
| `CommandHandler.handle()` | Business operation boundary — not auto-traced |
| `QueryHandler.handle()` | Read-model lookup with business context |
| `EventHandler.handle()` (Projectors) | Projection logic, separate from Kafka consumer span |
| Multi-step sub-operations (e.g. `resolveTemplateIfNeeded`) | Internal workflow stages |

### CQRS + Arrow-kt Pattern

Since handlers return `Either<BaseError, T>`, **never throw** to record errors.
Set span status manually on `Either.Left`:

```kotlin
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.instrumentation.annotations.WithSpan
import io.quarkus.logging.Log

@ApplicationScoped
class CreateNotificationHandler(...) : CommandHandler<CreateNotificationCommand> {

    @WithSpan("notification.create")
    override fun handle(command: CreateNotificationCommand): Either<BaseError, Unit> =
        either {
            Span.current().apply {
                setAttribute("notification.id", command.id)
                setAttribute("notification.type", command.type.name)
            }
            Log.info("Creating notification id=${command.id} type=${command.type}")

            val saved = doWork(command).bind()

            Span.current().setAttribute("notification.persisted", true)
            Log.info("Notification created id=${command.id}")
        }.onLeft { error ->
            // Record Either.Left failures on the span — no exceptions thrown
            Span.current().apply {
                setStatus(StatusCode.ERROR, error.message)
                setAttribute("error.type", error::class.simpleName ?: "BaseError")
            }
            Log.error("Notification creation failed id=${command.id} reason=${error.message}")
        }
}
```

### Span Naming Conventions for Hermes

Use dot-notation: `{aggregate}.{layer}.{operation}`

| Handler | Span Name |
|---|---|
| `CreateNotificationHandler` | `notification.command.create` |
| `GetNotificationQueryHandler` | `notification.query.get` |
| `NotificationCreatedProjector` | `notification.projector.apply` |
| `CreateTemplateHandler` | `template.command.create` |
| `UpdateTemplateHandler` | `template.command.update` |
| `DeleteTemplateHandler` | `template.command.delete` |
| `GetTemplateQueryHandler` | `template.query.get` |
| `ListTemplatesQueryHandler` | `template.query.list` |

### Standard Attributes for Hermes Handlers

**Commands:**
```kotlin
Span.current().apply {
    setAttribute("notification.id", command.id)       // always
    setAttribute("notification.type", command.type.name)
    setAttribute("notification.template.requested", !command.templateName.isNullOrBlank())
}
```

**Queries:**
```kotlin
Span.current().apply {
    setAttribute("notification.id", query.id)
    setAttribute("notification.found", view != null)  // after DB call
}
```

**Projectors:**
```kotlin
Span.current().apply {
    setAttribute("notification.id", event.aggregateId)
    setAttribute("notification.type", event.type.name)
    setAttribute("notification.view.upserted", true)  // after upsert
}
```

### Log Level Guidelines

| Situation | Level | Example |
|---|---|---|
| Handler entry (command/query) | `Log.info` | `"Creating notification id=... type=..."` |
| Handler success | `Log.info` | `"Notification created id=..."` |
| Query miss (not found) | `Log.debug` | `"Querying notification id=..."` |
| Projector apply | `Log.info` | `"Projecting notification event aggregate=..."` |
| Validation failure | `Log.warn` | `"Notification creation failed id=... reason=..."` |
| Infrastructure failure | `Log.error` | Unexpected/server errors only |

### Dependency

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-opentelemetry</artifactId>
</dependency>
```

## Best Practices

1. **Use @WithSpan for simple cases** - cleaner and less boilerplate
2. **Manual spans for complex flows** - when you need fine-grained control
3. **Enhance auto-spans** - add business attributes to HTTP/DB spans
4. **Structured logs** - use key=value format for better parsing
5. **Semantic attributes** - follow [OTel conventions](https://opentelemetry.io/docs/specs/semconv/)
6. **Don't over-instrument** - Quarkus already covers infrastructure
7. **Arrow-kt Either** - set span `StatusCode.ERROR` in `.onLeft {}`, never throw

## Common Attributes to Add

| Attribute                    | When to Use                |
| ---------------------------- | -------------------------- |
| `user.id`                    | User-specific operations   |
| `tenant.id`                  | Multi-tenant applications  |
| `notification.id`            | All notification operations (Hermes) |
| `notification.type`          | EMAIL / SMS / WHATSAPP (Hermes) |
| `operation.result`           | success/failure/partial    |
| `error.type`                 | Classification of errors   |
| `cache.hit`                  | Cache operations           |
| `retry.count`                | Retry logic                |

## Checklist

- [ ] Is this operation already auto-instrumented? (Don't duplicate)
- [ ] Does this span add business context?
- [ ] Are attributes following semantic conventions?
- [ ] Are logs structured (key=value)?
- [ ] Are errors properly recorded (recordException + setStatus)?
- [ ] Is span properly closed (finally block or use)?
- [ ] Are sensitive data excluded from logs/spans?

## Response Format

When suggesting improvements:

1. **Identify** what's missing in observability
2. **Explain** why it matters for debugging/monitoring
3. **Provide** complete Kotlin code with annotations
4. **Highlight** key attributes and log messages
5. **Note** if auto-instrumentation already covers it
````
