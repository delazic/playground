.PHONY: help start stop restart logs clean db-shell db-reset build test

# Default target
help:
	@echo "PBM System - Development Commands"
	@echo "=================================="
	@echo "make start       - Start all services (PostgreSQL, Redis, pgAdmin)"
	@echo "make stop        - Stop all services"
	@echo "make restart     - Restart all services"
	@echo "make logs        - View logs from all services"
	@echo "make logs-db     - View PostgreSQL logs"
	@echo "make logs-redis  - View Redis logs"
	@echo "make db-shell    - Connect to PostgreSQL shell"
	@echo "make db-reset    - Reset database (WARNING: deletes all data)"
	@echo "make clean       - Remove all containers and volumes"
	@echo "make build       - Build Java application"
	@echo "make test        - Run tests"
	@echo "make pgadmin     - Open pgAdmin in browser"

# Start all services
start:
	@echo "Starting PBM services..."
	docker-compose up -d
	@echo "Waiting for PostgreSQL to be ready..."
	@sleep 5
	@docker-compose exec -T postgres pg_isready -U pbm_user -d pbm_db
	@echo "✓ All services started successfully!"
	@echo ""
	@echo "PostgreSQL: localhost:5432"
	@echo "  Database: pbm_db"
	@echo "  User: pbm_user"
	@echo "  Password: pbm_password"
	@echo ""
	@echo "pgAdmin: http://localhost:5050"
	@echo "  Email: admin@pbm.local"
	@echo "  Password: admin"
	@echo ""
	@echo "Redis: localhost:6379"

# Stop all services
stop:
	@echo "Stopping PBM services..."
	docker-compose stop
	@echo "✓ All services stopped"

# Restart all services
restart: stop start

# View logs
logs:
	docker-compose logs -f

logs-db:
	docker-compose logs -f postgres

logs-redis:
	docker-compose logs -f redis

# Connect to PostgreSQL shell
db-shell:
	docker-compose exec postgres psql -U pbm_user -d pbm_db

# Reset database
db-reset:
	@echo "WARNING: This will delete all data!"
	@read -p "Are you sure? [y/N] " -n 1 -r; \
	echo; \
	if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		echo "Resetting database..."; \
		docker-compose exec -T postgres psql -U pbm_user -d pbm_db -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"; \
		docker-compose restart postgres; \
		sleep 5; \
		echo "✓ Database reset complete"; \
	fi

# Clean everything
clean:
	@echo "WARNING: This will remove all containers and volumes!"
	@read -p "Are you sure? [y/N] " -n 1 -r; \
	echo; \
	if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		echo "Cleaning up..."; \
		docker-compose down -v; \
		echo "✓ Cleanup complete"; \
	fi

# Build Java application
build:
	@echo "Building PBM application..."
	./mvnw clean package -DskipTests
	@echo "✓ Build complete"

# Run tests
test:
	@echo "Running tests..."
	./mvnw test
	@echo "✓ Tests complete"

# Open pgAdmin
pgadmin:
	@echo "Opening pgAdmin..."
	@open http://localhost:5050 || xdg-open http://localhost:5050 || start http://localhost:5050