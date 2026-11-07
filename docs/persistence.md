# Persistence (DynamoDB)

All DynamoDB code lives in `infrastructure/`:

- `config/DynamoDbConfig.java` — builds `DynamoDbClient`, `DynamoDbEnhancedClient` and one `DynamoDbTable` bean per table. When `dynamodb.create-table=true` it creates missing tables on startup and waits until they are `ACTIVE`. Safe with several instances starting at once: a `ResourceInUseException` (another instance created the table first) is ignored, and the wait also covers a table that is still `CREATING`.
- `repository/UrlItem.java`, `repository/OriginalUrlItem.java`, `repository/CounterItem.java` — `@DynamoDbBean`s, one per table.
- `repository/UrlRepository.java` — implements `UrlRepositoryPort`.
- `repository/DynamoDbBlockSource.java` — the range coordinator used by `generator/RangeAllocator`.

## Tables

| Table | Partition key | Other attributes | Purpose |
|---|---|---|---|
| `urls` | `shortUrl` (the 7-char code) | `originalUrl` | Redirects |
| `original_urls` | `originalUrlHash` (SHA-256 hex of the original URL) | `originalUrl`, `shortUrl` | Uniqueness of the original URL and lookup by it |
| `counters` | `name` (`"shortUrl"`) | `next` (number) | Range coordinator: next number not yet handed out |

`urls` and `original_urls` always hold the same pairs: they are only written together, in one transaction. Billing mode is on-demand (default of the Enhanced Client `createTable`).

## Range coordinator

Each instance reserves a block of `shortener.block-size` numbers (default 1000) when it needs one:

```
UpdateItem counters{name: "shortUrl"}  ADD next :1000  ReturnValues UPDATED_NEW   → next = 4000
                                                                                     → block [3000, 4000)
```

`ADD` is atomic, so concurrent reservations from any instance always get disjoint blocks; if the item does not exist yet, `ADD` creates it from 0. The instance then serves numbers from memory (`RangeAllocator`) and only calls DynamoDB again when the block runs out: one write per 1000 codes per instance. Numbers left in a block when an instance stops are never used (gaps, not duplicates).

## Operations

| Port method | DynamoDB call |
|---|---|
| `findByShortUrl` | `GetItem` on `urls` with `consistentRead(true)`, so a code is found right after any instance created it |
| `findByOriginalUrl` | `GetItem` on `original_urls` with `consistentRead(true)`, then checks the stored `originalUrl` matches exactly (discards SHA-256 collisions) |
| `save` | `TransactWriteItems` with two puts, each conditioned on `attribute_not_exists` of its key |

If the transaction is cancelled because a condition failed (the URL already has a code, or the code is taken) or because of a `TransactionConflict` with a concurrent transaction, the repository throws the domain `UrlWriteConflictException`. `UrlShortenUseCase` then repeats the lookup and, if the URL is still missing, retries with the next code (up to 5 attempts, then 503). A failed condition on `urls` means a code was repeated, which coordinated ranges should make impossible, so it is logged as `WARN`.

## Design notes

- **Why a second table instead of a GSI:** GSIs only support eventually consistent reads, and a lookup followed by a write is not atomic. With a GSI, two instances shortening the same new URL at once both created a code (reproduced). A base table key gives a conditional write to enforce uniqueness and a strongly consistent read to look it up.
- The original URL is keyed by its hash because key values are limited to 2048 bytes and URLs can be longer.
- Code uniqueness comes from the coordinator (disjoint blocks) plus a bijective permutation (distinct numbers → distinct codes). The conditional put on `urls` is a safety net against misconfiguration, e.g. a changed `shortener.code-secret` or codes from an older generator still in the table: instead of overwriting, the save fails and the next number is used.
- Cost: shortening a new URL is one strongly consistent read plus a two-item transaction (transactional writes cost twice a normal write). Every redirect is one strongly consistent read (twice the cost of an eventually consistent one); if redirect volume makes this expensive, a cache in front of `findByShortUrl` is cheaper than going back to eventual reads.
- In production, prefer `dynamodb.create-table=false` and creating the tables with infrastructure as code; startup creation is meant for local development.
- The domain `Url.id` is set to the short code; there is no separate generated id.

## Inspecting the local tables

```sh
docker run --rm -e AWS_ACCESS_KEY_ID=local -e AWS_SECRET_ACCESS_KEY=local -e AWS_DEFAULT_REGION=us-east-1 \
  amazon/aws-cli dynamodb scan --table-name urls --endpoint-url http://host.docker.internal:8042
```

Use `--table-name original_urls` or `--table-name counters` for the other tables.
