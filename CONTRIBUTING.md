# Contributing to Simplified Server

Thank you for your interest in contributing! This document explains how to get
started, what to expect during the review process, and the conventions this
project follows.

## Table of Contents

- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Development Setup](#development-setup)
- [Making Changes](#making-changes)
  - [Branching Strategy](#branching-strategy)
  - [Code Style](#code-style)
  - [Commit Messages](#commit-messages)
  - [Testing](#testing)
- [Submitting a Pull Request](#submitting-a-pull-request)
- [Reporting Issues](#reporting-issues)
- [Project Architecture](#project-architecture)
- [Legal](#legal)

## Getting Started

### Prerequisites

| Requirement | Version | Notes |
|-------------|---------|-------|
| [JDK](https://adoptium.net/) | **21+** | Required (virtual threads) |
| [Git](https://git-scm.com/) | 2.x+ | For cloning and contributing |
| [IntelliJ IDEA](https://www.jetbrains.com/idea/) | Latest | Recommended IDE (Gradle support built-in) |

For running the server locally:

| Requirement | Notes |
|-------------|-------|
| Hypixel API key | Required for most Hypixel/SkyBlock endpoints |
| Environment variables | `HYPIXEL_API_KEY`, and `INET6_NETWORK_PREFIX` for the `/mojang` endpoints, which throw without it |
| `ApiKeyStore` bean | Required while `api.key.authentication.enabled` is `true`, which `ServerConfig.optimized()` sets; keep a local one in `src/main/java/dev/sbs/server/config/LocalApiKeyStoreConfig.java`, which `.gitignore` excludes |

### Development Setup

1. **Fork and clone the repository**

   [Fork the repository](https://github.com/SkyBlock-Simplified/server/fork),
   then clone your fork:

   ```bash
   git clone https://github.com/<your-username>/server.git
   cd server
   ```

2. **Dependency modules**

   This module depends on the
   [spring-framework](https://github.com/simplified-dev/spring-framework) server
   framework (`com.github.simplified-dev:spring-framework`), the
   [hypixel](https://github.com/simplified-api/hypixel),
   [mojang](https://github.com/simplified-api/mojang), and
   [skyblock](https://github.com/simplified-api/skyblock) API modules, the
   SkyBlock Simplified [api](https://github.com/skyblock-simplified/api) module,
   and the [client](https://github.com/simplified-dev/client),
   [gson-extras](https://github.com/simplified-dev/gson-extras), and
   [manager](https://github.com/simplified-dev/manager) libraries, each declared
   in `build.gradle.kts` as a JitPack coordinate pinned to a commit. A standalone
   build resolves them from JitPack. To work on one of them alongside this
   server, clone it and include it in a Gradle composite build whose
   `dependencySubstitution` maps its coordinate onto the local project.

3. **Build the project**

   The Gradle wrapper is included - no separate Gradle installation is needed.

   ```bash
   ./gradlew build
   ```

4. **Open in IntelliJ IDEA**

   Open the project root as a Gradle project. IntelliJ will automatically
   detect the `build.gradle.kts` and import dependencies. Ensure annotation
   processing is enabled (`Settings > Build > Compiler > Annotation Processors`)
   for the `dev.simplified.annotations` processor.

5. **Verify the setup**

   ```bash
   ./gradlew test
   ```

## Making Changes

### Branching Strategy

- Create a feature branch from `master` for your work.
- Use a descriptive branch name: `fix/mojang-uuid-parsing`,
  `feat/skyblock-bingo-endpoint`, `docs/endpoint-examples`.

```bash
git checkout -b feat/my-feature master
```

### Code Style

#### General

- **Spring conventions** - Follow standard Spring Boot patterns for
  `@RestController`, `@RequestMapping`, `@Configuration`, and `@Bean`.
- **Collections** - Always use `Concurrent.newList()`, `Concurrent.newMap()`,
  `Concurrent.newSet()` instead of `new ArrayList`, `new HashMap`, etc.
- **Annotations** - Use `@NotNull` / `@Nullable` from `org.jetbrains.annotations`
  on all public method parameters and return types.
- **Simplified annotations** - Use `@Getter`, `@NoArgsConstructor`,
  `@RequiredArgsConstructor`, etc. from `dev.simplified.annotations` where
  appropriate. This module declares no Lombok dependency.
- **OpenAPI annotations** - Annotate controllers with `@Tag` (class-level) and
  `@Operation` / `@Parameter` (method-level) from `io.swagger.v3.oas.annotations`.

#### Braces

- Omit curly braces when the `if` body is a single line.
- Use curly braces when the body wraps across multiple lines.

#### Javadoc

- **Class level** - Noun phrase describing what the type is.
- **Method level** - Active verb, third person singular, describing what the
  method does.
- **Tags** - Always include `@param`, `@return`, `@throws` on public methods.
  Tag descriptions are lowercase sentence fragments with no trailing period.
  Single space after the param name (no column alignment).
- **Punctuation** - Only use single hyphens (` - `) as separators. Never em
  dashes, `&mdash;`, or double hyphens.
- Never use `@author` or `@since`.

#### Controllers

- Each controller class maps to a single upstream API domain (Mojang, Hypixel,
  SkyBlock, Resources).
- Use `@ResponseStatus(HttpStatus.OK)` on all endpoint methods.
- Delegate to the upstream Feign contracts through a private `contract()`
  helper that reads the client or proxy from `ServerApi`, rather than
  injecting Spring beans.
- Group related endpoints under a shared `@RequestMapping` base path.

### Commit Messages

Write clear, concise commit messages that describe *what* changed and *why*.

```
Add garden endpoint to SkyBlock controller

Proxies the Hypixel SkyBlock garden API for fetching garden data by
profile ID.
```

- Use the imperative mood ("Add", "Fix", "Update", not "Added", "Fixes").
- Keep the subject line under 72 characters.
- Add a body when the *why* isn't obvious from the subject.

### Testing

Tests use JUnit 5 (Jupiter):

```bash
./gradlew test
```

- Add tests for new functionality where practical.
- Many endpoints depend on live upstream APIs and database state, so not all
  controller methods can be unit tested in isolation. Integration tests that
  require a running server or external services should be clearly documented.

## Submitting a Pull Request

1. **Push your branch** to your fork.

   ```bash
   git push origin feat/my-feature
   ```

2. **Open a Pull Request** against the `master` branch of
   [SkyBlock-Simplified/server](https://github.com/SkyBlock-Simplified/server).

3. **In the PR description**, include:
   - A summary of the changes and the motivation behind them.
   - Steps to test or verify the changes.
   - Whether the change adds new API endpoints or modifies existing ones.

4. **Respond to review feedback.** PRs may go through one or more rounds of
   review before being merged.

### What gets reviewed

- Correctness of endpoint mappings and upstream client delegation.
- OpenAPI annotation completeness (`@Tag`, `@Operation`, `@Parameter`).
- Impact on the public API surface. New endpoints or breaking changes to
  existing endpoints should be discussed in the issue tracker before
  implementation.
- Adherence to the controller patterns established by existing controllers.
- Compatibility with the `spring-framework` server framework (error handling,
  API key security, message converters).

## Reporting Issues

Use [GitHub Issues](https://github.com/SkyBlock-Simplified/server/issues)
to report bugs or request features.

When reporting a bug, include:

- **Java version** (`java --version`)
- **Spring Boot version** (check `gradle/libs.versions.toml`)
- **Operating system**
- **Full error stacktrace** (if applicable)
- **Request URL and response** (if applicable)
- **Steps to reproduce**
- **Expected vs. actual behavior**

## Project Architecture

A brief overview to help you find your way around the codebase:

```
src/main/java/dev/sbs/server/
├── SimplifiedServer.java              # Spring Boot entry point with Gson bean
├── ServerApi.java                     # Gson, KeyManager, and the upstream clients
├── config/
│   └── OpenApiConfig.java             # OpenAPI metadata (title, description, version)
└── controller/
    ├── MojangController.java          # /mojang/ - profile, username, UUID, properties, bulk
    ├── HypixelController.java         # /hypixel/ - player, guild, status, counts, games
    ├── SkyBlockController.java        # /skyblock/ - profiles, auctions, bazaar, museum, garden
    └── ResourceController.java        # /resources/ - skills, collections, items, election
```

### Key extension points

- **New controller** - Add a `@RestController` in `controller/` with `@Tag`
  and `@Operation` annotations. Delegate to the upstream Feign contracts that
  `ServerApi` holds.
- **API key store** - Supply an `ApiKeyStore` `@Bean`; spring-framework's
  `ApiKeySecurityConfig` requires one while `api.key.authentication.enabled` is
  `true`, and `InMemoryApiKeyStore` is its reference implementation.
- **Custom error body** - spring-framework's `ErrorResponseWriter.buildBody()`
  builds the JSON error body, through the `errorResponseWriter` bean that
  `ServerWebConfig` registers; its `ErrorController` advice is final.
- **Custom Gson** - The `Gson` `@Bean` in `SimplifiedServer` controls the
  serializer used for all JSON responses. Modify it to customize serialization.
- **Server tuning** - Replace `ServerConfig.optimized()` with
  `ServerConfig.builder()` for fine-grained control over Tomcat, compression,
  HTTP/2, and other server settings.

## Legal

By submitting a pull request, you agree that your contributions are licensed
under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0),
the same license that covers this project.
