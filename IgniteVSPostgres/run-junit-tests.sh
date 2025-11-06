#!/bin/bash

# Script to run JUnit tests for database connectivity

echo "Building project and running JUnit tests..."
echo ""

# Run tests with Maven
mvn clean test

echo ""
echo "Test execution completed!"
