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

`server` is the SkyBlock-specific Spring Boot REST server. It is built on Simplified-Dev's [`spring-framework`](../../Simplified-Dev/spring-framework) (package `dev.simplified.serverapi`, coordinate `com.github.simplified-dev:spring-framework`), which provides API versioning, API key authentication, error handling, and server configuration; this module provides the concrete controllers, OpenAPI metadata, and application entry point. Its dependencies are JitPack coordinates pinned to commits in `build.gradle.kts`.

Follows the same split as Simplified-Dev's [`discord4j-framework`](../../Simplified-Dev/discord4j-framework) (framework) vs [`bot`](../bot) (implementation).

### Entry Point

- **`SimplifiedServer`** - Spring Boot application. Provides a `Gson` bean via `ServerApi.getGson()` that `ServerWebConfig` (from `spring-framework`) picks up for the `GsonHttpMessageConverter`. Jackson auto-configuration remains enabled for SpringDoc's internal OpenAPI spec generation. `main()` registers the `HYPIXEL_API_KEY` environment variable with `ServerApi.getKeyManager()`, passes `INET6_NETWORK_PREFIX` to `ServerApi.setInet6NetworkPrefix()` when set, and uses `ServerConfig.optimized()` to supply all default properties programmatically. Scans `dev.sbs.server` and `dev.simplified.serverapi` via `@SpringBootApplication(scanBasePackages = ...)`; the second is what registers the framework's security, error handling, API versioning, and web configuration.
- **`ServerApi`** - Static holder for the shared `Gson` and `GsonSettings`, the `KeyManager` that supplies the Hypixel `API-Key` header, the Hypixel and SkyBlock Simplified `Client`s, and the Mojang `Proxy` that rotates IPv6 source addresses across the prefix `setInet6NetworkPrefix()` receives.

### API Key Store

`ServerConfig.optimized()` sets `api.key.authentication.enabled=true`, so spring-framework's `ApiKeySecurityConfig` loads and requires an `ApiKeyStore` bean; without one, startup fails with `NoSuchBeanDefinitionException`. **No committed source in this module declares an `ApiKeyStore`.** A local checkout keeps one in `src/main/java/dev/sbs/server/config/LocalApiKeyStoreConfig.java`, a path `.gitignore` excludes. A deployment either supplies an `ApiKeyStore` bean, or sets `api.key.authentication.enabled=false` (a `--api.key.authentication.enabled=false` argument or the `API_KEY_AUTHENTICATION_ENABLED` environment variable, both of which outrank the default properties `ServerConfig` supplies) to get `PermitAllSecurityConfig`, which permits every request and leaves the controllers' `@PreAuthorize` unenforced.

### Package Structure

**`config/`** - Application-specific configuration:
- `OpenApiConfig` - `@Configuration` defining the `OpenAPI` metadata bean (title, description, version) used by SpringDoc for spec generation at `/v3/api-docs` and rendered by the Scalar UI at the root path.

**`controller/`** - Spring MVC REST controllers proxying upstream APIs. Each carries a class-level `@PreAuthorize("isAuthenticated()")`, so, while API key authentication is on, every request needs a valid `X-API-Key` header:
- `MojangController` - Mojang API proxy endpoints under `/mojang/`. Provides player profile lookup (by username or UUID), username resolution, UUID resolution, skin/cape properties, and bulk username lookup. Delegates to the `MojangContract` of `ServerApi.getMojangProxy()`, which rotates IPv6 source addresses and throws until `INET6_NETWORK_PREFIX` has been registered.
- `HypixelController` - Hypixel API proxy endpoints under `/hypixel/`. Provides player data, guild lookups (by ID, name, or player), online status, player counts, punishment statistics, and game information. Delegates to the `HypixelContract` of `ServerApi.getHypixelClient()`.
- `SkyBlockController` - SkyBlock API proxy endpoints under `/skyblock/`. Provides profiles, auctions (by ID, profile, or player), active/ended auction listings, bazaar products, museum, garden, news, and fire sales. Delegates to the `HypixelContract` of `ServerApi.getHypixelClient()`.
- `ResourceController` - SkyBlock resource proxy endpoints under `/resources/`. Provides skill definitions, collection definitions, item definitions, and election data. None needs a Hypixel API key upstream. Delegates to the `HypixelContract` of `ServerApi.getHypixelClient()`.

### Dependencies

Read `build.gradle.kts` for the pinned commits.
- **`spring-framework`** (`com.github.simplified-dev`) - Reusable Spring server framework (API versioning, API key auth with Bucket4j rate limiting, error handling, config). Its `api` scope carries the Spring Boot web, security, and actuator starters, Bucket4j, Gson, the Log4j2 API, `client`, and `gson-extras`.
- **`client`**, **`gson-extras`**, **`manager`** (`com.github.simplified-dev`) - The Feign-based `Client` and `Proxy` with subnet rotation, `GsonSettings`, and `KeyManager` that `ServerApi` builds on.
- **`hypixel`**, **`mojang`**, **`skyblock`** (`com.github.simplified-api`) and **`api`** (`com.github.skyblock-simplified`) - The Hypixel and Mojang contracts, the SkyBlock data module, and the SkyBlock Simplified `SimplifiedContract`.
- **`springdoc-openapi-starter-webmvc-scalar`** - OpenAPI spec generation and Scalar UI, declared here because `spring-framework` holds SpringDoc `compileOnly`.

### Configuration

All properties are managed programmatically through `ServerConfig` (from `spring-framework`):
- `ServerConfig.builder()` - Full control with Spring Boot defaults
- `ServerConfig.optimized()` - Production preset (virtual threads, compression, graceful shutdown, reverse proxy support)
- `api.key.authentication.enabled` - Toggles API key security (default `true` in `ServerConfig`); see [API Key Store](#api-key-store) for what `true` requires
- `springdocEnabled` - Toggles SpringDoc OpenAPI spec generation and Scalar UI (default `true` in `ServerConfig`)

### API Documentation (SpringDoc + Scalar)

- **Dependency:** `springdoc-openapi-starter-webmvc-scalar` (version in `gradle/libs.versions.toml`)
- **Root path:** `GET /` redirects to the Scalar UI (`springdoc.use-root-path=true`)
- **OpenAPI spec:** `GET /v3/api-docs` returns the auto-generated OpenAPI 3.0 JSON
- **Jackson coexistence:** Jackson auto-configuration is enabled (not excluded) so SpringDoc can use it internally. Gson remains the primary HTTP serializer because `GsonHttpMessageConverter` is registered first by `ServerWebConfig`
- **Controller annotations:** `@Tag` (class-level) and `@Operation` (method-level) from `io.swagger.v3.oas.annotations` enrich the generated documentation
