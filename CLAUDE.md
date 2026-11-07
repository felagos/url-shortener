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

- `domain/` — `Url` model, `BaseException` (carries an HTTP status code) and the ports: `UrlRepositoryPort`, `HashingServicePort`, `IdGeneratorPort`, `ConfigurationPort`. No framework dependencies.
- `application/` — one `@Service` per use case (`UrlShortenUseCase`, `GetUrlUseCase`), depending only on ports.
- `infrastructure/` — adapters: REST controller, DynamoDB repository, Snowflake ID generator, hashing, Spring config.

Swapping a technology (e.g. the Mongo → DynamoDB migration) should only touch `infrastructure/`; keep use cases and ports unchanged.

### Request flow

- `POST /shortener` `{"url": ...}` → `UrlShortenUseCase.shortenUrl`: returns the existing short code if the original URL is already stored; otherwise `IdGenerator` (Snowflake, worker id 1) produces a numeric id, `HashingServiceAdapter` turns it into a 7-char Base62 code, saves it, and returns `{domain-shortener}/{code}` as plain text.
- `GET /shortener/{hash}` → `GetUrlUseCase` → 301 redirect to the original URL. Missing hash throws `UrlNotFoundException`, which `GlobalExceptionHandler` maps to its status code (404) with the message as body.

### Persistence

Single table `urls` keyed by the short code, plus a GSI on a SHA-256 of the original URL used for deduplication. Details, design rationale and how to inspect the table: [docs/persistence.md](docs/persistence.md).

## Gotchas

- `ShortenUrlDto`'s URL regex rejects query strings (`?q=...`) and hosts without a subdomain (`https://google.com`); both return 400.
- `Base62` truncates to 7 characters, so different ids can collide. `UrlRepository.save` uses `attribute_not_exists(shortUrl)`, so a collision surfaces as a `ConditionalCheckFailedException` (currently unhandled → 500) instead of overwriting.
- `HashingFactory` is a hand-rolled singleton, not a Spring bean; `HashingType.UNIQUE_ID` (random NanoId) exists but is unused.
- The only test is `contextLoads`, run with `dynamodb.create-table=false` so it doesn't need DynamoDB running.
