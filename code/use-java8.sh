#!/bin/bash

# Script to activate Java 8 for this project
# Usage: source use-java8.sh

echo "Activating Java 8 for this terminal session..."

# Set JAVA_HOME to Java 8
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8 2>/dev/null)

if [ -z "$JAVA_HOME" ]; then
    echo "❌ ERROR: Java 8 not found!"
    echo ""
    echo "Please install Java 8 first:"
    echo "  brew install --cask zulu@8"
    echo ""
    return 1
fi

# Update PATH
export PATH="$JAVA_HOME/bin:$PATH"

# Verify
echo "✓ Java 8 activated successfully!"
echo ""
java -version
echo ""
echo "You can now build and run the project:"
echo "  cd code"
echo "  ./scripts/build.sh"
echo "  java -cp build/classes startup.StartQCServer"
