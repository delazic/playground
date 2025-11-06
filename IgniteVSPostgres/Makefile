.PHONY: help start stop restart logs clean db-shell db-reset build test run run-all run-create-plan run-read-plan run-update-plan run-delete-plan run-all-plan run-create-member run-read-member run-update-member run-delete-member run-all-member

# Default target
help:
	@echo "PBM System - Development Commands"
	@echo "=================================="
	@echo ""
	@echo "Docker Services:"
	@echo "  make start             - Start all services (PostgreSQL, Redis, pgAdmin)"
	@echo "  make stop              - Stop all services"
	@echo "  make restart           - Restart all services"
	@echo "  make logs              - View logs from all services"
	@echo "  make logs-db           - View PostgreSQL logs"
	@echo "  make logs-redis        - View Redis logs"
	@echo "  make clean             - Remove all containers and volumes"
	@echo ""
	@echo "Database:"
	@echo "  make db-shell          - Connect to PostgreSQL shell"
	@echo "  make db-reset          - Reset database (WARNING: deletes all data)"
	@echo "  make pgadmin           - Open pgAdmin in browser"
	@echo ""
	@echo "Build & Test:"
	@echo "  make build             - Build Java application"
	@echo "  make test              - Run tests"
	@echo ""
	@echo "Run Application (CRUD Operations):"
	@echo "  make run               - Run all CRUD operations for all entities"
	@echo "  make run-all           - Same as 'make run'"
	@echo ""
	@echo "Plan Operations:"
	@echo "  make run-create-plan   - CREATE: Insert benefit plans from CSV"
	@echo "  make run-read-plan     - READ: Display benefit plans"
	@echo "  make run-update-plan   - UPDATE: Update a sample plan"
	@echo "  make run-delete-plan   - DELETE: Delete a sample plan"
	@echo "  make run-all-plan      - Run all CRUD operations for plans"
	@echo ""
	@echo "Member Operations:"
	@echo "  make run-create-member - CREATE: Insert members from CSV"
	@echo "  make run-read-member   - READ: Display member count"
	@echo "  make run-update-member - UPDATE: Update a sample member"
	@echo "  make run-delete-member - DELETE: Delete a sample member"
	@echo "  make run-all-member    - Run all CRUD operations for members"

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
	mvn clean package -DskipTests
	@echo "✓ Build complete"

# Run tests
test:
	@echo "Running tests..."
	mvn test
	@echo "✓ Tests complete"

# Open pgAdmin
pgadmin:
	@echo "Opening pgAdmin..."
	@open http://localhost:5050 || xdg-open http://localhost:5050 || start http://localhost:5050

# ============================================================================
# Application Run Targets
# ============================================================================

# Run all CRUD operations for all entities
run: build
	@echo "Running all CRUD operations..."
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App"

run-all: run

# ============================================================================
# Plan CRUD Operations
# ============================================================================

# CREATE: Insert benefit plans from CSV
run-create-plan: build
	@echo "Running CREATE operation for Plans..."
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="CREATE PLAN"

# READ: Display benefit plans
run-read-plan: build
	@echo "Running READ operation for Plans..."
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="READ PLAN"

# UPDATE: Update a sample plan
run-update-plan: build
	@echo "Running UPDATE operation for Plans..."
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="UPDATE PLAN"

# DELETE: Delete a sample plan
run-delete-plan: build
	@echo "Running DELETE operation for Plans..."
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="DELETE PLAN"

# Run all CRUD operations for plans
run-all-plan: build
	@echo "Running all CRUD operations for Plans..."
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="ALL PLAN"

# ============================================================================
# Member CRUD Operations
# ============================================================================

# CREATE: Insert members from CSV
run-create-member: build
	@echo "Running CREATE operation for Members..."
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="CREATE MEMBER"

# READ: Display member count
run-read-member: build
	@echo "Running READ operation for Members..."
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="READ MEMBER"

# UPDATE: Update a sample member
run-update-member: build
	@echo "Running UPDATE operation for Members..."
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="UPDATE MEMBER"

# DELETE: Delete a sample member
run-delete-member: build
	@echo "Running DELETE operation for Members..."
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="DELETE MEMBER"

# Run all CRUD operations for members
run-all-member: build
	@echo "Running all CRUD operations for Members..."
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="ALL MEMBER"