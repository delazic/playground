#!/bin/bash

# Create the resources directory structure
mkdir -p src/main/resources/data

# Move all CSV files
echo "Moving CSV files to src/main/resources/data/..."
cp database/data/*.csv src/main/resources/data/

# List the files
echo ""
echo "Files in src/main/resources/data/:"
ls -lh src/main/resources/data/*.csv | wc -l
echo "CSV files copied successfully!"

# Keep originals in database/data for Docker/database use
echo ""
echo "Original files kept in database/data/ for Docker/database use"
