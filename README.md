# Market Mind

Market Mind is a Spring Boot application for financial market analysis, leveraging market data. This project provides RESTful APIs to manage and analyze market data for various financial instruments.

## Phase I Implementation

- Basic Spring Boot setup with JPA and H2 database
- MarketData entity with OHLCV (Open, High, Low, Close, Volume) data
- CRUD operations via REST API
- In-memory H2 database for development
- Built-in monitoring/logging support for startup, request handling, and persistence events
- Comprehensive JUnit test coverage (25 tests covering controller, service, and repository layers)

### Phase I API Endpoints

#### 1. Get All Market Data
**Endpoint:** `GET /market-mind/api/marketdata`  
**Description:** Retrieves a list of all market data entries stored in the database.  
**Curl Command:**
```bash
curl -X GET http://localhost:8080/market-mind/api/marketdata
```
**Response:** Array of MarketData objects in JSON format.

#### 2. Get Market Data by ID
**Endpoint:** `GET /market-mind/api/marketdata/{id}`  
**Description:** Retrieves a specific market data entry by its unique ID.  
**Curl Command:**
```bash
curl -X GET http://localhost:8080/market-mind/api/marketdata/1
```
**Response:** Single MarketData object in JSON format or 404 if not found.

#### 3. Create New Market Data
**Endpoint:** `POST /market-mind/api/marketdata`  
**Description:** Creates a new market data entry in the database.  
**Curl Command with Mock Data:**
```bash
curl -X POST http://localhost:8080/market-mind/api/marketdata \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "AAPL",
    "timestamp": "2026-04-18T10:00:00",
    "openPrice": 150.00,
    "highPrice": 155.00,
    "lowPrice": 149.00,
    "closePrice": 154.50,
    "volume": 1000000
  }'
```
**Response:** The created MarketData object with generated ID.

#### 4. Update Market Data
**Endpoint:** `PUT /market-mind/api/marketdata/{id}`  
**Description:** Updates an existing market data entry by its ID.  
**Curl Command with Mock Data:**
```bash
curl -X PUT http://localhost:8080/market-mind/api/marketdata/1 \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "AAPL",
    "timestamp": "2026-04-18T10:00:00",
    "openPrice": 150.00,
    "highPrice": 156.00,
    "lowPrice": 149.00,
    "closePrice": 155.50,
    "volume": 1100000
  }'
```
**Response:** The updated MarketData object or 404 if not found.

#### 5. Delete Market Data
**Endpoint:** `DELETE /market-mind/api/marketdata/{id}`  
**Description:** Deletes a market data entry by its ID.  
**Curl Command:**
```bash
curl -X DELETE http://localhost:8080/market-mind/api/marketdata/1
```
**Response:** 204 No Content if successful, 404 if not found.

## Getting Started

### Prerequisites
- Java 17
- Maven 3.6+

### Running the Application
1. Clone the repository
2. Navigate to the project directory
3. Run `mvn spring-boot:run`

The application will start on port 8080.

### API Endpoints
- `GET /market-mind/api/marketdata` - Get all market data
- `GET /market-mind/api/marketdata/{id}` - Get market data by ID
- `POST /market-mind/api/marketdata` - Create new market data
- `PUT /market-mind/api/marketdata/{id}` - Update market data
- `DELETE /market-mind/api/marketdata/{id}` - Delete market data

### H2 Console
Access the H2 database console at `http://localhost:8080/h2-console` when the application is running.

## Testing

The application includes comprehensive JUnit 5 test coverage:

- **Controller Tests** (`MarketDataControllerTest`): 8 integration tests for REST endpoints using MockMvc
- **Service Tests** (`MarketDataServiceTest`): 8 unit tests for business logic
- **Repository Tests** (`MarketDataRepositoryTest`): 8 integration tests for data access layer
- **Application Tests** (`MarketMindApplicationTests`): 1 context loading test

Run tests with:
```bash
mvn test
```

All tests use realistic mock data with major stock symbols (AAPL, GOOGL, MSFT, TSLA, AMZN, NVDA).
- Spring Boot 3.2.0
- Spring Data JPA
- H2 Database
- Maven
