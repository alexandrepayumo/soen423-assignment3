# DSMS Web Service Application

Distributed Supply Management System implemented with JAX-WS Web Services.

## Setup (One Time)

Activate Java 8 in your terminal:

```bash
source use-java8.sh
```

Build the project:

```bash
./scripts/build.sh
```

## Running the Application

### 1. Start Servers

```bash
./scripts/start_webservice_servers.sh
```

This starts all three servers (QC, ON, BC) in the background.

Wait a few seconds for servers to start, then verify they're running:
- http://localhost:8080/QCServer?wsdl
- http://localhost:8081/ONServer?wsdl
- http://localhost:8082/BCServer?wsdl

### 2. Run Client Applications

**Customer Client:**
```bash
./scripts/run_customer_client.sh
```
Enter customer ID (e.g., `QCU1111`, `ONU2222`, `BCU3333`) and use the menu.

**Manager Client:**
```bash
./scripts/run_manager_client.sh
```
Enter manager ID (e.g., `QCM1111`, `ONM2222`, `BCM3333`) and use the menu.

### 3. Run Tests (Optional)

Run comprehensive test suite:
```bash
./scripts/run_tests.sh
```

This runs 28 automated tests covering all operations.

### 4. Stop Servers

```bash
./scripts/stop_servers.sh
```

## Available Operations

**Manager Operations:**
- Add Item
- Remove Item
- List Items

**Customer Operations:**
- Purchase Item
- Find Item (searches across all stores)
- Return Item
- Exchange Item

## Rebuilding

If you modify source code:

```bash
source use-java8.sh
./scripts/build.sh
```

## Logs

- Server logs: `logs/server/`
- Client logs: `logs/client/`

## Notes

- Java 8 must be active (`source use-java8.sh`) before running any script
- Servers must be running before starting clients
- Multiple clients can run simultaneously
