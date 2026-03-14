---
paths:
  - "**/*.kt"
---

## Language Overview

**Kotlin** is a modern, statically-typed programming language that runs on the JVM, Android, JavaScript, and Native platforms. It emphasizes **safety**, **conciseness**, **interoperability**, and **tooling support**.

### Key Principles

- **Null safety by design**: distinguish nullable and non-nullable types at compile-time
- **Expressive and concise**: reduce boilerplate while maintaining readability
- **Functional and OOP**: blend functional programming with object-oriented paradigms
- **Interoperable**: seamless Java interop with zero-cost abstractions
- **Coroutines-first**: structured concurrency built into the language

## Code Style and Formatting

### General Formatting

- **Indentation**: 4 spaces (no tabs)
- **Line length**: max 120 characters (soft limit, 100 recommended)
- **Line endings**: LF (Unix-style)
- **Final newline**: always insert at end of file
- **Blank lines**: use sparingly to separate logical sections
- **Trailing whitespace**: remove all trailing spaces

### Braces and Blocks

```kotlin
// Correct: opening brace on same line
if (condition) {
    doSomething()
}

// Correct: single-line expression body
fun square(x: Int) = x * x

// Correct: multi-line when
val result = when (x) {
    1 -> "one"
    2 -> "two"
    else -> "many"
}
```

### Imports

- Use **wildcard imports** sparingly (prefer explicit imports)
- Remove unused imports
- Order: standard library → third-party → project-internal

```kotlin
import kotlin.math.sqrt
import arrow.core.Either
import com.example.myapp.domain.User
```

## Idiomatic Kotlin

### 1. Expression-Body Functions

Prefer expression body for single-expression functions:

```kotlin
// ✅ Idiomatic
fun sum(a: Int, b: Int) = a + b

fun isValid(user: User) = user.age >= 18 && user.email.isNotBlank()

// ❌ Non-idiomatic
fun sum(a: Int, b: Int): Int {
    return a + b
}
```

### 2. `when` Expressions Over `if`/`else` Chains

```kotlin
// ✅ Idiomatic
fun describe(obj: Any) = when (obj) {
    1 -> "One"
    "Hello" -> "Greeting"
    is Long -> "Long number"
    !is String -> "Not a string"
    else -> "Unknown"
}

// ❌ Non-idiomatic
fun describe(obj: Any): String {
    if (obj == 1) {
        return "One"
    } else if (obj == "Hello") {
        return "Greeting"
    } else if (obj is Long) {
        return "Long number"
    } else {
        return "Unknown"
    }
}
```

### 3. Smart Casts

Kotlin automatically casts after type checks:

```kotlin
// ✅ Idiomatic
fun process(value: Any) {
    if (value is String) {
        println(value.length) // value is automatically String
    }
}

// ❌ Non-idiomatic
fun process(value: Any) {
    if (value is String) {
        println((value as String).length)
    }
}
```

### 4. Null Safety Operators

```kotlin
// Safe call operator
val length = text?.length

// Elvis operator (default value)
val length = text?.length ?: 0

// Safe cast
val user = obj as? User

// Non-null assertion (use sparingly!)
val length = text!!.length // throws NPE if null
```

### 5. Scope Functions

Choose the right scope function for each use case:

| Function | Object Reference | Return Value   | Use Case                           |
| :------- | :--------------- | :------------- | :--------------------------------- |
| `let`    | `it`             | Lambda result  | Transform nullable, scope limiting |
| `run`    | `this`           | Lambda result  | Object config + computation        |
| `with`   | `this`           | Lambda result  | Group calls on object              |
| `apply`  | `this`           | Context object | Object configuration               |
| `also`   | `it`             | Context object | Side effects                       |

```kotlin
// let: transform nullable
val length = text?.let { it.trim().length }

// apply: configure object
val person = Person().apply {
    name = "Alice"
    age = 30
}

// also: side effects (logging, validation)
val result = calculate()
    .also { println("Result: $it") }
    .also { validate(it) }

// run: combine config + computation
val result = service.run {
    configure()
    execute()
}

// with: group operations on object
with(canvas) {
    drawCircle(x, y, radius)
    drawLine(x1, y1, x2, y2)
}
```

### 6. String Templates

```kotlin
// ✅ Idiomatic
val name = "Alice"
val message = "Hello, $name!"
val computed = "Sum: ${a + b}"

// ❌ Non-idiomatic
val message = "Hello, " + name + "!"
val computed = "Sum: " + (a + b).toString()
```

### 7. Destructuring

```kotlin
// Data classes
data class User(val name: String, val age: Int)
val (name, age) = user

// Pairs and Triples
val (key, value) = map.entries.first()

// In loops
for ((index, value) in list.withIndex()) {
    println("$index: $value")
}
```

### 8. Collection Operations

Prefer functional operations over imperative loops:

```kotlin
// ✅ Idiomatic
val adults = users
    .filter { it.age >= 18 }
    .map { it.name }
    .sorted()

val total = items.sumOf { it.price }

val userMap = users.associateBy { it.id }

// ❌ Non-idiomatic
val adults = mutableListOf<String>()
for (user in users) {
    if (user.age >= 18) {
        adults.add(user.name)
    }
}
adults.sort()
```

### 9. Named Arguments and Default Parameters

```kotlin
// ✅ Idiomatic
fun createUser(
    name: String,
    age: Int,
    email: String = "",
    isActive: Boolean = true,
    role: String = "USER"
)

// Call with named arguments
createUser(
    name = "Alice",
    age = 30,
    email = "alice@example.com"
)

// ❌ Non-idiomatic: multiple overloads
fun createUser(name: String, age: Int)
fun createUser(name: String, age: Int, email: String)
fun createUser(name: String, age: Int, email: String, isActive: Boolean)
```

### 10. Extension Functions

Add behavior to existing types without inheritance:

```kotlin
// ✅ Idiomatic
fun String.isPalindrome() = this == reversed()

fun List<Int>.median(): Double {
    val sorted = sorted()
    return if (size % 2 == 0) {
        (sorted[size / 2 - 1] + sorted[size / 2]) / 2.0
    } else {
        sorted[size / 2].toDouble()
    }
}

// Usage
"radar".isPalindrome() // true
listOf(1, 3, 2).median() // 2.0
```

## Naming Conventions

### Packages

- **All lowercase**, no underscores
- Use reverse domain notation: `com.example.project.module`

```kotlin
package com.example.myapp.domain.user
```

### Classes and Objects

- **PascalCase** for class names
- Nouns or noun phrases

```kotlin
class UserRepository
object DatabaseConfig
sealed interface PaymentMethod
data class OrderItem(val id: String, val quantity: Int)
```

### Functions and Properties

- **camelCase** for functions and properties
- Verbs or verb phrases for functions
- Nouns for properties

```kotlin
fun calculateTotal() { }
fun isValid() = true
val userName: String
var isActive = false
```

### Constants

- **SCREAMING_SNAKE_CASE** for compile-time constants
- Use `const val` for primitive and String constants in top-level or object declarations

```kotlin
const val MAX_RETRIES = 3
const val DEFAULT_TIMEOUT = 5000L

object Config {
    const val API_URL = "https://api.example.com"
}
```

### Type Parameters

- Single uppercase letter or PascalCase name

```kotlin
class Box<T>(val value: T)
interface Repository<Entity, Id>
```

### Backing Properties

- Prefix private backing property with underscore

```kotlin
private val _items = mutableListOf<String>()
val items: List<String> get() = _items
```

## Null Safety

### Nullable Types

```kotlin
// Nullable type declaration
var name: String? = null

// Safe call
val length = name?.length

// Elvis operator (default)
val length = name?.length ?: 0

// Safe cast
val user = obj as? User

// let for nullable handling
name?.let {
    println("Name is $it")
}
```

### Platform Types

When interoperating with Java, be explicit about nullability:

```kotlin
// Java returns String (platform type String!)
// ✅ Be explicit
val name: String = javaObject.getName() // crashes if null
val name: String? = javaObject.getName() // safe
```

### Avoid `!!` Operator

The `!!` operator should be used sparingly. Consider alternatives:

```kotlin
// ❌ Avoid
val length = text!!.length

// ✅ Use safe call + elvis
val length = text?.length ?: 0

// ✅ Use requireNotNull with message
val length = requireNotNull(text) { "Text must not be null" }.length

// ✅ Use checkNotNull for state validation
val length = checkNotNull(text) { "Invalid state: text is null" }.length
```

## Classes and Objects

### Data Classes

Use for classes that primarily hold data:

```kotlin
data class User(
    val id: String,
    val name: String,
    val email: String,
    val age: Int
)

// Free: equals, hashCode, toString, copy, componentN
val copy = user.copy(age = 31)
val (id, name, email, age) = user
```

### Sealed Classes and Interfaces

Use for restricted class hierarchies (ADTs):

```kotlin
sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val message: String) : Result<Nothing>
    data object Loading : Result<Nothing>
}

fun handle(result: Result<String>) = when (result) {
    is Result.Success -> println(result.data)
    is Result.Error -> println("Error: ${result.message}")
    Result.Loading -> println("Loading...")
}
```

### Value Classes (Inline Classes)

Use for type-safe wrappers with zero runtime overhead:

```kotlin
@JvmInline
value class UserId(val value: String)

@JvmInline
value class Email private constructor(val value: String) {
    companion object {
        fun from(value: String): Email? {
            return if (value.contains("@")) Email(value) else null
        }
    }
}
```

### Object Declarations

```kotlin
// Singleton
object DatabaseConfig {
    val url = "jdbc:postgresql://localhost:5432/db"
}

// Companion object
class User {
    companion object {
        fun create(name: String) = User(name)
    }
}
```

### Enum Classes

```kotlin
enum class Status {
    PENDING,
    ACTIVE,
    COMPLETED,
    CANCELLED
}

// With properties
enum class Color(val rgb: Int) {
    RED(0xFF0000),
    GREEN(0x00FF00),
    BLUE(0x0000FF)
}
```

## Properties

### Backing Fields

```kotlin
class User {
    // Custom getter/setter with backing field
    var name: String = ""
        get() = field.uppercase()
        set(value) {
            field = value.trim()
        }
}
```

### Computed Properties

```kotlin
class Rectangle(val width: Int, val height: Int) {
    val area: Int
        get() = width * height
}
```

### Late-Initialized Properties

```kotlin
// For non-nullable properties initialized later
class MyTest {
    lateinit var subject: TestSubject

    @Before
    fun setup() {
        subject = TestSubject()
    }
}

// Check if initialized
if (::subject.isInitialized) {
    subject.doSomething()
}
```

### Lazy Properties

```kotlin
val heavyObject: HeavyObject by lazy {
    println("Initializing...")
    HeavyObject()
}

// Thread-safe by default; use lazy(LazyThreadSafetyMode.NONE) for single-threaded
```

## Functions

### Parameters

```kotlin
// Default parameters
fun connect(
    host: String = "localhost",
    port: Int = 8080,
    timeout: Long = 5000
)

// Varargs
fun log(level: String, vararg messages: String) {
    messages.forEach { println("[$level] $it") }
}

// Named arguments
connect(port = 9090, host = "example.com")
```

### Extension Functions

```kotlin
fun String.toSnakeCase() =
    replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()

fun <T> List<T>.secondOrNull() = if (size >= 2) this[1] else null
```

### Infix Functions

Use for functions that feel like operators:

```kotlin
infix fun Int.pow(exponent: Int): Int {
    var result = 1
    repeat(exponent) { result *= this }
    return result
}

val result = 2 pow 3 // 8
```

### Inline Functions

Use for higher-order functions to avoid lambda allocation overhead:

```kotlin
inline fun <T> measure(block: () -> T): T {
    val start = System.currentTimeMillis()
    return block().also {
        println("Took ${System.currentTimeMillis() - start}ms")
    }
}
```

### Operator Overloading

```kotlin
data class Point(val x: Int, val y: Int) {
    operator fun plus(other: Point) = Point(x + other.x, y + other.y)
    operator fun times(scale: Int) = Point(x * scale, y * scale)
}

val p1 = Point(1, 2)
val p2 = Point(3, 4)
val sum = p1 + p2 // Point(4, 6)
```

## Collections

### Creation

```kotlin
// Immutable (preferred)
val list = listOf(1, 2, 3)
val set = setOf("a", "b", "c")
val map = mapOf("key" to "value", "foo" to "bar")

// Mutable (use sparingly)
val mutableList = mutableListOf(1, 2, 3)
val mutableSet = mutableSetOf("a", "b")
val mutableMap = mutableMapOf("key" to "value")

// Prefer immutable + functional operations
val newList = list + 4
val filtered = list.filter { it > 1 }
```

### Operations

```kotlin
// Transformation
val doubled = list.map { it * 2 }
val flattened = listOfLists.flatten()
val flatMapped = users.flatMap { it.orders }

// Filtering
val evens = list.filter { it % 2 == 0 }
val adults = users.filter { it.age >= 18 }
val first = list.firstOrNull { it > 10 }

// Aggregation
val sum = list.sum()
val total = items.sumOf { it.price }
val max = list.maxOrNull()
val grouped = users.groupBy { it.country }

// Checking
val hasAdult = users.any { it.age >= 18 }
val allActive = users.all { it.isActive }
val noneExpired = items.none { it.isExpired() }

// Association
val userMap = users.associateBy { it.id }
val nameToUser = users.associateBy({ it.name }, { it })
```

### Sequences

Use for large collections or multiple chained operations:

```kotlin
// ✅ Efficient: lazy evaluation, single pass
val result = largeList.asSequence()
    .filter { it.isValid() }
    .map { it.transform() }
    .take(10)
    .toList()

// ❌ Inefficient: creates intermediate lists
val result = largeList
    .filter { it.isValid() }
    .map { it.transform() }
    .take(10)
```

## Functional Programming

### Higher-Order Functions

```kotlin
fun <T, R> List<T>.mapCustom(transform: (T) -> R): List<R> {
    val result = mutableListOf<R>()
    for (item in this) {
        result.add(transform(item))
    }
    return result
}

// Usage
val doubled = listOf(1, 2, 3).mapCustom { it * 2 }
```

### Function Types and Lambdas

```kotlin
// Function type
val operation: (Int, Int) -> Int = { a, b -> a + b }

// Lambda with receiver
val buildString: StringBuilder.() -> Unit = {
    append("Hello")
    append(" World")
}

// Trailing lambda syntax
items.forEach { item ->
    println(item)
}

// Single parameter: it
items.forEach {
    println(it)
}
```

### Returning from Lambdas

```kotlin
// Return from enclosing function
fun processItems(items: List<Int>) {
    items.forEach {
        if (it < 0) return // returns from processItems
        println(it)
    }
}

// Return from lambda only
fun processItems(items: List<Int>) {
    items.forEach {
        if (it < 0) return@forEach // continues to next item
        println(it)
    }
}
```

## Coroutines

### Basics

```kotlin
// Launch: fire-and-forget
lifecycleScope.launch {
    fetchData()
}

// Async: return value
val deferred = lifecycleScope.async {
    computeResult()
}
val result = deferred.await()

// Structured concurrency
coroutineScope {
    launch { task1() }
    launch { task2() }
    // waits for both tasks
}
```

### Suspend Functions

```kotlin
suspend fun fetchUser(id: String): User {
    delay(1000) // suspends, doesn't block
    return api.getUser(id)
}

// Calling from coroutine
lifecycleScope.launch {
    val user = fetchUser("123")
    updateUI(user)
}
```

### Error Handling

```kotlin
// try-catch
lifecycleScope.launch {
    try {
        val data = fetchData()
        updateUI(data)
    } catch (e: Exception) {
        handleError(e)
    }
}

// CoroutineExceptionHandler
val handler = CoroutineExceptionHandler { _, exception ->
    println("Caught $exception")
}

lifecycleScope.launch(handler) {
    fetchData()
}
```

### Flow

```kotlin
// Create flow
fun fetchUsers(): Flow<User> = flow {
    val users = api.getUsers()
    users.forEach { emit(it) }
}

// Collect flow
lifecycleScope.launch {
    fetchUsers()
        .filter { it.isActive }
        .map { it.name }
        .collect { name ->
            println(name)
        }
}

// StateFlow (hot, state-holding)
private val _state = MutableStateFlow<UiState>(UiState.Loading)
val state: StateFlow<UiState> = _state.asStateFlow()

// SharedFlow (hot, event broadcasting)
private val _events = MutableSharedFlow<Event>()
val events: SharedFlow<Event> = _events.asSharedFlow()
```

## Error Handling

### Exceptions

```kotlin
// Throwing
throw IllegalArgumentException("Invalid parameter")

// Catching
try {
    riskyOperation()
} catch (e: IOException) {
    handleIOError(e)
} catch (e: Exception) {
    handleGenericError(e)
} finally {
    cleanup()
}
```

### Functional Error Handling

Use libraries like Arrow-kt for functional error handling:

```kotlin
// Using Arrow's Either
sealed interface AppError
data class ValidationError(val message: String) : AppError
data class NetworkError(val cause: Throwable) : AppError

fun validateUser(user: User): Either<ValidationError, User> =
    if (user.email.contains("@")) {
        Either.Right(user)
    } else {
        Either.Left(ValidationError("Invalid email"))
    }

// Using Result (standard library)
fun fetchData(): Result<Data> = runCatching {
    api.getData()
}

// Pattern matching on Result
fetchData()
    .onSuccess { data -> updateUI(data) }
    .onFailure { error -> handleError(error) }
```

## Testing

### Unit Tests (JUnit 5)

```kotlin
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows

class CalculatorTest {

    @Test
    fun `should add two numbers`() {
        val result = Calculator.add(2, 3)
        assertEquals(5, result)
    }

    @Test
    fun `should throw on division by zero`() {
        assertThrows<ArithmeticException> {
            Calculator.divide(10, 0)
        }
    }
}
```

### Mocking (MockK)

```kotlin
import io.mockk.*

class UserServiceTest {

    private val repository = mockk<UserRepository>()
    private val service = UserService(repository)

    @Test
    fun `should fetch user by id`() {
        // Given
        val userId = "123"
        val expectedUser = User(userId, "Alice")
        every { repository.findById(userId) } returns expectedUser

        // When
        val result = service.getUser(userId)

        // Then
        assertEquals(expectedUser, result)
        verify { repository.findById(userId) }
    }
}
```

### Coroutine Testing

```kotlin
import kotlinx.coroutines.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class MyViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should load data on init`() = runTest {
        val viewModel = MyViewModel(repository)

        advanceUntilIdle() // execute pending coroutines

        assertEquals(UiState.Success(data), viewModel.state.value)
    }
}
```

## Best Practices

### Immutability

- **Prefer `val` over `var`**: use immutable variables by default
- **Use immutable collections**: `List`, `Set`, `Map` over mutable variants
- **Copy instead of mutating**: use `copy()` on data classes

```kotlin
// ✅ Immutable
val user = User("Alice", 30)
val updated = user.copy(age = 31)

// ❌ Mutable
var user = User("Alice", 30)
user.age = 31
```

### Composition Over Inheritance

```kotlin
// ✅ Composition
class OrderService(
    private val paymentService: PaymentService,
    private val inventoryService: InventoryService
)

// ❌ Inheritance
class OrderService : PaymentService(), InventoryService()
```

### Single Responsibility

Each class/function should have one reason to change:

```kotlin
// ✅ Single responsibility
class UserValidator {
    fun validate(user: User): ValidationResult
}

class UserRepository {
    fun save(user: User)
    fun findById(id: String): User?
}

// ❌ Multiple responsibilities
class UserManager {
    fun validate(user: User): ValidationResult
    fun save(user: User)
    fun sendEmail(user: User)
    fun logActivity(user: User)
}
```

### Fail Fast

Validate inputs early and throw/return errors immediately:

```kotlin
fun processOrder(order: Order) {
    require(order.items.isNotEmpty()) { "Order must have items" }
    require(order.total > 0) { "Order total must be positive" }

    // process order
}
```

### Use Platform Types Carefully

When calling Java code, be explicit about nullability:

```kotlin
// Java: String getName()
// ✅ Explicit
val name: String = javaObject.name ?: "Unknown"

// ❌ Platform type propagation
val name = javaObject.name // String! (platform type)
```

### Avoid Over-Engineering

- Don't create abstractions prematurely
- Start simple, refactor when needed
- Prefer duplication over wrong abstraction

### Document Public APIs

```kotlin
/**
 * Calculates the factorial of a non-negative integer.
 *
 * @param n the non-negative integer
 * @return the factorial of [n]
 * @throws IllegalArgumentException if [n] is negative
 */
fun factorial(n: Int): Long {
    require(n >= 0) { "n must be non-negative" }
    return if (n <= 1) 1 else n * factorial(n - 1)
}
```

## Common Pitfalls

### 1. Overusing `!!`

```kotlin
// ❌ Dangerous
val length = text!!.length

// ✅ Safe alternatives
val length = text?.length ?: 0
val length = requireNotNull(text).length
```

### 2. Mutable Collections in Public APIs

```kotlin
// ❌ Exposes mutable list
class UserManager {
    val users = mutableListOf<User>()
}

// ✅ Expose immutable view
class UserManager {
    private val _users = mutableListOf<User>()
    val users: List<User> get() = _users
}
```

### 3. Ignoring Coroutine Context

```kotlin
// ❌ Blocking in coroutine
lifecycleScope.launch {
    val data = blockingNetworkCall() // blocks thread!
}

// ✅ Use suspend function or withContext
lifecycleScope.launch {
    val data = withContext(Dispatchers.IO) {
        blockingNetworkCall()
    }
}
```

### 4. Not Using Sequences for Large Collections

```kotlin
// ❌ Creates intermediate collections
val result = largeList
    .filter { it > 10 }
    .map { it * 2 }
    .take(5)

// ✅ Lazy evaluation
val result = largeList.asSequence()
    .filter { it > 10 }
    .map { it * 2 }
    .take(5)
    .toList()
```

### 5. Equals/HashCode on Mutable Properties

```kotlin
// ❌ Don't use var in data class equals/hashCode
data class User(var name: String, var age: Int)

// ✅ Use val for identity properties
data class User(val id: String, var name: String, var age: Int)
```

## Additional Resources

- **Official Docs**: [kotlinlang.org](https://kotlinlang.org)
- **Style Guide**: [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- **Coroutines Guide**: [kotlinlang.org/docs/coroutines-guide.html](https://kotlinlang.org/docs/coroutines-guide.html)
- **Arrow-kt**: [arrow-kt.io](https://arrow-kt.io) (functional programming library)
