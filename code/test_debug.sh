#!/bin/bash
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
export PATH="$JAVA_HOME/bin:$PATH"

echo "Testing specific scenarios..."

# Test manager ID validation
echo "Test: Manager validation"
java -cp build/classes -c "
qcServer.addItem('QCM1111', 'QC9999', 'TestItem', 10, 5.0)
" 2>&1

./scripts/run_tests.sh
