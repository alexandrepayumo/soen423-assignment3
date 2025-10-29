#!/bin/bash

echo "Building Web Service DSMS Application..."

# Check if Java 8 is active
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1-2)
if [ "$JAVA_VERSION" != "1.8" ]; then
    echo "ERROR: Java 8 is required but Java $JAVA_VERSION is active"
    echo ""
    echo "Please run these commands first:"
    echo "  export JAVA_HOME=\$(/usr/libexec/java_home -v 1.8)"
    echo "  export PATH=\"\$JAVA_HOME/bin:\$PATH\""
    echo ""
    exit 1
fi

echo "✓ Using Java 8"

# Save the starting directory
START_DIR=$(pwd)

# Create build directory
mkdir -p build/classes

# Step 1: Compile main source files (excluding generated client stubs for now)
echo "Step 1: Compiling Java source files..."
cd "$START_DIR"

# Compile all Java files
find src/main/java/interfaces -name "*.java" > sources.txt 2>/dev/null || true
find src/main/java/server -name "*.java" >> sources.txt 2>/dev/null || true
find src/main/java/models -name "*.java" >> sources.txt 2>/dev/null || true
find src/main/java/utils -name "*.java" >> sources.txt 2>/dev/null || true
find src/main/java/startup -name "*.java" >> sources.txt 2>/dev/null || true

# Check if we have any source files
if [ ! -s sources.txt ]; then
    echo "ERROR: No source files found!"
    rm sources.txt
    exit 1
fi

javac -d build/classes -sourcepath src/main/java @sources.txt
COMPILE_RESULT=$?
rm sources.txt

if [ $COMPILE_RESULT -ne 0 ]; then
    echo "ERROR: Java compilation failed!"
    exit 1
fi

echo "Java compilation successful!"

# Step 2: Compile generated client stubs if they exist
if [ -d "src/main/java/client/generated" ]; then
    echo "Step 2: Compiling generated client stubs..."
    find src/main/java/client/generated -name "*.java" > sources.txt 2>/dev/null || true
    
    if [ -s sources.txt ]; then
        javac -d build/classes -cp build/classes -sourcepath src/main/java @sources.txt
        if [ $? -ne 0 ]; then
            echo "WARNING: Client stub compilation failed!"
            echo "You may need to regenerate client stubs after starting servers."
        else
            echo "Client stub compilation successful!"
        fi
    fi
    rm sources.txt
else
    echo "Step 2: No client stubs found (run generate_client_stubs.sh after starting servers)"
fi

# Step 3: Compile client applications
echo "Step 3: Compiling client applications..."
find src/main/java/client -name "*.java" -not -path "*/generated/*" > sources.txt 2>/dev/null || true

if [ -s sources.txt ]; then
    javac -d build/classes -cp build/classes -sourcepath src/main/java @sources.txt
    if [ $? -ne 0 ]; then
        echo "WARNING: Client application compilation may fail if client stubs are not generated"
    else
        echo "Client application compilation successful!"
    fi
fi
rm sources.txt

echo ""
echo "Build complete!"
echo ""
echo "Next steps:"
echo "1. Start servers: ./scripts/start_webservice_servers.sh"
echo "2. Generate client stubs: ./scripts/generate_client_stubs.sh"
echo "3. Rebuild to compile client applications with stubs"
