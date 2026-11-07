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

- `docker-compose.yml` runs `url-shortener-dynamodb` (DynamoDB Local, host port **8042**, data persisted in the `dynamodb_data` volume) and `url-shortener-api` (built from the two-stage `Dockerfile`).
- The API's host port comes from `APP_PORT` (default 8080). On this machine 8000, 8001 and 8080 are often taken by other projects' containers, so use e.g. `APP_PORT=8090 make up`.
- Running with `bootRun` uses `application.properties` (`dynamodb.endpoint=http://localhost:8042`). Inside compose the API reaches DynamoDB at `http://url-shortener-dynamodb:8000` via env vars (`DYNAMODB_ENDPOINT`, `DYNAMODB_REGION`, `DYNAMODB_CREATETABLE`) that override the properties through Spring relaxed binding.
- For real AWS, leave `dynamodb.endpoint` empty: the client then uses the default AWS credential chain instead of the dummy `local` credentials.

## Architecture

Hexagonal (ports & adapters):

- `domain/` — `Url` model, `BaseException` (carries an HTTP status code) and the ports: `UrlRepositoryPort`, `HashingServicePort`, `ConfigurationPort`. No framework dependencies.
- `application/` — one `@Service` per use case (`UrlShortenUseCase`, `GetUrlUseCase`), depending only on ports.
- `infrastructure/` — adapters: REST controller, DynamoDB repository, short-code generation (`hashing/`), Spring config.

Swapping a technology (e.g. the Mongo → DynamoDB migration) should only touch `infrastructure/`; keep use cases and ports unchanged.

### Request flow

- `POST /shortener` `{"url": ...}` → `UrlShortenUseCase.shortenUrl`: returns the existing short code if the original URL is already stored; otherwise `HashingServiceAdapter` generates a random 7-char base62 code (`UniqueId`, NanoId) and saves it. If the code is taken, the repository throws `ShortUrlAlreadyExistsException` and the use case retries with a new code up to `MAX_ATTEMPTS` (5), then fails with `ShortUrlGenerationException` (503). Returns `{domain-shortener}/{code}` as plain text.
- Codes are random and uniqueness is enforced by DynamoDB's conditional put, so instances share no state and the API scales horizontally without coordination. Don't reintroduce counters, Snowflake worker ids, or anything derived from per-instance state.
- `GET /shortener/{hash}` → `GetUrlUseCase` → 301 redirect to the original URL. Missing hash throws `UrlNotFoundException`, which `GlobalExceptionHandler` maps to its status code (404) with the message as body.

### Persistence

Single table `urls` keyed by the short code, plus a GSI on a SHA-256 of the original URL used for deduplication. Details, design rationale and how to inspect the table: [docs/persistence.md](docs/persistence.md).

## Gotchas

- `ShortenUrlDto`'s URL regex rejects query strings (`?q=...`) and hosts without a subdomain (`https://google.com`); both return 400.
- `HashingServicePort.hash(input)` ignores its input: codes are random, not derived from the URL.
- `HashingFactory` is a hand-rolled singleton, not a Spring bean.
- `UrlShortenerApplicationTests.contextLoads` runs with `dynamodb.create-table=false` so it doesn't need DynamoDB running; the other tests are plain unit tests with Mockito.
