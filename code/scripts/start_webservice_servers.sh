#!/bin/bash

# Script to start all three web service servers
cd "$(dirname "$0")/.."

echo "Starting DSMS Web Service Servers..."
echo "======================================"

# Start QC Server
echo "Starting QC Server on port 8080..."
java -cp build/classes startup.StartQCServer &
QC_PID=$!
sleep 2

# Start ON Server
echo "Starting ON Server on port 8081..."
java -cp build/classes startup.StartONServer &
ON_PID=$!
sleep 2

# Start BC Server
echo "Starting BC Server on port 8082..."
java -cp build/classes startup.StartBCServer &
BC_PID=$!
sleep 2

echo ""
echo "All servers started successfully!"
echo "=================================="
echo "QC Server PID: $QC_PID (http://localhost:8080/QCServer?wsdl)"
echo "ON Server PID: $ON_PID (http://localhost:8081/ONServer?wsdl)"
echo "BC Server PID: $BC_PID (http://localhost:8082/BCServer?wsdl)"
echo ""
echo "PIDs saved to /tmp/ for later cleanup"

# Save PIDs for cleanup
echo $QC_PID > /tmp/qc_server.pid
echo $ON_PID > /tmp/on_server.pid
echo $BC_PID > /tmp/bc_server.pid

echo ""
echo "Press Ctrl+C to stop monitoring (servers will continue running)"
echo "Use ./scripts/stop_servers.sh to stop all servers"

wait
