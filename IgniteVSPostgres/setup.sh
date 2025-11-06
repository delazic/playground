#!/bin/bash

# PBM System Setup Script for macOS
# This script sets up the development environment

set -e

echo "=========================================="
echo "PBM System - Development Setup"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_info() {
    echo -e "ℹ $1"
}

# Check if running on macOS
if [[ "$OSTYPE" != "darwin"* ]]; then
    print_error "This script is designed for macOS"
    exit 1
fi

print_success "Running on macOS"

# Check for Homebrew
echo ""
print_info "Checking for Homebrew..."
if ! command -v brew &> /dev/null; then
    print_warning "Homebrew not found. Installing..."
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    print_success "Homebrew installed"
else
    print_success "Homebrew found"
fi

# Check for Docker
echo ""
print_info "Checking for Docker..."
if ! command -v docker &> /dev/null; then
    print_warning "Docker not found. Please install Docker Desktop from:"
    print_info "https://www.docker.com/products/docker-desktop"
    exit 1
else
    print_success "Docker found"
    
    # Check if Docker is running
    if ! docker info &> /dev/null; then
        print_error "Docker is not running. Please start Docker Desktop"
        exit 1
    fi
    print_success "Docker is running"
fi

# Check for Docker Compose
echo ""
print_info "Checking for Docker Compose..."
if ! command -v docker-compose &> /dev/null; then
    print_warning "Docker Compose not found. Installing..."
    brew install docker-compose
    print_success "Docker Compose installed"
else
    print_success "Docker Compose found"
fi

# Check for Java
echo ""
print_info "Checking for Java..."
if ! command -v java &> /dev/null; then
    print_warning "Java not found. Installing OpenJDK 17..."
    brew install openjdk@17
    print_success "Java installed"
else
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 17 ]; then
        print_success "Java $JAVA_VERSION found"
    else
        print_warning "Java version is less than 17. Installing OpenJDK 17..."
        brew install openjdk@17
    fi
fi

# Check for Maven
echo ""
print_info "Checking for Maven..."
if ! command -v mvn &> /dev/null; then
    print_warning "Maven not found. Installing..."
    brew install maven
    print_success "Maven installed"
else
    print_success "Maven found"
fi

# Check for PostgreSQL client (optional, for psql command)
echo ""
print_info "Checking for PostgreSQL client..."
if ! command -v psql &> /dev/null; then
    print_warning "PostgreSQL client not found. Installing..."
    brew install postgresql@16
    print_success "PostgreSQL client installed"
else
    print_success "PostgreSQL client found"
fi

# Create .env file if it doesn't exist
echo ""
print_info "Setting up environment configuration..."
if [ ! -f .env ]; then
    cp .env.pbm .env
    print_success "Created .env file from .env.pbm"
    print_warning "Please review and update .env file with your settings"
else
    print_success ".env file already exists"
fi

# Create necessary directories
echo ""
print_info "Creating project directories..."
mkdir -p database/init
mkdir -p database/scripts
mkdir -p logs
print_success "Directories created"

# Start Docker services
echo ""
print_info "Starting Docker services..."
docker-compose up -d

# Wait for PostgreSQL to be ready
echo ""
print_info "Waiting for PostgreSQL to be ready..."

# First check if container is running
sleep 5
if ! docker-compose ps postgres | grep -q "Up"; then
    print_error "PostgreSQL container failed to start"
    print_info "Checking logs..."
    docker-compose logs postgres | tail -20
    print_info ""
    print_info "Try these steps:"
    print_info "1. Check if port 5432 is in use: lsof -i :5432"
    print_info "2. View full logs: docker-compose logs postgres"
    print_info "3. Try manual start: docker-compose up postgres"
    exit 1
fi

# Wait for PostgreSQL to accept connections
MAX_RETRIES=60
RETRY_COUNT=0
until docker-compose exec -T postgres pg_isready -U pbm_user -d pbm_db &> /dev/null || [ $RETRY_COUNT -eq $MAX_RETRIES ]; do
    if [ $((RETRY_COUNT % 10)) -eq 0 ]; then
        echo -n " ${RETRY_COUNT}s"
    else
        echo -n "."
    fi
    sleep 1
    RETRY_COUNT=$((RETRY_COUNT + 1))
done
echo ""

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    print_error "PostgreSQL failed to become ready after ${MAX_RETRIES} seconds"
    print_info "Checking logs..."
    docker-compose logs postgres | tail -30
    print_info ""
    print_info "Troubleshooting steps:"
    print_info "1. Check logs: docker-compose logs postgres"
    print_info "2. Check status: docker-compose ps"
    print_info "3. Try restart: docker-compose restart postgres"
    print_info "4. See TROUBLESHOOTING.md for more help"
    exit 1
fi

print_success "PostgreSQL is ready"

# Verify database setup
echo ""
print_info "Verifying database setup..."
TABLE_COUNT=$(docker-compose exec -T postgres psql -U pbm_user -d pbm_db -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" | tr -d ' ')

if [ "$TABLE_COUNT" -gt 0 ]; then
    print_success "Database schema created successfully ($TABLE_COUNT tables)"
else
    print_error "Database schema creation failed"
    exit 1
fi

# Display connection information
echo ""
echo "=========================================="
echo "Setup Complete!"
echo "=========================================="
echo ""
echo "Services are running:"
echo ""
echo "PostgreSQL:"
echo "  Host: localhost"
echo "  Port: 5432"
echo "  Database: pbm_db"
echo "  User: pbm_user"
echo "  Password: pbm_password"
echo ""
echo "pgAdmin:"
echo "  URL: http://localhost:5050"
echo "  Email: admin@example.com"
echo "  Password: admin"
echo ""
echo "Redis:"
echo "  Host: localhost"
echo "  Port: 6379"
echo ""
echo "Useful commands:"
echo "  make help       - Show all available commands"
echo "  make start      - Start all services"
echo "  make stop       - Stop all services"
echo "  make logs       - View logs"
echo "  make db-shell   - Connect to PostgreSQL"
echo ""
echo "To connect to PostgreSQL from command line:"
echo "  psql -h localhost -p 5432 -U pbm_user -d pbm_db"
echo ""
print_success "Development environment is ready!"