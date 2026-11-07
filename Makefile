.PHONY: up down deps build run restart dev

# Build the API image and start it along with DynamoDB Local
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
