# Persistence (DynamoDB)

All DynamoDB code lives in `infrastructure/`:

- `config/DynamoDbConfig.java` — builds `DynamoDbClient`, `DynamoDbEnhancedClient` and the `DynamoDbTable<UrlItem>` bean. When `dynamodb.create-table=true` it creates the table and GSI on startup if they don't exist (and waits until the table is active).
- `repository/UrlItem.java` — `@DynamoDbBean` mapped to the table.
- `repository/UrlRepository.java` — implements `UrlRepositoryPort`.

## Table `urls`

| Attribute | Role |
|---|---|
| `shortUrl` | Partition key (the 7-char code) |
| `originalUrl` | Target URL |
| `originalUrlHash` | SHA-256 hex of `originalUrl`; partition key of GSI `originalUrlHash-index` (projection ALL) |

Billing mode is on-demand (default of the Enhanced Client `createTable`).

## Operations

| Port method | DynamoDB call |
|---|---|
| `findByShortUrl` | `GetItem` by `shortUrl` |
| `findByOriginalUrl` | `Query` on `originalUrlHash-index`, then filters by exact `originalUrl` to discard SHA-256 collisions |
| `save` | `PutItem` with condition `attribute_not_exists(shortUrl)` |

## Design notes

- The GSI is keyed by a hash instead of the raw URL because index key values are limited to 2048 bytes and URLs can be longer.
- GSIs are eventually consistent: two concurrent requests for the same new URL may each create a code. Both codes redirect correctly, so this is accepted.
- The domain `Url.id` is set to the short code; there is no separate generated id.

## Inspecting the local table

```sh
docker run --rm -e AWS_ACCESS_KEY_ID=local -e AWS_SECRET_ACCESS_KEY=local -e AWS_DEFAULT_REGION=us-east-1 \
  amazon/aws-cli dynamodb scan --table-name urls --endpoint-url http://host.docker.internal:8042
```
