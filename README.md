# Simplified Server

SkyBlock-specific Spring Boot REST server for the
[SkyBlock Simplified](https://github.com/SkyBlock-Simplified) ecosystem,
providing proxy endpoints for the Mojang API, Hypixel API, SkyBlock API, and
SkyBlock resource definitions. Built on top of the
[spring-framework](https://github.com/simplified-dev/spring-framework) server framework.

## Table of Contents

- [Features](#features)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
- [API Endpoints](#api-endpoints)
  - [Mojang](#mojang)
  - [Hypixel](#hypixel)
  - [SkyBlock](#skyblock)
  - [Resources](#resources)
- [Architecture](#architecture)
  - [Entry Point](#entry-point)
  - [Controllers](#controllers)
  - [API Documentation](#api-documentation)
- [Project Structure](#project-structure)
- [Dependencies](#dependencies)
- [Contributing](#contributing)
- [License](#license)

## Features

- **Mojang API proxy** - Player profile lookups, username resolution, skin
  properties, and bulk username lookups with automatic IPv6 rotation to avoid
  rate limits
- **Hypixel API proxy** - Player data, guild lookups, online status, punishment
  statistics, player counts, and game information
- **SkyBlock API proxy** - Profiles, auctions, bazaar, museum, garden, news,
  fire sales, and ended auctions
- **SkyBlock resource proxy** - Skills, collections, items, and election data
  (no API key required)
- **OpenAPI documentation** - Auto-generated API spec at `/v3/api-docs` with
  Scalar UI at the root path
- **Production-ready** - Virtual threads, response compression, and graceful
  shutdown via `ServerConfig.optimized()`, with API key authentication, rate
  limiting, and security headers from spring-framework

## Getting Started

### Prerequisites

| Requirement | Version | Notes |
|-------------|---------|-------|
| [JDK](https://adoptium.net/) | **21+** | Required (virtual threads) |
| [Gradle](https://gradle.org/) | **9.4+** | Included via wrapper (`./gradlew`) |

**Required environment variables** (for upstream API calls):

```
HYPIXEL_API_KEY         # Hypixel API key (required for most Hypixel/SkyBlock endpoints)
DATABASE_HOST           # MariaDB host
DATABASE_SCHEMA         # MariaDB schema
DATABASE_PORT           # MariaDB port
DATABASE_USER           # MariaDB user
DATABASE_PASSWORD       # MariaDB password
```

### Installation

This module depends on the
[spring-framework](https://github.com/simplified-dev/spring-framework) server
framework, the [hypixel](https://github.com/simplified-api/hypixel),
[mojang](https://github.com/simplified-api/mojang), and
[skyblock](https://github.com/simplified-api/skyblock) API modules, the
SkyBlock Simplified [api](https://github.com/skyblock-simplified/api) module,
and the [client](https://github.com/simplified-dev/client),
[gson-extras](https://github.com/simplified-dev/gson-extras), and
[manager](https://github.com/simplified-dev/manager) libraries, each declared
in `build.gradle.kts` as a JitPack coordinate pinned to a commit. A standalone
build resolves them from JitPack:

```bash
git clone https://github.com/SkyBlock-Simplified/server.git
cd server
```

To build against local checkouts of those modules instead, include them in a
Gradle composite build whose `dependencySubstitution` maps each coordinate onto
the local project.

Build the server:

```bash
./gradlew build
```

Run tests:

```bash
./gradlew test
```

Run the server:

```bash
java -jar build/libs/server-0.1.0.jar
```

> [!IMPORTANT]
> `ServerConfig.optimized()` turns API key authentication on, so
> spring-framework's `ApiKeySecurityConfig` requires an `ApiKeyStore` bean and
> startup fails without one. This repository commits none: supply an
> `ApiKeyStore` `@Bean` (`.gitignore` excludes
> `src/main/java/dev/sbs/server/config/LocalApiKeyStoreConfig.java` for a local
> one), or run with `--api.key.authentication.enabled=false` (or the
> `API_KEY_AUTHENTICATION_ENABLED=false` environment variable) to get
> `PermitAllSecurityConfig`, which permits every request.

<details>
<summary>Using as a dependency in another Gradle project</summary>

**JitPack** (for snapshot builds):

```kotlin
repositories {
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation("com.github.SkyBlock-Simplified:server:master-SNAPSHOT")
}
```

**Composite build** (for local development):

```kotlin
// settings.gradle.kts
includeBuild("../server")

// build.gradle.kts
dependencies {
    implementation("dev.sbs:server:0.1.0")
}
```

</details>

## API Endpoints

While API key authentication is on, every endpoint below requires a valid
`X-API-Key` header. "No API key required" below means the upstream Hypixel
endpoint needs no Hypixel API key.

### Mojang

Proxy endpoints under `/mojang/` with automatic IPv6 rotation to avoid rate
limits.

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/mojang/user/{identifier}` | Lookup player profile by username or UUID |
| `GET` | `/mojang/username/{username}` | Resolve case-corrected username and UUID |
| `GET` | `/mojang/uuid/{uniqueId}` | Resolve username by UUID |
| `GET` | `/mojang/properties/{uniqueId}` | Fetch signed skin/cape texture data |
| `POST` | `/mojang/bulk` | Bulk username lookup (up to 10 usernames) |

### Hypixel

Proxy endpoints under `/hypixel/` for Hypixel network data.

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/hypixel/player/{playerId}` | Fetch player data and game stats |
| `GET` | `/hypixel/guild/id/{guildId}` | Fetch guild by ID |
| `GET` | `/hypixel/guild/name/{guildName}` | Fetch guild by name |
| `GET` | `/hypixel/guild/player/{playerId}` | Fetch guild by player |
| `GET` | `/hypixel/status/{playerId}` | Fetch player online status |
| `GET` | `/hypixel/counts` | Fetch current player counts |
| `GET` | `/hypixel/punishmentstats` | Fetch punishment statistics |
| `GET` | `/hypixel/games` | Fetch game information (no API key required) |

### SkyBlock

Proxy endpoints under `/skyblock/` for SkyBlock-specific data.

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/skyblock/profiles/{playerId}` | Fetch all SkyBlock profiles for a player |
| `GET` | `/skyblock/museum/{islandId}` | Fetch museum data for a profile |
| `GET` | `/skyblock/garden/{islandId}` | Fetch garden data for a profile |
| `GET` | `/skyblock/news` | Fetch SkyBlock news and announcements |
| `GET` | `/skyblock/bazaar` | Fetch all Bazaar products (no API key required) |
| `GET` | `/skyblock/auction/id/{auctionId}` | Fetch auction by ID |
| `GET` | `/skyblock/auction/profile/{islandId}` | Fetch auctions by profile |
| `GET` | `/skyblock/auction/player/{playerId}` | Fetch auctions by player |
| `GET` | `/skyblock/auctions` | Fetch active auctions, first page (no API key required) |
| `GET` | `/skyblock/auctions/{page}` | Fetch active auctions by page (no API key required) |
| `GET` | `/skyblock/auctions/ended` | Fetch recently ended auctions (no API key required) |
| `GET` | `/skyblock/firesales` | Fetch active/upcoming Fire Sales (no API key required) |

### Resources

Proxy endpoints under `/resources/` for SkyBlock resource definitions. None of
these endpoints require an API key.

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/resources/skills` | Fetch skill definitions and leveling data |
| `GET` | `/resources/collections` | Fetch collection definitions and tier data |
| `GET` | `/resources/items` | Fetch item definitions |
| `GET` | `/resources/election` | Fetch current mayor and election data |

## Architecture

### Entry Point

`SimplifiedServer` is the Spring Boot application class. It provides a `Gson`
bean via `ServerApi.getGson()` that spring-framework's `ServerWebConfig` picks
up for its Gson message converter. Jackson auto-configuration remains enabled
so SpringDoc can use it internally for OpenAPI spec generation. `main()`
registers the Hypixel API key and the Mojang proxy's IPv6 prefix from the
environment, and `ServerConfig.optimized()` supplies all default properties
programmatically.

```java
@SpringBootApplication(scanBasePackages = { "dev.sbs.server", "dev.simplified.serverapi" })
public class SimplifiedServer {

    @Bean
    public @NotNull Gson gson() {
        return ServerApi.getGson();
    }

    public static void main(String[] args) {
        ServerApi.getKeyManager().add("HYPIXEL_API_KEY", SystemUtil.getEnv("HYPIXEL_API_KEY"));
        SystemUtil.getEnv("INET6_NETWORK_PREFIX").ifPresent(ServerApi::setInet6NetworkPrefix);
        SpringApplication application = new SpringApplication(SimplifiedServer.class);
        application.setDefaultProperties(
            ServerConfig.optimized()
                .build()
                .toProperties()
        );
        application.run(args);
    }

}
```

> [!IMPORTANT]
> The `scanBasePackages` array must include `dev.simplified.serverapi` for
> Spring to discover spring-framework's security, error handling, API
> versioning, and web configuration.

### Controllers

All controllers delegate to the upstream Feign contracts that `ServerApi`
holds, and each carries a class-level `@PreAuthorize("isAuthenticated()")`:

| Controller | Base Path | Upstream Client |
|------------|-----------|-----------------|
| `MojangController` | `/mojang/` | `MojangContract` via `ServerApi.getMojangProxy()` (IPv6 rotation) |
| `HypixelController` | `/hypixel/` | `HypixelContract` via `ServerApi.getHypixelClient()` |
| `SkyBlockController` | `/skyblock/` | `HypixelContract` via `ServerApi.getHypixelClient()` |
| `ResourceController` | `/resources/` | `HypixelContract` via `ServerApi.getHypixelClient()` |

### API Documentation

- **Root path:** `GET /` redirects to the Scalar UI (`springdoc.use-root-path=true`)
- **OpenAPI spec:** `GET /v3/api-docs` returns the auto-generated OpenAPI 3.0 JSON
- **Controller annotations:** `@Tag` (class-level) and `@Operation` (method-level)
  from `io.swagger.v3.oas.annotations` enrich the generated documentation
- **Jackson coexistence:** Jackson auto-configuration is enabled (not excluded)
  so SpringDoc can use it internally. Gson remains the primary HTTP serializer
  because `GsonHttpMessageConverter` is registered first by `ServerWebConfig`

## Project Structure

```
server/
├── src/main/java/dev/sbs/server/
│   ├── SimplifiedServer.java          # Spring Boot entry point with Gson bean
│   ├── ServerApi.java                 # Gson, KeyManager, and the upstream clients
│   ├── config/
│   │   └── OpenApiConfig.java         # OpenAPI metadata (title, description, version)
│   └── controller/
│       ├── MojangController.java      # Mojang API proxy (/mojang/)
│       ├── HypixelController.java     # Hypixel API proxy (/hypixel/)
│       ├── SkyBlockController.java    # SkyBlock API proxy (/skyblock/)
│       └── ResourceController.java    # SkyBlock resources (/resources/)
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/libs.versions.toml          # Version catalog
```

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| spring-framework | JitPack commit | Spring server framework (versioning, API key auth, rate limiting, error handling, config) |
| client | JitPack commit | Feign-based `Client` and `Proxy` with IPv6 subnet rotation |
| gson-extras | JitPack commit | `GsonSettings` behind the shared `Gson` |
| manager | JitPack commit | `KeyManager` holding the Hypixel API key |
| hypixel | JitPack commit | Hypixel API contract and response models |
| mojang | JitPack commit | Mojang API contract and response models |
| skyblock | JitPack commit | SkyBlock data module |
| api | JitPack commit | SkyBlock Simplified API contract |
| SpringDoc OpenAPI Scalar | 3.0.3 | OpenAPI spec generation and Scalar UI |
| simplified-annotations | 2.6.1 | Annotation processing (`@Getter`, `@NoArgsConstructor`) |
| JUnit 5 | 5.11.4 | Testing |
| Hamcrest | 2.2 | Test matchers |

`build.gradle.kts` pins each JitPack dependency to a commit.

> [!NOTE]
> `spring-framework` exports the Spring Boot web, security, and actuator
> starters, Bucket4j, Gson, and the Log4j2 API through its `api` scope - no
> need to declare them separately.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for development setup, code style
guidelines, and how to submit a pull request.

## License

This project is licensed under the **Apache License 2.0** - see
[LICENSE.md](LICENSE.md) for the full text.
