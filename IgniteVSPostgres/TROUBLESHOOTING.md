# Troubleshooting Guide

## PostgreSQL Failed to Start

If you're seeing "PostgreSQL failed to start" error, follow these steps:

### Step 1: Check Docker Logs

```bash
cd IgniteVSPostgres
docker-compose logs postgres
```

Look for error messages in the output.

### Step 2: Check Container Status

```bash
docker-compose ps
```

Check if the postgres container is running or has exited.

### Step 3: Check Port Conflicts

```bash
# Check if port 5432 is already in use
lsof -i :5432

# Or on some systems
netstat -an | grep 5432
```

If port 5432 is in use by another process:
- Stop the conflicting service
- Or change the port in `docker-compose.yml`:
  ```yaml
  ports:
    - "5433:5432"  # Use 5433 instead
  ```

### Step 4: Check Docker Resources

```bash
# Check Docker is running
docker info

# Check available disk space
df -h
```

### Step 5: Clean Start

```bash
# Stop all containers
docker-compose down

# Remove volumes (WARNING: deletes all data)
docker-compose down -v

# Pull fresh images
docker-compose pull

# Start again
docker-compose up -d

# Watch logs
docker-compose logs -f postgres
```

### Common Issues and Solutions

#### Issue: "permission denied" errors

**Solution:**
```bash
# Fix permissions on database directory
chmod -R 755 database/

# Recreate volumes
docker-compose down -v
docker-compose up -d
```

#### Issue: "port is already allocated"

**Solution:**
```bash
# Find what's using port 5432
lsof -i :5432

# Kill the process (replace PID with actual process ID)
kill -9 <PID>

# Or stop PostgreSQL if installed locally
brew services stop postgresql@16
```

#### Issue: "no space left on device"

**Solution:**
```bash
# Clean up Docker
docker system prune -a --volumes

# Check disk space
df -h
```

#### Issue: SQL syntax errors in init scripts

**Solution:**
```bash
# Check SQL files for errors
cat database/init/01-create-schema.sql | head -20
cat database/init/02-seed-data.sql | head -20

# Test SQL manually
docker-compose exec postgres psql -U pbm_user -d pbm_db -f /docker-entrypoint-initdb.d/01-create-schema.sql
```

### Step 6: Manual Database Setup

If automatic initialization fails, set up manually:

```bash
# Start only PostgreSQL without init scripts
docker-compose up -d postgres

# Wait for it to be ready
sleep 10

# Connect to database
docker-compose exec postgres psql -U pbm_user -d pbm_db

# In psql, run:
\i /docker-entrypoint-initdb.d/01-create-schema.sql
\i /docker-entrypoint-initdb.d/02-seed-data.sql
\q
```

### Step 7: Verify Setup

```bash
# Check if database is accessible
docker-compose exec postgres pg_isready -U pbm_user -d pbm_db

# Connect and verify tables
docker-compose exec postgres psql -U pbm_user -d pbm_db -c "\dt"

# Check sample data
docker-compose exec postgres psql -U pbm_user -d pbm_db -c "SELECT COUNT(*) FROM member;"
```

### Getting Help

If issues persist, gather this information:

```bash
# System info
uname -a
docker --version
docker-compose --version

# Container status
docker-compose ps

# Full logs
docker-compose logs > docker-logs.txt

# Docker info
docker info > docker-info.txt
```

Then review the logs for specific error messages.

## Redis Issues

### Redis won't start

```bash
# Check logs
docker-compose logs redis

# Check port
lsof -i :6379

# Restart
docker-compose restart redis
```

## pgAdmin Issues

### Can't access pgAdmin

```bash
# Check if running
docker-compose ps pgadmin

# Check logs
docker-compose logs pgadmin

# Access at: http://localhost:5050
```

### Can't connect to PostgreSQL from pgAdmin

Use these connection settings:
- Host: `host.docker.internal` (on Mac)
- Port: `5432`
- Database: `pbm_db`
- Username: `pbm_user`
- Password: `pbm_password`

## Setup Script Issues

### setup.sh permission denied

```bash
chmod +x setup.sh
./setup.sh
```

### Homebrew installation fails

```bash
# Install Homebrew manually
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Add to PATH (for Apple Silicon Macs)
echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile
eval "$(/opt/homebrew/bin/brew shellenv)"
```

## Quick Reset

To completely reset everything:

```bash
# Stop and remove everything
docker-compose down -v

# Remove any leftover containers
docker ps -a | grep pbm | awk '{print $1}' | xargs docker rm -f

# Remove any leftover volumes
docker volume ls | grep pbm | awk '{print $2}' | xargs docker volume rm

# Start fresh
./setup.sh