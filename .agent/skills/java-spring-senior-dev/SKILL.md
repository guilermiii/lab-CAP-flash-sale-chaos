Senior Java and Spring Boot engineer persona and technical standards. Use when designing, coding, reviewing, architecting, or refactoring modern Java (Java 21+) and Spring Boot (3.x/4.x) backend systems, REST APIs, microservices, database transactions, concurrency, and cloud-native infrastructure.
Instructions
java-spring-senior-dev

Senior Java and Spring Boot engineering guidelines for developing scalable, high-throughput, and clean enterprise backend services.
When to Use

    Designing or implementing Spring Boot 3.x/4.x enterprise microservices or modular monoliths.

    Writing modern Java (Java 21/25) utilizing Virtual Threads, Records, Sealed Interfaces, and Pattern Matching.

    Architecting Clean or Hexagonal (Ports and Adapters) architectures in Java.

    Optimizing database interactions, handling JPA/Hibernate queries, avoiding N+1 problems, and managing ACID transaction locks.

    Writing automated test suites using JUnit 5, AssertJ, and Testcontainers.

    Packaging containerized, cloud-native Java applications with Docker multi-stage builds.

Engineering Standards and Principles
1. Modern Java Idioms (Java 21+)

    Favor immutability: Use Records for DTOs, value objects, and projection responses.

    Leverage Pattern Matching: Replace complex instanceof casts and nested if-else ladders with pattern matching for switch and records.

    Concurrency with Virtual Threads: Enable virtual threads (spring.threads.virtual.enabled=true) for I/O-bound throughput instead of complex reactive pipelines unless explicitly streaming.

    Scoped values and structured concurrency: Prefer structured task lifecycles when coordinating concurrent subtasks.

2. Spring Boot Architecture and Dependency Injection

    Architecture: Organize code by domain boundaries or Hexagonal architecture (domain, application/ports, infrastructure/adapters). Keep domain logic free of Spring framework annotations whenever possible.

    Constructor Injection: Always use constructor injection (via @RequiredArgsConstructor with final fields or explicit constructor). Never use @Autowired on fields.

    Configuration Properties: Use @ConfigurationProperties with immutable records and @Validated instead of scattered @Value annotations.

    Error Handling: Centralize API exception handling via @RestControllerAdvice and RFC 7807 / RFC 9457 ProblemDetail structures.

3. Data Persistence, Transactions, and Concurrency

    Transaction Boundaries: Apply @Transactional strictly at the service/use-case layer. Keep transactions as short as possible to avoid holding database connections.

    Concurrency Control:
        Optimistic Locking: Use @Version on entities for read-heavy, low-contention entities.
        Pessimistic Locking: Use PESSIMISTIC_WRITE (SELECT ... FOR UPDATE) with explicit ordering on IDs to prevent deadlocks in financial transfers and inventory reservation.

    Hibernate and JPA Performance:
        Set hibernate.default_batch_fetch_size (e.g. 25-50) and use JOIN FETCH or Entity Graphs to eliminate N+1 queries.
        Disable Open Session in View (spring.jpa.open-in-view=false) to prevent deferred queries during serialization.
        Map entity IDs using appropriate types and sequence generators rather than GenerationType.IDENTITY when batch inserts are required.

    Numeric Precision: Never use float or double for monetary calculations. Always use BigDecimal or store integer cents (long) in the database.

4. API Design and Validation

    Explicit DTOs: Never expose JPA entities directly in HTTP request or response bodies.

    Bean Validation: Validate incoming payloads using jakarta.validation annotations (@NotNull, @Size, @Positive, etc.) on DTO records.

    Idempotency: Enforce idempotency keys (Idempotency-Key header) on critical state-changing POST/PUT operations, storing keys atomically in Redis or a relational table.

5. Testing Strategy

    Unit Tests: Test domain logic and isolated components using pure JUnit 5 and Mockito. Fast execution, zero Spring context overhead.

    Slice Tests: Use @WebMvcTest for controller routing/validation and @DataJpaTest for complex repository queries.

    Integration Tests: Use Testcontainers to run real dependencies (PostgreSQL, MySQL, Redis, Kafka) instead of in-memory H2 databases to ensure production fidelity.

    Assertions: Write expressive assertions using AssertJ (assertThat(...)).

6. Observability and Cloud-Native Readiness

    Health and Metrics: Expose Spring Boot Actuator health, info, and metrics endpoints (/actuator/health, /actuator/metrics).

    Structured Logging: Emit JSON logs with correlation IDs (traceId, spanId) via MDC or Micrometer Tracing.

    Packaging: Use multi-stage Docker builds based on lightweight JDK/JRE runtime images (e.g. Eclipse Temurin or Distroless).

Gotchas and Anti-Patterns to Avoid

    Never leave open-in-view enabled in production.

    Never use @Data on JPA @Entity classes; equals() and hashCode() implementations generated by Lombok on collections cause lazy initialization exceptions and performance degradation. Use @Getter, @Setter, and custom equals/hashCode based on business natural keys or ID.

    Avoid catching generic Exception or Throwable. Handle specific business exceptions.

    Avoid synchronous blocking calls inside asynchronous reactive flows, or blocking operations that saturate the common fork-join pool.
