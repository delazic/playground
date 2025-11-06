#!/bin/bash

# Script to run RDBMS App with proper classpath

echo "Building project..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

echo ""
echo "Running RDBMS App..."
echo ""

# Run with Maven (easiest way - handles classpath automatically)
mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App"
