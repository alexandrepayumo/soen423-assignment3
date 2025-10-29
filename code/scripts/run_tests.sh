#!/bin/bash

# Check if Java 8 is active
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1-2)
if [ "$JAVA_VERSION" != "1.8" ]; then
    echo "ERROR: Java 8 is required but Java $JAVA_VERSION is active"
    echo ""
    echo "Please run: source use-java8.sh"
    echo ""
    exit 1
fi

echo "Running Comprehensive Test Suite..."
echo ""
cd "$(dirname "$0")/.."
java -cp build/classes ComprehensiveTestRunner
