#!/bin/bash

# Local Development Setup Script
# Prepares local environment for running ETL pipeline

set -e

echo "=========================================="
echo "Spark Scala ETL - Local Development Setup"
echo "=========================================="

# Check Java installation
echo "Checking Java..."
if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed. Please install Java 11+"
    exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | grep version | head -1)
echo "✓ Java found: $JAVA_VERSION"

# Check SBT installation
echo ""
echo "Checking SBT..."
if ! command -v sbt &> /dev/null; then
    echo "ERROR: SBT is not installed."
    echo "Install from: https://www.scala-sbt.org/download.html"
    exit 1
fi
SBT_VERSION=$(sbt sbtVersion 2>&1 | tail -1)
echo "✓ SBT found: $SBT_VERSION"

# Create local warehouse directory
echo ""
echo "Creating local warehouse directory..."
mkdir -p warehouse
mkdir -p data/input/{csv,json,parquet}
echo "✓ Directories created"

# Download sample data if needed
if [ ! -f "data/input/csv/sample_customers.csv" ]; then
    echo ""
    echo "Sample data files already exist"
else
    echo "✓ Sample data found"
fi

# Compile the project
echo ""
echo "Compiling Scala project..."
sbt clean compile

if [ $? -eq 0 ]; then
    echo ""
    echo "=========================================="
    echo "✓ Setup completed successfully!"
    echo "=========================================="
    echo ""
    echo "Next steps:"
    echo "1. Configure environment variables:"
    echo "   export SPARK_ENV=local"
    echo ""
    echo "2. Run the ETL pipeline:"
    echo "   sbt run"
    echo ""
    echo "3. Check results:"
    echo "   ls -la warehouse/"
    echo ""
else
    echo ""
    echo "=========================================="
    echo "✗ Build failed"
    echo "=========================================="
    exit 1
fi

