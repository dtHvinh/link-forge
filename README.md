# LinkForge — a Kotlin + Spring Boot learning project

A small URL shortener that touches **every item** on your study list: Kotlin core, Gradle (Kotlin DSL), Redis, Docker,
docker-compose, and Nginx — all built inside WSL2.

This is a **plan, not a solution**. Structure, signatures, and acceptance criteria are given; the bodies are yours to
write.

---

## Why a URL shortener?

| Your topic            | Where it shows up naturally                                             |
|-----------------------|-------------------------------------------------------------------------|
| Redis                 | The whole datastore — keys, TTL, counters, sorted sets. No SQL needed.  |
| Null safety           | `redisTemplate.opsForValue().get(key)` returns `String?`. Unavoidable.  |
| Data / sealed classes | Request & response DTOs; error hierarchies for `@RestControllerAdvice`. |
| Extension functions   | Base62 encoding, URL validation, `Map<String,String>` → domain mapping. |
| Scope functions       | Bean configuration, nullable Redis reads, builder-style construction.   |
| Collections           | Recent-links feed, stats aggregation.                                   |
| Exception handling    | Redis down, malformed URL, alias already taken.                         |
| Gradle (Kotlin DSL)   | `bootJar`, layered JARs, version catalog, Spring plugins.               |
| Docker / compose      | Three services on one network, named volume, bind mounts, env vars.     |
| Nginx                 | Reverse proxy on :80, so short links read as `http://localhost/aB3xY`.  |

---

## Architecture

```
                     ┌──────────────────────────────────────┐
   browser  ──:80──► │  nginx        reverse proxy          │
                     └──────────────────┬───────────────────┘
                                        │  docker network "linkforge-net"
                                        ▼
                     ┌──────────────────────────────────────┐
                     │  app          Spring Boot :8080      │
                     │  Controller → Service → Repository   │
                     └──────────────────┬───────────────────┘
                                        │
                                        ▼
                     ┌──────────────────────────────────────┐
                     │  redis        :6379, named volume    │
                     └──────────────────────────────────────┘
```

## API surface

| Method   | Path                  | Purpose                                                                          |
|----------|-----------------------|----------------------------------------------------------------------------------|
| `POST`   | `/api/links`          | Create a link. Body: `{ "url": "...", "ttlSeconds": 3600?, "alias": "mylink"? }` |
| `GET`    | `/{code}`             | 302 redirect to the target, increments hit counter                               |
| `GET`    | `/api/links/{code}`   | Stats: target URL, hits, createdAt, remaining TTL                                |
| `GET`    | `/api/links?limit=20` | Most recently created links                                                      |
| `DELETE` | `/api/links/{code}`   | Remove a link                                                                    |
| `GET`    | `/actuator/health`    | Spring Actuator, includes a Redis health check for free                          |

---

# Folder structure

The full target layout. Build it incrementally — the phases below tell you when each piece appears.

```
linkforge/
│
├── settings.gradle.kts                 # rootProject.name, includes ":app"
├── build.gradle.kts                    # root: plugins declared but not applied
├── gradle.properties                   # JVM args, kotlin.code.style
├── gradle/
│   ├── libs.versions.toml              # version catalog — single source of truth
│   └── wrapper/                        # committed: gradlew works with no Gradle installed
│
├── .dockerignore
├── .env                                # local compose overrides, git-ignored
├── docker-compose.yml
├── Dockerfile
├── README.md
│
├── nginx/
│   ├── nginx.conf                      # bind-mounted into the nginx container
│   └── logs/                           # bind mount target — readable from the host
│
└── app/
    ├── build.gradle.kts                # the real module build file
    └── src/
        ├── main/
        │   ├── kotlin/com/linkforge/
        │   │   │
        │   │   ├── LinkForgeApplication.kt          # @SpringBootApplication + main()
        │   │   │
        │   │   ├── config/
        │   │   │   ├── AppProperties.kt             # @ConfigurationProperties data class
        │   │   │   ├── RedisConfig.kt               # RedisTemplate / serializer beans
        │   │   │   └── WebConfig.kt                 # CORS, interceptors (optional)
        │   │   │
        │   │   ├── domain/                          # pure Kotlin — zero Spring imports
        │   │   │   ├── ShortLink.kt                 # data class
        │   │   │   ├── LinkError.kt                 # sealed interface
        │   │   │   └── Base62.kt                    # extension functions
        │   │   │
        │   │   ├── repository/
        │   │   │   ├── LinkRepository.kt            # interface
        │   │   │   └── RedisLinkRepository.kt       # @Repository impl
        │   │   │
        │   │   ├── service/
        │   │   │   └── LinkService.kt               # @Service — business rules
        │   │   │
        │   │   ├── web/
        │   │   │   ├── LinkController.kt            # @RestController — /api/links
        │   │   │   ├── RedirectController.kt        # @Controller — /{code}
        │   │   │   ├── GlobalExceptionHandler.kt    # @RestControllerAdvice
        │   │   │   └── dto/
        │   │   │       ├── CreateLinkRequest.kt     # + jakarta validation annotations
        │   │   │       ├── LinkResponse.kt
        │   │   │       └── LinkStatsResponse.kt
        │   │   │
        │   │   └── exception/
        │   │       └── LinkExceptions.kt            # custom exception hierarchy
        │   │
        │   └── resources/
        │       ├── application.yml                  # defaults, ${ENV:fallback} placeholders
        │       ├── application-local.yml            # profile: localhost redis
        │       ├── application-docker.yml           # profile: redis by service name
        │       ├── logback-spring.xml               # optional
        │       └── static/
        │           └── index.html                   # tiny form to create links
        │
        └── test/
            └── kotlin/com/linkforge/
                ├── domain/Base62Test.kt             # pure unit test, no Spring context
                ├── service/LinkServiceTest.kt       # mocked repository
                └── web/LinkControllerTest.kt        # @WebMvcTest slice
```

### Why this shape

- **`domain/` imports nothing from Spring.** It's plain Kotlin, unit-testable in milliseconds. Every Spring project that
  becomes painful to test got that way by skipping this rule.
- **`repository/` is an interface plus a Redis implementation.** Swapping in an in-memory fake for tests then costs
  nothing, and it makes the dependency-inversion point concrete rather than theoretical.
- **`web/dto/` is separate from `domain/`.** Your API shape and your internal model change for different reasons — don't
  serialize domain objects directly.
- **Two controllers.** `/{code}` is a catch-all redirect and belongs apart from the JSON API. Keeping them in separate
  files makes the routing precedence obvious.

---

# Phase 0 — WSL2 workspace

Everything below happens **inside WSL2**, not on the Windows filesystem. Files under `/mnt/c/...` are slow, and Gradle's
file watching plus Docker bind mounts both misbehave across the boundary.

```bash
# Windows PowerShell (admin), if not already set up
wsl --install -d Ubuntu
wsl --update

# inside the Ubuntu shell
mkdir -p ~/projects && cd ~/projects
```

Install in WSL2:

- **JDK 21** — `sudo apt install openjdk-21-jdk` (Spring Boot 4.x needs 17 minimum; 21 gives you virtual threads)
- **Docker** — Docker Desktop with the WSL2 backend, or `docker.io` + `docker-compose-plugin` natively in Ubuntu
- Optional: [SDKMAN](https://sdkman.io) for juggling JDK versions

**Checkpoint 0:** `java -version`, `docker run hello-world`, and `docker compose version` all succeed inside WSL2.

> Open the project through the IDE's WSL remote mode (IntelliJ or VS Code) so the IDE edits files *in* Linux rather than
> over `\\wsl$`.

---

# Phase 1 — Kotlin Core

Before any Spring, spend a session in [play.kotlinlang.org](https://play.kotlinlang.org) writing the pure-logic pieces.
They have zero dependencies — perfect playground material — and they drop straight into `domain/` in Phase 2.

### 1.1 Base62 (extension functions)

```kotlin
private const val ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

fun Long.toBase62(): String {
    TODO()
}
fun String.fromBase62(): Long {
    TODO()
}
```

Acceptance: `0L.toBase62() == "0"`, `61L.toBase62() == "Z"`, `62L.toBase62() == "10"`, and
`x.toBase62().fromBase62() == x` for any non-negative `x`.

### 1.2 URL validation (null safety)

```kotlin
fun String.normalizedUrlOrNull(): String?   // prepends https:// if no scheme; null if invalid
```

Acceptance: `"example.com"` → `"https://example.com"`; `"not a url"` → `null`; `"   "` → `null`. Use `java.net.URI` and
catch what it throws — don't let it escape.

### 1.3 Domain model (data classes, default args)

```kotlin
data class ShortLink(
    val code: String,
    val targetUrl: String,
    val createdAt: Instant,
    val hits: Long = 0,
    val ttlSeconds: Long? = null,      // null = never expires
)
```

Exercise `copy()`, destructuring, and `==` vs `===` here. Cheapest way to internalise what `data` actually generates.

### 1.4 Sealed error hierarchy

```kotlin
sealed interface LinkError {
    data class InvalidUrl(val input: String) : LinkError
    data class AliasTaken(val alias: String) : LinkError
    data object NotFound : LinkError
    data class StorageUnavailable(val cause: Throwable) : LinkError
}
```

Write a function mapping each case to an HTTP status with an **exhaustive `when`** and no `else`. Delete a branch and
watch the compiler stop you — that's the entire value of sealed types, and in Phase 5 this `when` becomes the body of
your `@RestControllerAdvice`.

### 1.5 Collections drills

Given a `List<ShortLink>`, produce:

- top 5 by hits → `sortedByDescending` + `take`
- hits per domain → `groupBy` + `mapValues` + `sumOf`
- unexpired only → `filter`
- `Map<String, ShortLink>` keyed by code → `associateBy`
- a comma-joined summary → `joinToString`

### 1.6 Scope functions

Write each at least once and be able to say *why* that one:

| Function | Returns       | Typical use here                             |
|----------|---------------|----------------------------------------------|
| `apply`  | receiver      | Configuring a `RedisTemplate` bean           |
| `also`   | receiver      | Logging mid-chain without changing the value |
| `let`    | lambda result | Acting on a nullable Redis read              |
| `run`    | lambda result | Computing a value from a receiver            |
| `with`   | lambda result | Several calls on one object                  |

### 1.7 Exception handling

Practise `try/catch/finally`, `runCatching { }.getOrElse { }`, and turning a caught exception into a typed error. Decide
the rule now, because Spring will push you toward one of them:

> **Throw custom exceptions at the boundary; keep sealed results inside the domain.** `@RestControllerAdvice` catches
> thrown exceptions, so the boundary layer is where they belong.

**Checkpoint 1:** All of the above compiles and behaves correctly in the Kotlin Playground.

📺 [freeCodeCamp Kotlin course](https://www.youtube.com/watch?v=F9UC9DY-vIU) — the classes, null safety, and lambda
sections map onto 1.3–1.6.
📖 [kotlinlang.org/docs](https://kotlinlang.org/docs/home.html) · [Spring's Kotlin docs](https://docs.spring.io/spring-framework/reference/languages/kotlin.html)

---

# Phase 2 — Gradle (Kotlin DSL) + Spring skeleton

### 2.1 Generate from Initializr

Don't hand-write the build file first. Generate it, then read every line and understand why it's there:

```bash
cd ~/projects
curl https://start.spring.io/starter.zip \
  -d type=gradle-project-kotlin \
  -d language=kotlin \
  -d javaVersion=21 \
  -d groupId=com.linkforge \
  -d artifactId=linkforge \
  -d packageName=com.linkforge \
  -d dependencies=web,data-redis,actuator,validation \
  -o linkforge.zip
unzip linkforge.zip -d linkforge && cd linkforge
```

Or use the web UI at [start.spring.io](https://start.spring.io) and pick: **Spring Web**, **Spring Data Redis**, *
*Spring Boot Actuator**, **Validation**.

> Current stable is <cite index="8-1">Spring Boot 4.1.0, released June 2026, requiring Java 17 as a minimum</cite>
> and <cite index="2-1">compatible with Java up to and including Java 26</cite>. Note that <cite index="8-1">Spring Boot
> 3.5 reached end of life on June 30, 2026</cite> — so start on 4.x. Let Initializr pin the exact version rather than
> copying numbers from any guide, including this one.

Then restructure into the `app/` module layout shown above (`settings.gradle.kts` → `include("app")`). A single-module
project would work fine, but doing the split teaches you what `settings.gradle.kts` is actually for.

### 2.2 The Kotlin plugins Spring needs — and why

```kotlin
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")        // ← allopen: Kotlin classes are final by default
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}
```

`kotlin("plugin.spring")` is not optional cosmetics. Kotlin classes are `final` unless marked `open`, but Spring's CGLIB
proxying (`@Configuration`, `@Transactional`, AOP) needs to subclass them. The plugin auto-opens classes carrying Spring
annotations. **Remove it once, watch the failure, put it back.** That five-minute experiment will save you an hour of
confusion later.

### 2.3 Version catalog — `gradle/libs.versions.toml`

Migrate the Initializr dependencies into a catalog. Let the Spring BOM manage Spring versions; only pin what it doesn't
cover.

```toml
[versions]
kotlin = "2.2.0"    # use whatever Initializr chose
springBoot = "4.1.0"    # likewise
springDepMgmt = "1.1.7"

[libraries]
spring-web = { module = "org.springframework.boot:spring-boot-starter-web" }
spring-data-redis = { module = "org.springframework.boot:spring-boot-starter-data-redis" }
spring-actuator = { module = "org.springframework.boot:spring-boot-starter-actuator" }
spring-validation = { module = "org.springframework.boot:spring-boot-starter-validation" }
jackson-kotlin = { module = "com.fasterxml.jackson.module:jackson-module-kotlin" }
kotlin-reflect = { module = "org.jetbrains.kotlin:kotlin-reflect" }
spring-test = { module = "org.springframework.boot:spring-boot-starter-test" }

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-spring = { id = "org.jetbrains.kotlin.plugin.spring", version.ref = "kotlin" }
spring-boot = { id = "org.springframework.boot", version.ref = "springBoot" }
spring-depmgmt = { id = "io.spring.dependency-management", version.ref = "springDepMgmt" }
```

No `version.ref` on the starters — the Spring BOM supplies them. That's the point of `io.spring.dependency-management`.

### 2.4 `app/build.gradle.kts` — details that matter

- `kotlin { jvmToolchain(21) }`
- `kotlin { compilerOptions { freeCompilerArgs.add("-Xjsr305=strict") } }` — makes Spring's `@Nullable` annotations
  enforce Kotlin null safety across the Java boundary. Without it you get platform types and silent NPEs.
- `tasks.withType<Test> { useJUnitPlatform() }`
- `tasks.bootJar { archiveFileName.set("linkforge.jar") }` — stable name for the Dockerfile
- `jackson-module-kotlin` and `kotlin-reflect` — needed so Jackson can deserialize data classes with constructor params
  and default values

Add a custom task as a Kotlin DSL exercise:

```kotlin
tasks.register("printVersion") {
    group = "linkforge"
    doLast { println("LinkForge ${project.version} on Boot ${libs.versions.springBoot.get()}") }
}
```

### 2.5 Configuration properties (null safety at the edge)

`config/AppProperties.kt`:

```kotlin
@ConfigurationProperties(prefix = "linkforge")
data class AppProperties(
    val baseUrl: String = "http://localhost",
    val defaultTtlSeconds: Long? = null,
    val codeLength: Int = 6,
)
```

Register with `@EnableConfigurationProperties(AppProperties::class)` on the application class, then inject it as a
constructor param. Note what you *don't* write: no `System.getenv`, no `!!`, no manual `toIntOrNull()`. Spring binds and
type-converts, and the data class defaults are the fallbacks.

`application.yml`:

```yaml
spring:
  application:
    name: linkforge
  data:
    redis:
      host: ${REDIS_HOST:localhost}      # env var, with a local fallback
      port: ${REDIS_PORT:6379}

linkforge:
  base-url: ${BASE_URL:http://localhost:8080}
  default-ttl-seconds: ${DEFAULT_TTL:}
  code-length: 6

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics

server:
  port: ${PORT:8080}
```

Those `${ENV:default}` placeholders are the seam that Phase 7's docker-compose plugs into. Relaxed binding means
`base-url` in YAML maps to `baseUrl` in Kotlin and `LINKFORGE_BASE_URL` as an env var — all three are the same property.

**Checkpoint 2:**

```bash
./gradlew bootRun          # starts, logs the Spring banner, then fails on Redis (expected)
./gradlew bootJar && java -jar app/build/libs/linkforge.jar
./gradlew printVersion
```

---

# Phase 3 — Redis

### 3.1 Run Redis and learn it by hand first

```bash
docker run -d --name redis-dev -p 6379:6379 redis:7-alpine
docker exec -it redis-dev redis-cli
```

Type these before wrapping any of them in Kotlin:

```
SET foo bar
GET foo
SETNX foo baz          # returns 0 — key exists. This is how alias uniqueness works.
EXPIRE foo 60
TTL foo
INCR counter:code
HSET link:abc url https://example.com createdAt 1730000000 hits 0
HGETALL link:abc
HINCRBY link:abc hits 1
ZADD links:recent 1730000000 abc
ZREVRANGE links:recent 0 19
DEL link:abc
```

### 3.2 Key design

| Key             | Type   | Holds                                                |
|-----------------|--------|------------------------------------------------------|
| `link:{code}`   | HASH   | `url`, `createdAt`, `hits`                           |
| `alias:{alias}` | STRING | the code it maps to — `SETNX` guards uniqueness      |
| `links:recent`  | ZSET   | score = createdAt epoch, member = code               |
| `counter:code`  | STRING | `INCR`-ed for the next id → fed through `toBase62()` |

### 3.3 `config/RedisConfig.kt` (scope functions in anger)

```kotlin
@Configuration
class RedisConfig {

    @Bean
    fun redisTemplate(factory: RedisConnectionFactory): RedisTemplate<String, String> =
        RedisTemplate<String, String>().apply {          // ← apply: configure, return receiver
            connectionFactory = factory
            keySerializer = StringRedisSerializer()
            valueSerializer = StringRedisSerializer()
            hashKeySerializer = StringRedisSerializer()
            hashValueSerializer = StringRedisSerializer()
            afterPropertiesSet()
        }
}
```

Spring Boot auto-configures a `StringRedisTemplate` already, so strictly you could skip this — write it anyway. Seeing
`apply` used exactly as it's meant to be used is worth the fifteen lines, and default `RedisTemplate` serialization (JDK
binary) produces unreadable keys in `redis-cli`, which is a genuinely useful thing to have discovered on purpose.

> **Design note:** Spring Data Redis also offers `@RedisHash` + `CrudRepository`, which would generate all of this for
> you. Use `RedisTemplate` here — you're trying to *learn Redis*, and the repository abstraction hides exactly the
> commands you want to practise. Try `@RedisHash` afterwards as a comparison exercise.

### 3.4 Repository

`repository/LinkRepository.kt` — the interface, pure domain types:

```kotlin
interface LinkRepository {
    fun nextCode(): String
    fun save(link: ShortLink)
    fun findByCode(code: String): ShortLink?
    fun recordHit(code: String): Long
    fun recent(limit: Int): List<ShortLink>
    fun reserveAlias(alias: String, code: String): Boolean
    fun delete(code: String): Boolean
}
```

`repository/RedisLinkRepository.kt`:

```kotlin
@Repository
class RedisLinkRepository(
    private val redis: StringRedisTemplate,        // constructor injection — no @Autowired
) : LinkRepository {

    override fun nextCode(): String =
        TODO("redis.opsForValue().increment(\"counter:code\") returns Long? — handle it")

    override fun findByCode(code: String): ShortLink? =
        TODO("opsForHash<String,String>().entries(key); empty map means null. No !!")

    override fun recent(limit: Int): List<ShortLink> =
        TODO("opsForZSet().reverseRange(...) then mapNotNull(::findByCode)")

    // ...
}
```

Two Kotlin points worth pausing on:

1. **Constructor injection needs no annotation.** A single constructor means Spring uses it automatically. `val` makes
   the dependency immutable. This is idiomatic Kotlin-Spring and is why field injection with `lateinit var` is a smell
   here.
2. **`findByCode` returns `ShortLink?`** and that nullability propagates up through the service to the controller.
   Resist `!!` at every layer — if you want one, that's a design signal, not a syntax problem.

Add an extension function to keep the mapping tidy:

```kotlin
private fun Map<String, String>.toShortLink(code: String): ShortLink? = TODO()
```

**Checkpoint 3:** Links survive an app restart. `redis-cli KEYS 'link:*'` shows readable data (not JDK-serialized
garbage). A `ttlSeconds: 60` link disappears after a minute.

---

# Phase 4 — Service layer

`service/LinkService.kt` is where the sealed types from 1.4 earn their keep:

```kotlin
@Service
class LinkService(
    private val repository: LinkRepository,
    private val properties: AppProperties,
) {
    fun create(request: CreateLinkRequest): ShortLink = TODO(
        """
        1. normalize + validate the URL  -> throw InvalidUrlException if null
        2. if alias present, reserveAlias -> throw AliasTakenException if false
        3. otherwise nextCode()
        4. build ShortLink, save, return
        """
    )

    fun resolve(code: String): String = TODO("findByCode ?: throw LinkNotFoundException; recordHit; return url")
    fun stats(code: String): ShortLink = TODO()
    fun recent(limit: Int): List<ShortLink> = TODO("coerce limit into 1..100")
    fun delete(code: String)
}
```

Note the service takes `AppProperties` directly — no `@Value("\${...}")` string literals scattered through the codebase.
Typed configuration in one place.

**Checkpoint 4:** `LinkServiceTest` passes with a hand-written in-memory `LinkRepository` fake — no Spring context, no
Redis, runs in milliseconds. That speed is the payoff for having kept `domain/` and the repository interface
Spring-free.

---

# Phase 5 — Web layer

### 5.1 DTOs with validation — mind the `@field:` prefix

```kotlin
data class CreateLinkRequest(
    @field:NotBlank(message = "url is required")
    val url: String,

    @field:Min(60) @field:Max(31_536_000)
    val ttlSeconds: Long? = null,

    @field:Pattern(regexp = "^[a-zA-Z0-9_-]{3,32}$")
    val alias: String? = null,
)
```

⚠️ **The `@field:` use-site target is required.** A Kotlin constructor `val` generates a parameter, a field, and a
getter — without `@field:`, the annotation lands on the constructor parameter where Bean Validation never looks, and
validation silently does nothing. This is the single most common Kotlin-Spring gotcha and it fails quietly, which is
worse than failing loudly.

### 5.2 Controllers

```kotlin
@RestController
@RequestMapping("/api/links")
class LinkController(private val service: LinkService, private val props: AppProperties) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateLinkRequest): LinkResponse = TODO()

    @GetMapping("/{code}")
    fun stats(@PathVariable code: String): LinkStatsResponse = TODO()

    @GetMapping
    fun recent(@RequestParam(defaultValue = "20") limit: Int): List<LinkResponse> = TODO()

    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable code: String) = TODO()
}
```

```kotlin
@Controller
class RedirectController(private val service: LinkService) {

    @GetMapping("/{code:[a-zA-Z0-9]{3,32}}")     // regex constraint keeps it off /actuator, /api
    fun redirect(@PathVariable code: String): ResponseEntity<Void> = TODO(
        "ResponseEntity.status(FOUND).location(URI.create(target)).build()"
    )
}
```

The regex in the path variable is doing real work. Without it, `/{code}` swallows `/actuator/health` and every other
unmatched path. Spring's matching is more specific than a raw catch-all, but constrain it anyway — it documents the
intent.

### 5.3 `GlobalExceptionHandler.kt` — where the sealed `when` lands

```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(LinkNotFoundException::class)
    fun handleNotFound(e: LinkNotFoundException): ProblemDetail = TODO()

    @ExceptionHandler(InvalidUrlException::class, AliasTakenException::class)
    fun handleBadRequest(e: RuntimeException): ProblemDetail = TODO()

    @ExceptionHandler(RedisConnectionFailureException::class)
    fun handleStorageDown(e: Exception): ProblemDetail = TODO("503, and log the cause")

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ProblemDetail = TODO(
        "e.bindingResult.fieldErrors — a collections exercise: map to field -> message"
    )
}
```

`ProblemDetail` (RFC 7807) is built into Spring Boot 3+ and gives you consistent error JSON for free.

**Checkpoint 5:**

```bash
curl -X POST localhost:8080/api/links -H 'Content-Type: application/json' -d '{"url":"example.com"}'
curl -i localhost:8080/<code>                       # 302 with Location header
curl -X POST localhost:8080/api/links -H 'Content-Type: application/json' -d '{"url":""}'   # 400 + field errors
curl localhost:8080/actuator/health                  # {"status":"UP"} — includes Redis
```

Then stop Redis and confirm you get a clean 503, not a stack trace in the response body.

---

# Phase 6 — Docker

### 6.1 Layered JARs — Spring's Docker feature

Spring Boot's `bootJar` produces a **layered** JAR that can be exploded into layers ordered by how often they change:
dependencies (rarely), spring-boot-loader, snapshot dependencies, application code (every build). Copy them into the
image separately and a code change rebuilds only the last, tiny layer.

```dockerfile
# ---- build stage ----
FROM gradle:8.14-jdk21 AS build
WORKDIR /src
COPY app/settings.gradle.kts build.gradle.kts gradle.properties ./
COPY app/gradle ./gradle
COPY app/build.gradle.kts ./app/
RUN gradle dependencies --no-daemon || true      # warms the dependency cache
COPY app/src ./app/src
RUN gradle bootJar --no-daemon

# ---- extract layers ----
FROM eclipse-temurin:21-jre-alpine AS extract
WORKDIR /extracted
COPY --from=build /src/app/build/libs/linkforge.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination .

# ---- runtime stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring
COPY --from=extract /extracted/app/dependencies/ ./
COPY --from=extract /extracted/app/spring-boot-loader/ ./
COPY --from=extract /extracted/app/snapshot-dependencies/ ./
COPY --from=extract /extracted/app/application/ ./
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

> `-Djarmode=tools ... extract` is the Boot 3.3+ form; older guides show `-Djarmode=layertools list/extract`. If your
> Boot version rejects one, try the other.

Dockerfile syntax to actually understand, not just paste:

- `FROM ... AS name` — multi-stage; JDK and Gradle caches never reach the final image
- `COPY` vs `ADD` — prefer `COPY`; `ADD` also untars and fetches URLs, rarely what you want
- `RUN` (build time) vs `CMD` / `ENTRYPOINT` (run time)
- `ENTRYPOINT` in **exec form** (JSON array) so signals reach the JVM — shell form means `docker stop` waits the full 10
  seconds and then kills it
- `USER` — don't run as root; Spring Boot's own docs push this
- **Layer caching** — build files copied before source, so editing Kotlin doesn't re-resolve dependencies

`.dockerignore`:

```
.git
**/build
.gradle
*.md
nginx/logs
```

### 6.2 Build and compare

```bash
docker build -t linkforge:0.1 .
docker images | grep linkforge
docker run --rm -p 8080:8080 -e REDIS_HOST=host.docker.internal linkforge:0.1
docker history linkforge:0.1        # see the layer sizes you just designed
```

**Checkpoint 6:** Image runs standalone. Then write a naive single-stage Dockerfile (`FROM gradle`, no extraction) and
compare `docker images` output — the size difference *is* the lesson. Also try `./gradlew bootBuildImage`, which builds
an OCI image with no Dockerfile at all via Cloud Native Buildpacks, and note where each approach makes sense.

---

# Phase 7 — docker-compose

```yaml
services:
  redis:
    image: redis:7-alpine
    command: [ "redis-server", "--appendonly", "yes" ]
    volumes:
      - redis-data:/data                    # named volume — survives `compose down`
    networks: [ linkforge-net ]
    healthcheck:
      test: [ "CMD", "redis-cli", "ping" ]
      interval: 5s
      retries: 5

  app:
    build: .
    environment:
      SPRING_PROFILES_ACTIVE: docker
      REDIS_HOST: redis                     # ← service name resolves on the docker network
      REDIS_PORT: 6379
      BASE_URL: http://localhost
      JAVA_TOOL_OPTIONS: "-XX:MaxRAMPercentage=75"
    depends_on:
      redis:
        condition: service_healthy
    networks: [ linkforge-net ]
    healthcheck:
      test: [ "CMD", "wget", "-qO-", "http://localhost:8080/actuator/health" ]
      interval: 10s
      retries: 5

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"                             # the ONLY service exposed to the host
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/conf.d/default.conf:ro   # bind mount, host → container
      - ./nginx/logs:/var/log/nginx                            # bind mount to a physical path
    depends_on:
      app:
        condition: service_healthy
    networks: [ linkforge-net ]

volumes:
  redis-data:

networks:
  linkforge-net:
    driver: bridge
```

What this file is teaching, item by item from your list:

- **docker network** — all three join `linkforge-net`. `redis` and `app` are reachable *by service name* and are **not**
  published to the host. Only nginx binds a port. Confirm with `docker compose ps` and try `redis-cli -p 6379` from the
  host — it should fail.
- **docker service** — `depends_on` + `condition: service_healthy` means the app waits for a *ready* Redis, not merely a
  started container. Remove the condition once and watch the startup race.
- **named volume** — `redis-data` is Docker-managed. `docker volume ls`, `docker volume inspect linkforge_redis-data`.
- **bind mount from container to a physical path** — `./nginx/logs` means access logs land in your WSL2 filesystem and
  you can `tail -f` them from the host shell.
- **environment variables** — injected here, resolved by the `${REDIS_HOST:localhost}` placeholders in
  `application.yml`. `SPRING_PROFILES_ACTIVE: docker` activates `application-docker.yml`. Move the values into a `.env`
  file afterwards and confirm compose picks it up automatically.

```bash
docker compose up -d --build
docker compose ps
docker compose logs -f app
docker compose exec redis redis-cli
docker compose down          # volume survives
docker compose down -v       # volume destroyed — verify the data is really gone
```

**Checkpoint 7:** `docker compose down && docker compose up -d` and your links are still there. `down -v` and they're
gone.

---

# Phase 8 — Nginx

`nginx/nginx.conf`:

```nginx
upstream linkforge_app {
    server app:8080;              # "app" = compose service name
}

server {
    listen 80;
    server_name localhost;
    access_log /var/log/nginx/access.log;

    location / {
        proxy_pass http://linkforge_app;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /actuator {
        deny all;                 # don't expose management endpoints publicly
    }
}
```

Then extend one change at a time, verifying each:

1. **Rate limiting** — `limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;` in the `http` block,
   `limit_req zone=api burst=20 nodelay;` inside `location /api/`.
2. **Gzip** — `gzip on; gzip_types application/json;`
3. **Static UI** — serve `index.html` from nginx directly at `/ui` so you can create links in a browser instead of curl.
4. **Custom 404** — a friendly page for dead short codes.
5. **Forwarded headers** — add `server.forward-headers-strategy: framework` to `application.yml` so Spring builds
   correct absolute URLs behind the proxy. Without it your `LinkResponse.shortUrl` says `:8080` when it should say
   `:80`. Good, concrete lesson in why those `X-Forwarded-*` headers exist.

**Checkpoint 8:** `curl -i localhost/api/links` works on port 80. `docker compose ps` shows only nginx publishing a
port. Hammering the API trips the rate limiter. `tail -f nginx/logs/access.log` works from the host — proof the bind
mount is live. `curl localhost/actuator/health` is blocked while
`docker compose exec app wget -qO- localhost:8080/actuator/health` still works.

---

# Suggested schedule

| Session | Focus             | Done when                                                               |
|---------|-------------------|-------------------------------------------------------------------------|
| 1       | Phase 0 + 1.1–1.4 | Base62 round-trips in the Playground                                    |
| 2       | Phase 1.5–1.7     | Sealed `when` is exhaustive; scope functions understood                 |
| 3       | Phase 2           | `bootRun` starts; `printVersion` works; you can explain `plugin.spring` |
| 4       | Phase 3.1–3.2     | Comfortable in `redis-cli`                                              |
| 5       | Phase 3.3–3.4     | Data survives restart, readable in `redis-cli`                          |
| 6       | Phase 4           | Service tests pass with a fake repository                               |
| 7       | Phase 5           | Full API responds; validation returns clean 400s                        |
| 8       | Phase 6           | Image built, layers understood, sizes compared                          |
| 9       | Phase 7           | Three services up, volume and network proven                            |
| 10      | Phase 8           | Reverse proxy, rate limiting, forwarded headers correct                 |

---

# Stretch goals

- **Testcontainers** — `@Testcontainers` + a real Redis container in integration tests. Spring Boot's
  `@ServiceConnection` wires the container's host and port into the context automatically; it's genuinely elegant.
- **Coroutines** — switch to WebFlux + `ReactiveRedisTemplate` and make the repository `suspend`. This covers the async
  gap in your Kotlin Core list, and Spring's Kotlin coroutine support is first-class.
- **Redis pub/sub** — publish a hit event; a `@Component` listener aggregates daily stats.
- **Actuator metrics** — a custom `MeterRegistry` counter per link; expose Prometheus, add Grafana as a fourth compose
  service.
- **Nginx caching** — `proxy_cache` on redirect responses; measure the difference with `ab` or `wrk`.
- **Delegated properties** — a `by env(...)` delegate as a pure Kotlin exercise, then compare it against
  `@ConfigurationProperties` and articulate why Spring's version is better here.
- **CI** — GitHub Actions running `./gradlew build` and `docker build`.

---

# Rules to hold yourself to

1. **No `!!`.** Every one is a design question you haven't answered.
2. **No `else` in a `when` over a sealed type.** Exhaustiveness is what you're paying for.
3. **`domain/` imports nothing from Spring.** The moment it does, your fast tests die.
4. **Constructor injection with `val`.** No `@Autowired` fields, no `lateinit var`.
5. **`@field:` on every validation annotation.** It fails silently otherwise.
6. **Type the Redis and Docker commands by hand at least once** before scripting them.
7. **Commit after every checkpoint.** Ten small commits beat one giant one, and you'll want to diff back.

# Deployment

```docker
docker build -f deploy/Dockerfile -t link-forge/link-service .
docker run -p 8080:8080 link-forge/link-service
```