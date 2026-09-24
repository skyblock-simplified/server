# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

See the root [`CLAUDE.md`](../CLAUDE.md) for cross-cutting patterns.
See [`spring-framework/CLAUDE.md`](../../Simplified-Dev/spring-framework/CLAUDE.md) for the reusable Spring server framework (server config and the Gson message converter, Spring 7 path-segment API versioning, Spring Security API key auth with Bucket4j rate limiting, content-negotiated HTML or JSON error responses, and the SpringDoc customizers that document the key scheme).

## Build & Test

```bash
# From repo root
./gradlew :server:build          # Build (includes shadowJar)
./gradlew :server:test           # Run all tests

# Fat JAR (shadowJar merged into build task)
./gradlew :server:shadowJar      # Output: build/libs/server-0.1.0.jar
```

## Module Overview

`server` is the SkyBlock-specific Spring Boot REST server. It depends on `server-api` (framework) and `minecraft-api` via Maven coordinates. The `server-api` framework provides API versioning, API key authentication, error handling, and server configuration; this module provides the concrete controllers, OpenAPI metadata, and application entry point.

Follows the same split pattern as `discord-api` (framework) vs `bot` (implementation).

### Entry Point

- **`SimplifiedServer`** - Spring Boot application. Provides a `Gson` bean via `MinecraftApi.getGson()` that `ServerWebConfig` (from `server-api`) picks up automatically for the `GsonHttpMessageConverter`. Jackson auto-configuration remains enabled for SpringDoc's internal OpenAPI spec generation. Uses `ServerConfig.optimized()` to supply all default properties programmatically. Scans both `dev.sbs.server` and `dev.sbs.serverapi` packages via `@SpringBootApplication(scanBasePackages = ...)`.

### Package Structure

**`config/`** - Application-specific configuration:
- `OpenApiConfig` - `@Configuration` defining the `OpenAPI` metadata bean (title, description, version) used by SpringDoc for spec generation at `/v3/api-docs` and rendered by the Scalar UI at the root path.

**`controller/`** - Spring MVC REST controllers proxying upstream APIs:
- `MojangController` - Mojang API proxy endpoints under `/mojang/`. Provides player profile lookup (by username or UUID), username resolution, UUID resolution, skin/cape properties, and bulk username lookup. Delegates to `MojangProxy` for upstream calls with automatic IPv6 rotation.
- `HypixelController` - Hypixel API proxy endpoints under `/hypixel/`. Provides player data, guild lookups (by ID, name, or player), online status, player counts, punishment statistics, and game information. Delegates to `HypixelEndpoint`.
- `SkyBlockController` - SkyBlock API proxy endpoints under `/skyblock/`. Provides profiles, auctions (by ID, profile, or player), active/ended auction listings, bazaar products, museum, garden, news, and fire sales. Delegates to `HypixelEndpoint`.
- `ResourceController` - SkyBlock resource proxy endpoints under `/resources/`. Provides skill definitions, collection definitions, item definitions, and election data. None require an API key. Delegates to `HypixelEndpoint`.

### Dependencies

- **`server-api`** - Reusable Spring server framework (API versioning, auth, error handling, config). Transitively provides `api` and `spring-boot-starter-web`.
- **`minecraft-api`** - Minecraft/Hypixel API client, Mojang proxy.
- **`springdoc-openapi-starter-webmvc-scalar`** - OpenAPI spec generation and Scalar UI (implementation-specific).

### Configuration

All properties are managed programmatically through `ServerConfig` (from `server-api`):
- `ServerConfig.builder()` - Full control with Spring Boot defaults
- `ServerConfig.optimized()` - Production preset (virtual threads, compression, graceful shutdown, reverse proxy support)
- `api.key.authentication.enabled` - Toggles API key security (default `true` in `ServerConfig`)
- `springdocEnabled` - Toggles SpringDoc OpenAPI spec generation and Scalar UI (default `true` in `ServerConfig`)

### API Documentation (SpringDoc + Scalar)

- **Dependency:** `springdoc-openapi-starter-webmvc-scalar` (version in `gradle/libs.versions.toml`)
- **Root path:** `GET /` redirects to the Scalar UI (`springdoc.use-root-path=true`)
- **OpenAPI spec:** `GET /v3/api-docs` returns the auto-generated OpenAPI 3.0 JSON
- **Jackson coexistence:** Jackson auto-configuration is enabled (not excluded) so SpringDoc can use it internally. Gson remains the primary HTTP serializer because `GsonHttpMessageConverter` is registered first by `ServerWebConfig`
- **Controller annotations:** `@Tag` (class-level) and `@Operation` (method-level) from `io.swagger.v3.oas.annotations` enrich the generated documentation
