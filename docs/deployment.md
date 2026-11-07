# Local deployment: replicas behind nginx

```
client → :${APP_PORT:-8080} → url-shortener-nginx (round-robin)
                                   → url-shortener-api × ${API_REPLICAS:-3}
                                        ├─ url-shortener-redis     (read cache, optional)
                                        └─ url-shortener-dynamodb  (source of truth)
```

The API can run as many replicas as needed because instances share no state: uniqueness of codes and of original URLs is enforced by DynamoDB (see [persistence.md](persistence.md)).

## Commands

```sh
APP_PORT=8090 make up                 # build + start everything (3 replicas)
API_REPLICAS=5 APP_PORT=8090 make up  # start with a different replica count
make scale N=5                        # change replicas live; existing containers (and nginx) are not recreated
make logs-lb                          # nginx access log: request, status, replica, time
docker compose ps                     # replicas show (healthy) once Spring is up
docker exec url-shortener-redis redis-cli info stats | grep keyspace   # cache hits / misses
docker exec url-shortener-redis redis-cli dbsize                       # cached keys (2 per URL)
```

## Services

- **`url-shortener-api`**: no `container_name` or published port (both would prevent replicas); only `expose: 8080` on the compose network. Healthcheck: `curl -fs http://localhost:8080/actuator/health` (`curl` ships with the `eclipse-temurin` JRE image; Actuator exposes only `health`). All replicas start at once and race to create the tables; `DynamoDbConfig` handles that.
- **`url-shortener-nginx`** (`nginx:1.28.0-alpine`, config in `nginx/nginx.conf`, mounted read-only): waits for the API to be healthy, proxies only `location /shortener` and returns 404 for everything else.
- **`url-shortener-redis`** (`redis:7.4.1-alpine`, port 6379 published for `bootRun`): read cache only, so no persistence (`--save "" --appendonly no`) and `--maxmemory 128mb --maxmemory-policy allkeys-lru`. The API depends on it with `service_started`, not `service_healthy`: replicas work without it (reads fall back to DynamoDB). Measured: with Redis stopped, all requests succeeded and replicas stayed healthy; after restarting it, each replica resumed using the cache within seconds. See [persistence.md](persistence.md#read-cache-redis).

## nginx configuration

- **Service discovery:** `server url-shortener-api:8080 resolve` with `resolver 127.0.0.11 valid=10s` (Docker's DNS returns one IP per replica). The `resolve` parameter needs a shared-memory `zone` and nginx ≥ 1.27.3. nginx re-resolves every 10 s, so scaled or restarted replicas are picked up without reloading.
- **Failover:** a stopped replica stays in nginx's list until the next DNS refresh, and connecting to its IP hangs instead of being refused. `proxy_connect_timeout 2s` (default 60 s) plus `proxy_next_upstream error timeout` and `proxy_next_upstream_tries 3` move the request to another replica. Measured: with a replica stopped, every request succeeds, worst case ~2 s. With the default timeout a request hung for 60 s and returned 502.
- **POST safety:** nginx never retries a non-idempotent request once it was sent to a replica; it only retries when the connection itself failed, so a POST cannot be applied twice by the balancer. Even if it were, deduplication would return the same code.
- **No retry on 503:** a 503 from the API is a business answer (`ShortUrlGenerationException`), not a broken replica.
- **Log format:** `$upstream_addr` and `$upstream_response_time` per request, to check the distribution.

Changing `nginx/nginx.conf` does not need a restart: `docker exec url-shortener-nginx nginx -t && docker exec url-shortener-nginx nginx -s reload`.
