# Database Initialization Guide

## How Database Schema and Data Are Created

### Overview

The PostgreSQL database is automatically initialized when the Docker container starts for the **first time**. This is handled by PostgreSQL's built-in initialization mechanism.

## Automatic Initialization Process

### 1. Docker Compose Configuration

In `docker-compose.yml`, line 14 mounts the init directory:

```yaml
volumes:
  - ./database/init:/docker-entrypoint-initdb.d
```

### 2. PostgreSQL Init Directory

PostgreSQL automatically executes all `.sql` and `.sh` files in `/docker-entrypoint-initdb.d` **in alphabetical order** when the container is created for the first time.

### 3. Initialization Scripts

The scripts are executed in this order:

#### **01-create-schema.sql** (Executed First)
- Creates all database tables
- Creates custom types (ENUM types)
- Creates indexes for performance
- Creates triggers for `updated_at` columns
- Creates table partitions (for claims table)
- Adds table comments

**What it creates:**
- 15+ tables (member, plan, drug, pharmacy, claim, etc.)
- Custom types (claim_status_type, gender_type, etc.)
- 30+ indexes
- Triggers for automatic timestamp updates
- Partitioned claims table (2024, 2025)

#### **02-seed-data.sql** (Executed Second)
- Populates tables with sample data
- Creates realistic test data for development

**What it inserts:**
- 4 sample plans (Gold, Silver, Bronze, Medicare)
- 5 sample members
- 5 enrollments (linking members to plans)
- 13 drugs (generic, brand, specialty)
- Formulary entries (drug coverage rules)
- 5 pharmacies (retail, mail order, specialty)
- Pharmacy network assignments
- Drug interactions
- 1 prior authorization
- Plan rules (quantity limits, step therapy)
- 15 sample claims

## When Does Initialization Happen?

### ✅ Initialization RUNS When:
1. **First time starting the container** with a new volume
2. **After deleting the volume** and recreating it

### ❌ Initialization DOES NOT RUN When:
1. Restarting an existing container (`docker-compose restart`)
2. Stopping and starting the container (`docker-compose stop/start`)
3. The PostgreSQL data volume already exists

## How to Verify Initialization

### Check if data was loaded:

```bash
# Connect to PostgreSQL
docker exec -it pbm-postgres psql -U pbm_user -d pbm_db

# Check table counts
SELECT 'members' as table_name, COUNT(*) as count FROM member
UNION ALL
SELECT 'plans', COUNT(*) FROM plan
UNION ALL
SELECT 'drugs', COUNT(*) FROM drug
UNION ALL
SELECT 'pharmacies', COUNT(*) FROM pharmacy
UNION ALL
SELECT 'claims', COUNT(*) FROM claim;
```

**Expected output:**
```
 table_name | count 
------------+-------
 members    |     5
 plans      |     4
 drugs      |    13
 pharmacies |     5
 claims     |    15
```

## How to Re-initialize the Database

If you need to reset the database and reload all data:

### Option 1: Delete Volume and Recreate

```bash
# Stop and remove containers and volumes
docker-compose down -v

# Start fresh (will run init scripts)
docker-compose up -d postgres

# Wait for initialization to complete
docker-compose logs -f postgres
```

### Option 2: Manual Re-initialization

```bash
# Connect to database
docker exec -it pbm-postgres psql -U pbm_user -d pbm_db

# Drop all tables (careful!)
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;

# Exit psql
\q

# Run init scripts manually
docker exec -i pbm-postgres psql -U pbm_user -d pbm_db < database/init/01-create-schema.sql
docker exec -i pbm-postgres psql -U pbm_user -d pbm_db < database/init/02-seed-data.sql
```

## Checking Initialization Logs

To see if initialization ran successfully:

```bash
# View PostgreSQL logs
docker-compose logs postgres | grep -A 20 "database system is ready"

# Look for these messages:
# - "PostgreSQL init process complete"
# - "Sample Data Loaded Successfully"
# - Table counts in the output
```

## Common Issues

### Issue 1: No Data in Tables

**Cause:** Container was restarted, not recreated

**Solution:**
```bash
docker-compose down -v  # Remove volumes
docker-compose up -d postgres
```

### Issue 2: Init Scripts Not Running

**Cause:** Volume already exists from previous run

**Solution:**
```bash
# List volumes
docker volume ls | grep postgres

# Remove specific volume
docker volume rm ignitevspostgres_postgres_data

# Or remove all project volumes
docker-compose down -v
```

### Issue 3: Partial Data Load

**Cause:** Script error during initialization

**Solution:**
```bash
# Check logs for errors
docker-compose logs postgres | grep ERROR

# Fix the SQL script
# Then recreate: docker-compose down -v && docker-compose up -d
```

## File Structure

```
IgniteVSPostgres/
├── docker-compose.yml          # Mounts init directory
└── database/
    └── init/                   # Auto-executed on first start
        ├── 01-create-schema.sql   # Creates tables (runs first)
        └── 02-seed-data.sql       # Inserts data (runs second)
```

## Summary

| Action | Schema Created | Data Loaded |
|--------|---------------|-------------|
| First `docker-compose up` | ✅ Yes | ✅ Yes |
| `docker-compose restart` | ❌ No | ❌ No |
| `docker-compose down -v` then `up` | ✅ Yes | ✅ Yes |
| Container stop/start | ❌ No | ❌ No |

**Key Point:** The init scripts only run **once** when the PostgreSQL data directory is empty (first container creation).