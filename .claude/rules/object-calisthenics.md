---
paths:
  - "**/*.kt"
---

# Object Calisthenics Rules

> ⚠️ **Warning:** This file contains the 9 original Object Calisthenics rules adjusted for Kotlin. No additional rules must be added, and none of these rules should be replaced or removed.
> Examples may be added later if needed.

## Objective
This rule enforces the principles of Object Calisthenics to ensure clean, maintainable, and robust code in the backend, **primarily for business domain code**.

## Scope and Application
- **Primary focus**: Business domain classes (aggregates, entities, value objects, domain services)
- **Secondary focus**: Application layer services and use case handlers
- **Exemptions**: 
  - DTOs (Data Transfer Objects)
  - API models/contracts
  - Configuration classes
  - Simple data containers without business logic
  - Infrastructure code where flexibility is needed

## Key Principles
1. **One Level of Indentation per Method**:
   - Ensure methods are simple and do not exceed one level of indentation.

   ```kotlin
   // Bad Example - this method has multiple levels of indentation
   fun sendNewsletter() {
       for (user in users) {
           if (user.isActive) {
               // Do something
               mailer.send(user.email)
           }
       }
   }

   // Good Example - Extracted method to reduce indentation
   fun sendNewsletter() {
       for (user in users) {
           sendEmail(user)
       }
   }

   private fun sendEmail(user: User) {
       if (user.isActive) {
           mailer.send(user.email)
       }
   }

   // Good Example - Filtering users before sending emails
   fun sendNewsletter() {
       val activeUsers = users.filter { it.isActive }

       for (user in activeUsers) {
           mailer.send(user.email)
       }
   }
   ```

2. **Don't Use the ELSE Keyword**:

   - Avoid using the `else` keyword to reduce complexity and improve readability.
   - Use early returns to handle conditions instead.
   - Use Fail Fast principle
   - Use Guard Clauses to validate inputs and conditions at the beginning of methods.

   ```kotlin
   // Bad Example - Using else
   fun processOrder(order: Order) {
       if (order.isValid) {
           // Process order
       } else {
           // Handle invalid order
       }
   }
   
   // Good Example - Avoiding else
   fun processOrder(order: Order) {
       if (!order.isValid) return
       // Process order
   }
   ```

   Sample Fail fast principle:
   ```kotlin
   fun processOrder(order: Order?) {
       requireNotNull(order) { "Order cannot be null" }
       check(order.isValid) { "Invalid order" }
       // Process order
   }
   ```

3. **Wrapping All Primitives and Strings**:
   - Avoid using primitive types directly in your code.
   - Wrap them in classes to provide meaningful context and behavior.
   - In Kotlin, `@JvmInline value class` is perfect for this.

   ```kotlin
   // Bad Example - Using primitive types directly
   class User(
       val name: String,
       val age: Int
   )
   
   // Good Example - Wrapping primitives
   class User(
       private val name: String,
       private val age: Age
   )
   
   @JvmInline
   value class Age(val value: Int) {
       init {
           require(value >= 0) { "Age cannot be negative" }
       }
   }
   ```   

4. **First Class Collections**:
   - Use collections to encapsulate data and behavior, rather than exposing raw data structures.
   - First Class Collections: a class that contains a collection as an attribute should not contain any other attributes.

   ```kotlin
   // Bad Example - Exposing raw collection
   class Group(
      val id: Int,
      val name: String,
      val users: List<User>
   ) {
      fun getNumberOfActiveUsers(): Int {
         return users.count { it.isActive }
      }
   }

   // Good Example - Encapsulating collection behavior
   class Group(
      val id: Int,
      val name: String,
      private val userCollection: GroupUserCollection // The list of users is encapsulated in a class
   ) {
      fun getNumberOfActiveUsers(): Int {
         return userCollection.getActiveUsers().count()
      }
   }
   ```

5. **One Dot per Line**:
   - Avoid violating Law of Demeter by only having a single dot per line.

   ```kotlin
   // Bad Example - Multiple dots in a single line
   fun processOrder(order: Order) {
       val userEmail = order.user.getEmail().uppercase().trim()
       // Do something with userEmail
   }
   
   // Good Example - One dot per line
   class User {
     fun getEmail(): NormalizedEmail {
       return NormalizedEmail.create(/*...*/)       
     }
   }
   
   class Order {
     /*...*/
     fun confirmationEmail(): NormalizedEmail {
       return user.getEmail()         
     }
   }
   
   fun processOrder(order: Order) {
       val confirmationEmail = order.confirmationEmail()
       // Do something with confirmationEmail
   }
   ```

6. **Don't abbreviate**:
   - Use meaningful names for classes, methods, and variables.
   - Avoid abbreviations that can lead to confusion.

   ```kotlin
   // Bad Example - Abbreviated names
   class U {
       var n: String = ""
   }
   
   // Good Example - Meaningful names
   class User {
       var name: String = ""
   }
   ```

7. **Keep entities small (Class, method, namespace or package)**:
   - Limit the size of classes and methods to improve code readability and maintainability.
   - Each class should have a single responsibility and be as small as possible.
   
   Constraints:
   - Maximum 10 methods per class
   - Maximum 50 lines per class
   - Maximum 10 classes per package or namespace

   ```kotlin
   // Bad Example - Large class with multiple responsibilities
   class UserManager {
       fun createUser(name: String) { /*...*/ }
       fun deleteUser(id: Int) { /*...*/ }
       fun sendEmail(email: String) { /*...*/ }
   }

   // Good Example - Small classes with single responsibility
   class UserCreator {
       fun createUser(name: String) { /*...*/ }
   }
   class UserDeleter {
       fun deleteUser(id: Int) { /*...*/ }
   }

   class UserUpdater {
       fun updateUser(id: Int, name: String) { /*...*/ }
   }
   ```

8. **No Classes with More Than Two Instance Variables**:
   - Encourage classes to have a single responsibility by limiting the number of instance variables.
   - Limit the number of instance variables to two to maintain simplicity.
   - Do not count ILogger or any other logger as instance variable.

   ```kotlin
   // Bad Example - Class with multiple instance variables
   class UserCreateCommandHandler(
      // Bad: Too many instance variables
      private val userRepository: UserRepository,
      private val emailService: EmailService,
      private val logger: Logger,
      private val smsService: SmsService
   )

   // Good: Class with two instance variables
   class UserCreateCommandHandler(
      private val userRepository: UserRepository,
      private val notificationService: NotificationService,
      private val logger: Logger // This is not counted as instance variable
   )
   ```

9. **No Getters/Setters in Domain Classes**:
   - Avoid exposing setters for properties in domain classes.
   - Use private constructors and static factory methods for object creation.
   - In Kotlin, use `val` or `private set` to hide mutability.
   - **Note**: This rule applies primarily to domain classes, not DTOs or data transfer objects.

   ```kotlin
   // Bad Example - Domain class with public setters
   class User {  // Domain class
       var name: String = "" // Avoid this in domain classes
   }
   
   // Good Example - Domain class with encapsulation
   class User private constructor(val name: String) {  // Domain class
       companion object {
           fun create(name: String) = User(name)
       }
   }
   
   // Acceptable Example - DTO with public setters
   class UserDto {  // DTO - exemption applies
       var name: String = "" // Acceptable for DTOs
   }
   ```

## Implementation Guidelines
- **Domain Classes**:
  - Use private constructors and static factory methods for creating instances.
  - Avoid exposing setters for properties (prefer immutable `val`).
  - Apply all 9 rules strictly for business domain code.
  - Utilize functional error handling and value classes as per project standards.

- **Application Layer**:
  - Apply these rules to use case handlers and application services.
  - Focus on maintaining single responsibility and clean abstractions.

- **DTOs and Data Objects**:
  - Rules 3 (wrapping primitives), 8 (two instance variables), and 9 (no getters/setters) may be relaxed for DTOs.
  - Public properties with getters/setters are acceptable for data transfer objects.

- **Testing**:
  - Ensure tests validate the behavior of objects rather than their state.
  - Test classes may have relaxed rules for readability and maintainability.

- **Code Reviews**:
  - Enforce these rules during code reviews for domain and application code.
  - Be pragmatic about infrastructure and DTO code.

## References
- [Object Calisthenics - Original 9 Rules by Jeff Bay](https://www.cs.helsinki.fi/u/luontola/tdd-2009/ext/ObjectCalisthenics.pdf)
- [ThoughtWorks - Object Calisthenics](https://www.thoughtworks.com/insights/blog/object-calisthenics)
- [Clean Code: A Handbook of Agile Software Craftsmanship - Robert C. Martin](https://www.oreilly.com/library/view/clean-code-a/9780136083238/)