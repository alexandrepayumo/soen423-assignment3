#!/bin/bash

echo "Stopping Web Service Servers..."

# Stop servers using saved PIDs
if [ -f /tmp/qc_server.pid ]; then
    kill $(cat /tmp/qc_server.pid) 2>/dev/null
    rm /tmp/qc_server.pid
    echo "QC Server stopped"
fi

if [ -f /tmp/on_server.pid ]; then
    kill $(cat /tmp/on_server.pid) 2>/dev/null
    rm /tmp/on_server.pid
    echo "ON Server stopped"
fi

if [ -f /tmp/bc_server.pid ]; then
    kill $(cat /tmp/bc_server.pid) 2>/dev/null
    rm /tmp/bc_server.pid
    echo "BC Server stopped"
fi

# Kill any remaining Java server processes
pkill -f "StartQCServer"
pkill -f "StartONServer"
pkill -f "StartBCServer"

echo "All web service servers stopped"
