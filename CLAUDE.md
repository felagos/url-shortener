# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

URL shortener REST API: Spring Boot 3.4, Java 21, Gradle (Kotlin DSL), persistence on DynamoDB (AWS SDK v2 Enhanced Client). Base package: `com.shortener.url_shortener`.

## Commands

```sh
./gradlew build -x test          # compile + jar
./gradlew test                   # all tests
./gradlew test --tests "com.shortener.url_shortener.UrlShortenerApplicationTests"   # single class
./gradlew test --tests "*UrlShortenerApplicationTests.contextLoads"                 # single method
./gradlew bootRun                # run locally (needs DynamoDB Local on :8042)
```

`Makefile` wraps the usual flow: `make up` (build API image + start compose), `make down`, `make restart`, `make deps` (`--refresh-dependencies`), `make build`, `make run`, `make dev` (restart + deps + build + run).

No linter is configured.

## Local environment

- `docker-compose.yml` runs DynamoDB Local (host port **8042**, `dynamodb_data` volume), Redis (host port 6379, cache only, no persistence), `${API_REPLICAS:-3}` replicas of `url-shortener-api` (two-stage `Dockerfile`) and `url-shortener-nginx` as the load balancer and only entry point, on `APP_PORT` (default 8080). Replicas have no published port. Details (nginx config, scaling, failover): [docs/deployment.md](docs/deployment.md).
- On this machine 8000, 8001 and 8080 are often taken by other projects' containers, so use e.g. `APP_PORT=8090 make up`.
- `make scale N=5` changes the replica count live; nginx picks up the new replicas without a restart. `make logs-lb` shows which replica served each request.
- Running with `bootRun` uses `application.properties` (`dynamodb.endpoint=http://localhost:8042`, Redis on `localhost:6379`). Inside compose the API reaches DynamoDB at `http://url-shortener-dynamodb:8000` and Redis at `url-shortener-redis` via env vars (`DYNAMODB_ENDPOINT`, `DYNAMODB_REGION`, `DYNAMODB_CREATETABLE`, `SPRING_DATA_REDIS_HOST`) that override the properties through Spring relaxed binding.
- `/actuator/health` is exposed only for the compose healthcheck; nginx proxies just `/shortener`, so it is not reachable from outside.
- For real AWS, leave `dynamodb.endpoint` empty: the client then uses the default AWS credential chain instead of the dummy `local` credentials.

## Architecture

Hexagonal (ports & adapters):

- `domain/` — `Url` model, `BaseException` (carries an HTTP status code) and the ports: `UrlRepositoryPort`, `ShortCodeGeneratorPort`, `ConfigurationPort`. No framework dependencies.
- `application/` — one `@Service` per use case (`UrlShortenUseCase`, `GetUrlUseCase`), depending only on ports.
- `infrastructure/` — adapters: REST controller, DynamoDB repositories, short-code generation (`generator/`), Spring config.

Swapping a technology (e.g. the Mongo → DynamoDB migration) should only touch `infrastructure/`; keep use cases and ports unchanged.

### Request flow

- `POST /shortener` `{"url": ...}` → `UrlShortenUseCase.shortenUrl`, in a loop of up to `MAX_ATTEMPTS` (5): look up the original URL (strongly consistent) and return its code if stored; otherwise get a new code from `ShortCodeGeneratorPort` and save it. If the save conflicts (another instance shortened the same URL first, or the code is taken), the repository throws `UrlWriteConflictException` and the loop starts over, so the next lookup returns the winner's code. After 5 conflicts it fails with `ShortUrlGenerationException` (503). Returns `{domain-shortener}/{code}` as plain text.
- **Code generation is coordinated by ranges** (`infrastructure/generator/`): `RangeAllocator` reserves blocks of `shortener.block-size` numbers from a shared counter (`DynamoDbBlockSource`, atomic `ADD` on the `counters` table) and serves them from memory, so instances never produce the same number. `RangeShortCodeGenerator` scrambles each number with `FeistelPermutation` (keyed bijection on [0, 62⁷)) and encodes it as 7 base62 chars, so codes are unique but not sequential.
- `shortener.code-secret` (env `SHORTENER_CODESECRET`) is the permutation key: it must be identical on every instance and **must never change** once codes exist, or new codes can repeat old ones.
- The conditional writes stay as a safety net: a conflict on the `urls` condition should never happen and is logged as `WARN`. Don't remove them, and don't reintroduce check-then-act without a conditional write or GSI lookups for uniqueness.
- `GET /shortener/{hash}` → `GetUrlUseCase` → 301 redirect to the original URL. Missing hash throws `UrlNotFoundException`, which `GlobalExceptionHandler` maps to its status code (404) with the message as body.

### Persistence

Two tables written together in one `TransactWriteItems`: `urls` (short code → original URL, for redirects) and `original_urls` (SHA-256 of the original URL → short code, for deduplication). A third table, `counters`, holds the range coordinator's counter. Details, design rationale and how to inspect them: [docs/persistence.md](docs/persistence.md).

Reads go through a Redis cache: `CachingUrlRepository` (`@Primary`) decorates the DynamoDB `UrlRepository`, so use cases are unaware of it. Rules that keep it correct:
- Only safe because both mappings are immutable. If update/delete of URLs is ever added, invalidate the cache there.
- Never cache absences (a missing code may be created a moment later by another instance), and only cache after a successful `save`.
- Redis is never a source of truth: uniqueness (conditional transaction) and the range counter stay in DynamoDB.
- Fail-open: Redis errors fall back to DynamoDB and Redis is skipped for 5 s; `management.health.redis.enabled=false` so Redis can't make replicas unhealthy.

## Gotchas

- `ShortenUrlDto`'s URL regex rejects query strings (`?q=...`) and hosts without a subdomain (`https://google.com`); both return 400.
- Numbers left in a block when an instance stops are never used: the counter has gaps by design, never duplicates. Each reservation is logged as `Reserved short code block [start, end)`.
- `UrlShortenerApplicationTests.contextLoads` runs with `dynamodb.create-table=false` so it doesn't need DynamoDB running (blocks are reserved lazily, on the first code) or Redis (Lettuce connects on first use); the other tests are plain unit tests.
