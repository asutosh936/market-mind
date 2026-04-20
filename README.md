# Market Mind

Market Mind is a Spring Boot application for financial market analysis, leveraging both local OHLCV market data and external real-time feeds. The project exposes REST APIs for CRUD operations, symbol-based analytics, and Alpha Vantage data integration.

## Overview

- Spring Boot application with JPA, H2 database, and mockable services
- MarketData entity supports OHLCV (Open, High, Low, Close, Volume) data
- Pattern detection and AI-backed trading signal support
- Local database cache with Alpha Vantage fallback for real-time quotes, intraday data, and daily history
- Comprehensive JUnit test coverage for controllers, services, and repositories

## Getting Started

### Run the application

```bash
mvn clean package
mvn spring-boot:run
```

The application starts on `http://localhost:8080` by default.

### Configure Alpha Vantage API Key

**Option 1: Environment variable (recommended)**

```bash
export ALPHA_VANTAGE_API_KEY=your_api_key_here
mvn spring-boot:run
```

**Option 2: application.properties**

Edit `src/main/resources/application.properties`:

```properties
alpha-vantage.api-key=your_api_key_here
```

Get your free API key at: https://www.alphavantage.co/

## Base URL

All endpoints are served under:

```text
http://localhost:8080/market-mind/api/marketdata
```

## Core CRUD Endpoints

### 1. Get All Market Data

- Endpoint: `GET /api/marketdata`
- Description: Returns all MarketData entries stored in the local database.
- Optional query parameter: `includePatterns` (default: `true`)

**Curl**
```bash
curl -X GET http://localhost:8080/market-mind/api/marketdata
```

**Sample Success Response**
```json
{
  "success": true,
  "message": "Data retrieved successfully",
  "data": [
    {
      "id": 1,
      "symbol": "AAPL",
      "timestamp": "2026-04-18T10:00:00",
      "openPrice": 150.00,
      "highPrice": 155.00,
      "lowPrice": 149.00,
      "closePrice": 154.50,
      "volume": 1000000,
      "detectedPatterns": []
    }
  ],
  "count": 1
}
```

**Empty Response**
```json
{
  "success": true,
  "message": "No data found",
  "data": [],
  "count": 0
}
```

### 2. Get Market Data by ID

- Endpoint: `GET /api/marketdata/{id}`
- Description: Returns one MarketData entry by its numeric ID.
- Optional query parameter: `includePatterns` (default: `true`)

**Curl**
```bash
curl -X GET http://localhost:8080/market-mind/api/marketdata/1
```

**Sample Success Response**
```json
{
  "id": 1,
  "symbol": "AAPL",
  "timestamp": "2026-04-18T10:00:00",
  "openPrice": 150.0,
  "highPrice": 155.0,
  "lowPrice": 149.0,
  "closePrice": 154.5,
  "volume": 1000000,
  "detectedPatterns": []
}
```

**Not Found**
```text
HTTP/1.1 404 Not Found
```

### 3. Create Market Data

- Endpoint: `POST /api/marketdata`
- Description: Saves a new MarketData record in the local database.

**Curl**
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

**Sample Response**
```json
{
  "id": 1,
  "symbol": "AAPL",
  "timestamp": "2026-04-18T10:00:00",
  "openPrice": 150.0,
  "highPrice": 155.0,
  "lowPrice": 149.0,
  "closePrice": 154.5,
  "volume": 1000000
}
```

### 4. Update Market Data

- Endpoint: `PUT /api/marketdata/{id}`
- Description: Updates an existing record by ID.

**Curl**
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

**Sample Response**
```json
{
  "id": 1,
  "symbol": "AAPL",
  "timestamp": "2026-04-18T10:00:00",
  "openPrice": 150.0,
  "highPrice": 156.0,
  "lowPrice": 149.0,
  "closePrice": 155.5,
  "volume": 1100000
}
```

**Not Found**
```text
HTTP/1.1 404 Not Found
```

### 5. Delete Market Data

- Endpoint: `DELETE /api/marketdata/{id}`
- Description: Removes a record by ID.

**Curl**
```bash
curl -X DELETE http://localhost:8080/market-mind/api/marketdata/1
```

**Success**
```text
HTTP/1.1 204 No Content
```

**Not Found**
```text
HTTP/1.1 404 Not Found
```

## Alpha Vantage and Market Data Endpoints

The following endpoints support real-time quote retrieval and history lookups. When local cached data is available, the controller prefers the database and only falls back to the Alpha Vantage API when needed.

### 6. Test Pattern Demo Endpoint

- Endpoint: `GET /api/marketdata/test/patterns`
- Description: Creates synthetic market candles for a test Doji pattern, saves them to the local database, and returns the detected patterns.

**Curl**
```bash
curl -X GET http://localhost:8080/market-mind/api/marketdata/test/patterns
```

**Sample Response**
```json
{
  "marketData": {
    "symbol": "TEST_DOJI",
    "timestamp": "2026-04-18T10:00:00",
    "openPrice": 100.0,
    "highPrice": 110.0,
    "lowPrice": 90.0,
    "closePrice": 100.1,
    "volume": 100000
  },
  "detectedPatterns": [
    "Doji"
  ]
}
```

### 7. Get Real-Time Quote by Symbol

- Endpoint: `GET /api/marketdata/symbol/{symbol}`
- Description: Returns the latest quote for a symbol. It checks local cache first, then queries Alpha Vantage if no cached data exists.
- Optional query parameter: `includePatterns` (default: `true`)

**Curl**
```bash
curl -X GET http://localhost:8080/market-mind/api/marketdata/symbol/AAPL
```

**Sample Response**
```json
{
  "symbol": "AAPL",
  "timestamp": "2026-04-18T10:00:00",
  "openPrice": 150.0,
  "highPrice": 155.0,
  "lowPrice": 149.0,
  "closePrice": 154.5,
  "volume": 1000000,
  "detectedPatterns": []
}
```

**Not Found**
```text
HTTP/1.1 404 Not Found
```

### 8. Get Latest Intraday Bar

- Endpoint: `GET /api/marketdata/intraday/{symbol}`
- Description: Retrieves the latest 1-minute intraday bar from Alpha Vantage.
- Optional query parameter: `includePatterns` (default: `true`)

**Curl**
```bash
curl -X GET http://localhost:8080/market-mind/api/marketdata/intraday/AAPL
```

**Sample Response**
```json
{
  "symbol": "AAPL",
  "timestamp": "2026-04-18T16:00:00",
  "openPrice": 154.50,
  "highPrice": 155.00,
  "lowPrice": 154.00,
  "closePrice": 154.75,
  "volume": 500000,
  "detectedPatterns": []
}
```

**Not Found**
```text
HTTP/1.1 404 Not Found
```

### 9. Get Intraday History

- Endpoint: `GET /api/marketdata/intraday/{symbol}/history`
- Description: Retrieves intraday history from Alpha Vantage.

**Curl**
```bash
curl -X GET http://localhost:8080/market-mind/api/marketdata/intraday/AAPL/history
```

**Sample Response**
```json
{
  "success": true,
  "message": "Data retrieved successfully",
  "data": [
    {
      "id": null,
      "symbol": "AAPL",
      "timestamp": "2026-04-18T16:00:00",
      "openPrice": 154.50,
      "highPrice": 155.00,
      "lowPrice": 154.00,
      "closePrice": 154.75,
      "volume": 500000,
      "detectedPatterns": []
    }
  ],
  "count": 1
}
```

**Empty Response**
```json
{
  "success": false,
  "message": "No intraday history data available for symbol: AAPL",
  "data": [],
  "count": 0
}
```

### 10. Get Latest Daily Bar

- Endpoint: `GET /api/marketdata/daily/{symbol}`
- Description: Retrieves the latest daily OHLCV data from Alpha Vantage.

**Curl**
```bash
curl -X GET http://localhost:8080/market-mind/api/marketdata/daily/AAPL
```

**Sample Response**
```json
{
  "symbol": "AAPL",
  "timestamp": "2026-04-18T16:00:00",
  "openPrice": 150.00,
  "highPrice": 155.00,
  "lowPrice": 149.00,
  "closePrice": 154.50,
  "volume": 1000000,
  "detectedPatterns": []
}
```

**Not Found**
```text
HTTP/1.1 404 Not Found
```

### 11. Get Daily History

- Endpoint: `GET /api/marketdata/daily/{symbol}/history`
- Description: Retrieves the full daily time series for a symbol from Alpha Vantage.

**Curl**
```bash
curl -X GET http://localhost:8080/market-mind/api/marketdata/daily/AAPL/history
```

**Sample Response**
```json
{
  "success": true,
  "message": "Data retrieved successfully",
  "data": [
    {
      "id": null,
      "symbol": "AAPL",
      "timestamp": "2026-04-18T16:00:00",
      "openPrice": 150.00,
      "highPrice": 155.00,
      "lowPrice": 149.00,
      "closePrice": 154.50,
      "volume": 1000000,
      "detectedPatterns": []
    }
  ],
  "count": 1
}
```

**Empty Response**
```json
{
  "success": false,
  "message": "No daily history data available for symbol: AAPL",
  "data": [],
  "count": 0
}
```

### 12. Get Market Data by Symbol and Date Range

- Endpoint: `GET /api/marketdata/symbol/{symbol}/range`
- Query parameters: `start`, `end`, `includePatterns` (optional, default `true`)
- Description: Retrieves records from the local database between the requested timestamps.

**Curl**
```bash
curl -X GET "http://localhost:8080/market-mind/api/marketdata/symbol/AAPL/range?start=2026-04-18T09:00:00&end=2026-04-18T11:00:00"
```

**Sample Response**
```json
{
  "success": true,
  "message": "Data retrieved successfully",
  "data": [
    {
      "id": 2,
      "symbol": "AAPL",
      "timestamp": "2026-04-18T10:00:00",
      "openPrice": 150.00,
      "highPrice": 155.00,
      "lowPrice": 149.00,
      "closePrice": 154.50,
      "volume": 1000000,
      "detectedPatterns": []
    }
  ],
  "count": 1
}
```

**Invalid Range**
```text
HTTP/1.1 400 Bad Request
```

## Notes

- The controller supports `includePatterns=true|false` on most endpoints.
- When `includePatterns=true`, the service performs pattern detection and AI-backed trading signal analysis.
- Local database cache is used to reduce external API calls and preserve Alpha Vantage rate-limited usage.

## Testing

Run the test suite with:

```bash
mvn test
```

This project includes controller, service, and repository tests covering the main data and endpoint flows.
