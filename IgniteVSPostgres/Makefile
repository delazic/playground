.PHONY: help start stop restart logs clean db-shell db-reset build test run run-all run-create-plan run-read-plan run-update-plan run-delete-plan run-all-plan run-create-member run-read-member run-update-member run-delete-member run-all-member run-create-enrollment run-read-enrollment run-update-enrollment run-delete-enrollment run-all-enrollment run-create-drug run-read-drug run-update-drug run-delete-drug run-all-drug run-create-pharmacy run-read-pharmacy run-update-pharmacy run-delete-pharmacy run-all-pharmacy run-create-formulary run-read-formulary run-update-formulary run-delete-formulary run-all-formulary run-create-formulary-drug run-read-formulary-drug run-update-formulary-drug run-delete-formulary-drug run-all-formulary-drug run-create-pharmacy-network run-read-pharmacy-network run-update-pharmacy-network run-delete-pharmacy-network run-all-pharmacy-network load-all-data

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
	@echo "  make load-all-data     - Load ALL data in correct order (Plan→Member→Enrollment)"
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
	@echo ""
	@echo "Enrollment Operations:"
	@echo "  make run-create-enrollment - CREATE: Insert enrollments from CSV (10M records)"
	@echo "  make run-read-enrollment   - READ: Display enrollment statistics"
	@echo "  make run-update-enrollment - UPDATE: Update a sample enrollment"
	@echo "  make run-delete-enrollment - DELETE: Delete a sample enrollment"
	@echo "  make run-all-enrollment    - Run all CRUD operations for enrollments"
	@echo ""
	@echo "Drug Operations:"
	@echo "  make run-create-drug       - CREATE: Insert drugs from CSV (20K records)"
	@echo "  make run-read-drug         - READ: Display drug statistics"
	@echo "  make run-update-drug       - UPDATE: Update a sample drug"
	@echo "  make run-delete-drug       - DELETE: Delete a sample drug"
	@echo "  make run-all-drug          - Run all CRUD operations for drugs"
	@echo ""
	@echo "Pharmacy Operations:"
	@echo "  make run-create-pharmacy   - CREATE: Insert pharmacies from CSV (50K records)"
	@echo "  make run-read-pharmacy     - READ: Display pharmacy statistics"
	@echo "  make run-update-pharmacy   - UPDATE: Update a sample pharmacy"
	@echo "  make run-delete-pharmacy   - DELETE: Delete a sample pharmacy"
	@echo "  make run-all-pharmacy      - Run all CRUD operations for pharmacies"
	@echo ""
	@echo "Formulary Operations:"
	@echo "  make run-create-formulary  - CREATE: Insert formularies from CSV"
	@echo "  make run-read-formulary    - READ: Display formulary statistics"
	@echo "  make run-update-formulary  - UPDATE: Update a sample formulary"
	@echo "  make run-delete-formulary  - DELETE: Delete a sample formulary"
	@echo "  make run-all-formulary     - Run all CRUD operations for formularies"
	@echo ""
	@echo "Formulary-Drug Operations:"
	@echo "  make run-create-formulary-drug - CREATE: Insert formulary-drug relationships from CSV (10M records)"
	@echo "  make run-read-formulary-drug   - READ: Display formulary-drug statistics"
	@echo "  make run-update-formulary-drug - UPDATE: Update a sample formulary-drug relationship"
	@echo "  make run-delete-formulary-drug - DELETE: Delete a sample formulary-drug relationship"
	@echo "  make run-all-formulary-drug    - Run all CRUD operations for formulary-drug relationships"
	@echo ""
	@echo "Pharmacy Network Operations:"
	@echo "  make run-create-pharmacy-network - CREATE: Insert pharmacy networks from CSV (550K records)"
	@echo "  make run-read-pharmacy-network   - READ: Display pharmacy network statistics"
	@echo "  make run-update-pharmacy-network - UPDATE: Update a sample pharmacy network"
	@echo "  make run-delete-pharmacy-network - DELETE: Delete a sample pharmacy network"
	@echo "  make run-all-pharmacy-network    - Run all CRUD operations for pharmacy networks"

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

# ============================================================================
# Enrollment CRUD Operations
# ============================================================================

# CREATE: Insert enrollments from CSV (10 million records)
run-create-enrollment: build
	@echo "Running CREATE operation for Enrollments..."
	@echo "⚠️  This will load and insert 10 million enrollment records"
	@echo "⏳ Expected time: 5-10 minutes depending on system performance"
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="CREATE ENROLLMENT"

# READ: Display enrollment statistics
run-read-enrollment: build
	@echo "Running READ operation for Enrollments..."
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="READ ENROLLMENT"

# UPDATE: Update a sample enrollment
run-update-enrollment: build
	@echo "Running UPDATE operation for Enrollments..."
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="UPDATE ENROLLMENT"

# DELETE: Delete a sample enrollment
run-delete-enrollment: build
	@echo "Running DELETE operation for Enrollments..."
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="DELETE ENROLLMENT"

# Run all CRUD operations for enrollments
run-all-enrollment: build
	@echo "Running all CRUD operations for Enrollments..."
	@echo "⚠️  This will load and insert 10 million enrollment records"
	@echo "⏳ Expected time: 5-10 minutes depending on system performance"
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="ALL ENROLLMENT"

# ============================================================================
# Drug CRUD Operations
# ============================================================================

# CREATE: Insert drugs from CSV (20,000 records)
run-create-drug: build
	@echo "Running CREATE operation for Drugs..."
	@echo "⚠️  This will load and insert 20,000 drug records"
	@echo "⏳ Expected time: 1-2 minutes depending on system performance"
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="CREATE DRUG"

# READ: Display drug statistics
run-read-drug: build
	@echo "Running READ operation for Drugs..."
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="READ DRUG"

# UPDATE: Update a sample drug
run-update-drug: build
	@echo "Running UPDATE operation for Drugs..."
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="UPDATE DRUG"

# DELETE: Delete a sample drug
run-delete-drug: build
	@echo "Running DELETE operation for Drugs..."
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="DELETE DRUG"

# Run all CRUD operations for drugs
run-all-drug: build
	@echo "Running all CRUD operations for Drugs..."
	@echo "⚠️  This will load and insert 20,000 drug records"
	@echo "⏳ Expected time: 1-2 minutes depending on system performance"
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="ALL DRUG"

# ============================================================================
# Pharmacy CRUD Operations
# ============================================================================

# CREATE: Insert pharmacies from CSV (50,000 records)
run-create-pharmacy: build
	@echo "Running CREATE operation for Pharmacies..."
	@echo "⚠️  This will load and insert 50,000 pharmacy records"
	@echo "⏳ Expected time: 1-2 minutes depending on system performance"
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="CREATE PHARMACY"

# READ: Display pharmacy statistics
run-read-pharmacy: build
	@echo "Running READ operation for Pharmacies..."
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="READ PHARMACY"

# UPDATE: Update a sample pharmacy
run-update-pharmacy: build
	@echo "Running UPDATE operation for Pharmacies..."
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="UPDATE PHARMACY"

# DELETE: Delete a sample pharmacy
run-delete-pharmacy: build
	@echo "Running DELETE operation for Pharmacies..."
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="DELETE PHARMACY"

# Run all CRUD operations for pharmacies
run-all-pharmacy: build
	@echo "Running all CRUD operations for Pharmacies..."
	@echo "⚠️  This will load and insert 50,000 pharmacy records"
	@echo "⏳ Expected time: 1-2 minutes depending on system performance"
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="ALL PHARMACY"

# ============================================================================
# Formulary CRUD Operations
# ============================================================================

# CREATE: Insert formularies from CSV
run-create-formulary: build
	@echo "Running CREATE operation for Formularies..."
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="CREATE FORMULARY"

# READ: Display formulary statistics
run-read-formulary: build
	@echo "Running READ operation for Formularies..."
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="READ FORMULARY"

# UPDATE: Update a sample formulary
run-update-formulary: build
	@echo "Running UPDATE operation for Formularies..."
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="UPDATE FORMULARY"

# DELETE: Delete a sample formulary
run-delete-formulary: build
	@echo "Running DELETE operation for Formularies..."
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="DELETE FORMULARY"

# Run all CRUD operations for formularies
run-all-formulary: build
	@echo "Running all CRUD operations for Formularies..."
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="ALL FORMULARY"

# ============================================================================
# Formulary-Drug CRUD Operations
# ============================================================================

# CREATE: Insert formulary-drug relationships from CSV (10 million records)
run-create-formulary-drug: build
	@echo "Running CREATE operation for Formulary-Drug relationships..."
	@echo "⚠️  This will load and insert 10 million formulary-drug relationship records"
	@echo "⏳ Expected time: 5-10 minutes depending on system performance"
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="CREATE FORMULARY_DRUG"

# READ: Display formulary-drug statistics
run-read-formulary-drug: build
	@echo "Running READ operation for Formulary-Drug relationships..."
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="READ FORMULARY_DRUG"

# UPDATE: Update a sample formulary-drug relationship
run-update-formulary-drug: build
	@echo "Running UPDATE operation for Formulary-Drug relationships..."
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="UPDATE FORMULARY_DRUG"

# DELETE: Delete a sample formulary-drug relationship
run-delete-formulary-drug: build
	@echo "Running DELETE operation for Formulary-Drug relationships..."
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="DELETE FORMULARY_DRUG"

# Run all CRUD operations for formulary-drug relationships
run-all-formulary-drug: build
	@echo "Running all CRUD operations for Formulary-Drug relationships..."
	@echo "⚠️  This will load and insert 10 million formulary-drug relationship records"
	@echo "⏳ Expected time: 5-10 minutes depending on system performance"
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="ALL FORMULARY_DRUG"

# ============================================================================
# Pharmacy Network CRUD Operations
# ============================================================================

# CREATE: Insert pharmacy networks from CSV (550,000 records)
run-create-pharmacy-network: build
	@echo "Running CREATE operation for Pharmacy Networks..."
	@echo "⚠️  This will load and insert 550,000 pharmacy network records"
	@echo "⏳ Expected time: 2-3 minutes depending on system performance"
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="CREATE PHARMACY_NETWORK"

# READ: Display pharmacy network statistics
run-read-pharmacy-network: build
	@echo "Running READ operation for Pharmacy Networks..."
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="READ PHARMACY_NETWORK"

# UPDATE: Update a sample pharmacy network
run-update-pharmacy-network: build
	@echo "Running UPDATE operation for Pharmacy Networks..."
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="UPDATE PHARMACY_NETWORK"

# DELETE: Delete a sample pharmacy network
run-delete-pharmacy-network: build
	@echo "Running DELETE operation for Pharmacy Networks..."
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="DELETE PHARMACY_NETWORK"

# Run all CRUD operations for pharmacy networks
run-all-pharmacy-network: build
	@echo "Running all CRUD operations for Pharmacy Networks..."
	@echo "⚠️  This will load and insert 550,000 pharmacy network records"
	@echo "⏳ Expected time: 2-3 minutes depending on system performance"
	mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="ALL PHARMACY_NETWORK"

# ============================================================================
# Load All Data in Correct Order
# ============================================================================

# Load all data respecting foreign key relationships
load-all-data: build
	@echo "════════════════════════════════════════════════════════════════"
	@echo "Loading ALL Data in Correct Order"
	@echo "════════════════════════════════════════════════════════════════"
	@echo ""
	@echo "This will load data in the following order to satisfy foreign key relationships:"
	@echo "  1. Plans (34 records) - Referenced by enrollments and formularies"
	@echo "  2. Drugs (20,000 records) - Referenced by formulary-drug relationships"
	@echo "  3. Pharmacies (50,000 records) - Referenced by pharmacy networks"
	@echo "  4. Members (1,000,000 records) - Referenced by enrollments"
	@echo "  5. Enrollments (10,000,000 records) - References plans and members"
	@echo "  6. Formularies - References plans"
	@echo "  7. Formulary-Drug Relationships (10,000,000 records) - References formularies and drugs"
	@echo "  8. Pharmacy Networks (550,000 records) - References pharmacies"
	@echo ""
	@echo "⚠️  Total expected time: 25-35 minutes"
	@echo "⏳ Please be patient, this is a large dataset..."
	@echo ""
	@echo "════════════════════════════════════════════════════════════════"
	@echo "Step 1/8: Loading Benefit Plans..."
	@echo "════════════════════════════════════════════════════════════════"
	@mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="CREATE PLAN"
	@echo ""
	@echo "════════════════════════════════════════════════════════════════"
	@echo "Step 2/8: Loading Drugs..."
	@echo "════════════════════════════════════════════════════════════════"
	@mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="CREATE DRUG"
	@echo ""
	@echo "════════════════════════════════════════════════════════════════"
	@echo "Step 3/8: Loading Pharmacies..."
	@echo "════════════════════════════════════════════════════════════════"
	@mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="CREATE PHARMACY"
	@echo ""
	@echo "════════════════════════════════════════════════════════════════"
	@echo "Step 4/8: Loading Members..."
	@echo "════════════════════════════════════════════════════════════════"
	@mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="CREATE MEMBER"
	@echo ""
	@echo "════════════════════════════════════════════════════════════════"
	@echo "Step 5/8: Loading Enrollments..."
	@echo "════════════════════════════════════════════════════════════════"
	@mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="CREATE ENROLLMENT"
	@echo ""
	@echo "════════════════════════════════════════════════════════════════"
	@echo "Step 6/8: Loading Formularies..."
	@echo "════════════════════════════════════════════════════════════════"
	@mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="CREATE FORMULARY"
	@echo ""
	@echo "════════════════════════════════════════════════════════════════"
	@echo "Step 7/8: Loading Formulary-Drug Relationships..."
	@echo "════════════════════════════════════════════════════════════════"
	@mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="CREATE FORMULARY_DRUG"
	@echo ""
	@echo "════════════════════════════════════════════════════════════════"
	@echo "Step 8/8: Loading Pharmacy Networks..."
	@echo "════════════════════════════════════════════════════════════════"
	@mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App" -Dexec.args="CREATE PHARMACY_NETWORK"
	@echo ""
	@echo "════════════════════════════════════════════════════════════════"
	@echo "✅ ALL DATA LOADED SUCCESSFULLY!"
	@echo "════════════════════════════════════════════════════════════════"
	@echo ""
	@echo "Summary:"
	@echo "  ✓ Plans: 34 records"
	@echo "  ✓ Drugs: 20,000 records"
	@echo "  ✓ Pharmacies: 50,000 records"
	@echo "  ✓ Members: 1,000,000 records"
	@echo "  ✓ Enrollments: 10,000,000 records"
	@echo "  ✓ Formularies: ~34 records"
	@echo "  ✓ Formulary-Drug Relationships: 10,000,000 records"
	@echo "  ✓ Pharmacy Networks: 550,000 records"
	@echo ""
	@echo "Verify data with:"
	@echo "  make db-shell"
	@echo "  SELECT COUNT(*) FROM plan;"
	@echo "  SELECT COUNT(*) FROM drug;"
	@echo "  SELECT COUNT(*) FROM pharmacy;"
	@echo "  SELECT COUNT(*) FROM member;"
	@echo "  SELECT COUNT(*) FROM enrollment;"
	@echo "  SELECT COUNT(*) FROM formulary;"
	@echo "  SELECT COUNT(*) FROM formulary_drug;"
	@echo "  SELECT COUNT(*) FROM pharmacy_network;"
	@echo ""