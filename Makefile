.PHONY: up down deps build run restart dev scale logs-lb

N ?= 3

# Build the API image and start DynamoDB Local, the API replicas and nginx
up:
	docker compose up -d --build

# Stop containers
down:
	docker compose down

# Refresh Gradle dependencies
deps:
	./gradlew --refresh-dependencies

# Rebuild the project (skips tests)
build:
	./gradlew clean build -x test

# Run the app in the foreground
run:
	./gradlew bootRun

# Stop containers and bring them back up fresh
restart: down up

# Full cycle: restart docker, refresh deps, rebuild and run
dev: restart deps build run

# Change the number of API replicas without restarting the others (make scale N=5)
scale:
	docker compose up -d --no-recreate --scale url-shortener-api=$(N)

# Follow nginx access logs, showing which replica served each request
logs-lb:
	docker compose logs -f url-shortener-nginx
