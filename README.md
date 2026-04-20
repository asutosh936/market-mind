# Market Mind

Market Mind is a Spring Boot application for financial market analysis, leveraging market data. This project provides RESTful APIs to manage and analyze market data for various financial instruments.

## Phase I Implementation

- Basic Spring Boot setup with JPA and H2 database
- MarketData entity with OHLCV (Open, High, Low, Close, Volume) data
- CRUD operations via REST API
- In-memory H2 database for development
- Built-in monitoring/logging support for startup, request handling, and persistence events
- Comprehensive JUnit test coverage (32 tests covering controller, service, and repository layers)

### Phase I API Endpoints

#### 1. Get All Market Data
**Endpoint:** `GET /market-mind/api/marketdata`  
**Description:** Retrieves a list of all market data entries stored in the database.  
**Curl Command:**
```bash
curl -X GET http://localhost:8080/market-mind/api/marketdata
```
**Response:** ApiResponse object containing success status, message, data array, and count.
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
      "volume": 1000000
    }
  ],
  "count": 1
}
```
**Empty Response:**
```json
{
  "success": true,
  "message": "No data found",
  "data": [],
  "count": 0
}
```

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

## Phase II Implementation - Alpha Vantage Integration

Phase II focuses on **real-time market data via Alpha Vantage API** integration while maintaining local database caching.

### Features
- **Real-time data** from Alpha Vantage API leveraging free tier
- **Three data types**: QUOTE_ENDPOINT (real-time), INTRADAY (1-minute), DAILY (daily OHLCV)
- **Smart caching** to handle rate limits (5 requests/minute free tier limit)
- **Hybrid approach**: Database-first lookup, falls back to Alpha Vantage API
- **Automatic response caching** for 1 minute to minimize API calls
- **Full historical data support** for intraday and daily queries

### Configuration

#### Set Your Alpha Vantage API Key

**Option 1: Via Environment Variable (Recommended)**
```bash
export ALPHA_VANTAGE_API_KEY=your_api_key_here
mvn spring-boot:run
```

**Option 2: Via application.properties**
Edit `src/main/resources/application.properties`:
```properties
alpha-vantage.api-key=your_api_key_here
```

Get your free API key at: https://www.alphavantage.co/

### Phase II API Endpoints

#### 6. Get Real-Time Quote (Alpha Vantage QUOTE_ENDPOINT)
**Endpoint:** `GET /market-mind/api/marketdata/symbol/{symbol}`  
**Description:** Fetches real-time stock quote from Alpha Vantage. First checks local cache (database), then queries Alpha Vantage.  
**Curl Command:**
```bash
curl -X GET http://localhost:8080/market-mind/api/marketdata/symbol/AAPL
```
**Response:** Latest MarketData object with current market conditions.
**Rate Limit:** Cached for 1 minute to respect free tier limits.

#### 7. Get Latest Intraday 1-Minute Bar (Alpha Vantage TIME_SERIES_INTRADAY)
**Endpoint:** `GET /market-mind/api/marketdata/intraday/{symbol}`  
**Description:** Fetches the most recent 1-minute bar data from Alpha Vantage.  
**Curl Command:**
```bash
curl -X GET http://localhost:8080/market-mind/api/marketdata/intraday/AAPL
```
**Response:** Latest intraday MarketData with timestamp and OHLCV.
**Interval:** 1-minute bars with real-time precision.

#### 8. Get Intraday History (Alpha Vantage TIME_SERIES_INTRADAY Full)
**Endpoint:** `GET /market-mind/api/marketdata/intraday/{symbol}/history`  
**Description:** Retrieves full historical intraday data (up to 100 data points) from Alpha Vantage.  
**Curl Command:**
```bash
curl -X GET http://localhost:8080/market-mind/api/marketdata/intraday/AAPL/history
```
**Response:** ApiResponse object with array of intraday MarketData records ordered by timestamp.
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
      "volume": 500000
    }
  ],
  "count": 1
}
```
**Empty Response:**
```json
{
  "success": false,
  "message": "No intraday history data available for symbol: AAPL",
  "data": [],
  "count": 0
}
```

#### 9. Get Latest Daily Bar (Alpha Vantage TIME_SERIES_DAILY)
**Endpoint:** `GET /market-mind/api/marketdata/daily/{symbol}`  
**Description:** Fetches the latest daily OHLCV data from Alpha Vantage.  
**Curl Command:**
```bash
curl -X GET http://localhost:8080/market-mind/api/marketdata/daily/AAPL
```
**Response:** Latest daily MarketData.

#### 10. Get Daily History (Alpha Vantage TIME_SERIES_DAILY Full)
**Endpoint:** `GET /market-mind/api/marketdata/daily/{symbol}/history`  
**Description:** Retrieves full daily data from Alpha Vantage with `outputsize=full` parameter.  
**Curl Command:**
```bash
curl -X GET http://localhost:8080/market-mind/api/marketdata/daily/AAPL/history
```
**Response:** ApiResponse object with array of all available daily MarketData.
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
      "volume": 1000000
    }
  ],
  "count": 1
}
```
**Empty Response:**
```json
{
  "success": false,
  "message": "No daily history data available for symbol: AAPL",
  "data": [],
  "count": 0
}
```

#### 11. Get Market Data by Symbol and Date Range (Database Query)
**Endpoint:** `GET /market-mind/api/marketdata/symbol/{symbol}/range?start={start}&end={end}`  
**Description:** Retrieves market data from local database for a symbol between two timestamps. (Original Phase II endpoint, database-backed)  
**Curl Command:**
```bash
curl -X GET "http://localhost:8080/market-mind/api/marketdata/symbol/AAPL/range?start=2026-04-18T09:00:00&end=2026-04-18T11:00:00"
```
**Response:** ApiResponse object containing success status, message, data array, and count.
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
      "volume": 1000000
    }
  ],
  "count": 1
}
```
**Empty Response:**
```json
{
  "success": true,
  "message": "No data found",
  "data": [],
  "count": 0
}
```

## Phase IV Implementation - Pattern Detection and AI Trading Confidence

Phase IV extends existing endpoints to return detected candlestick patterns alongside trading confidence, without introducing new API routes.

### What Phase IV Adds
- Existing endpoints now include:
  - `detectedPatterns`: identified candlestick patterns for the latest market candle
  - `tradingSignal`: buy/sell/hold recommendation with an AI-informed confidence score and reasoning
- No new endpoints were added: the enhancement is purely in response payloads.
- Pattern analysis and trading signal generation are enabled by default via `includePatterns=true`.
- Use `includePatterns=false` to disable analysis and return `tradingSignal: null`.
- If OpenAI is unavailable, the app falls back to deterministic rule-based signal generation.

### Phase IV Curl Examples

#### Get symbol quote with pattern analysis and trading confidence
```bash
curl -X GET "http://localhost:8080/market-mind/api/marketdata/symbol/AAPL"
```

#### Get symbol quote without pattern analysis
```bash
curl -X GET "http://localhost:8080/market-mind/api/marketdata/symbol/AAPL?includePatterns=false"
```

#### Get all market data with pattern analysis
```bash
curl -X GET "http://localhost:8080/market-mind/api/marketdata?includePatterns=true"
```

### Phase IV Response Example
```json
{
  "id": 1,
  "symbol": "AAPL",
  "timestamp": "2026-04-18T16:00:00",
  "openPrice": 150.0,
  "highPrice": 155.0,
  "lowPrice": 149.0,
  "closePrice": 154.5,
  "volume": 1000000,
  "detectedPatterns": [
    {
      "name": "Bullish Engulfing",
      "type": "BULLISH_ENGULFING",
      "confidence": 0.92,
      "description": "Bullish Engulfing pattern detected with strong momentum"
    }
  ],
  "tradingSignal": {
    "signal": "BUY",
    "confidence": 78.0,
    "reasoning": "Bullish reversal pattern with strong momentum suggests a buy opportunity"
  }
}
```

### AI Setup for Phase IV

Set your OpenAI API key before starting the application:
```bash
export OPENAI_API_KEY=your-openai-api-key-here
```

The application can also be configured via `src/main/resources/application.properties`.

### Phase IV Notes

- `includePatterns=true` is the default.
- A missing or invalid OpenAI key does not break the endpoint; the app will return a fallback `tradingSignal`.
- Existing endpoints remain unchanged; only the response payload is enhanced.

### Caching Strategy

- **Quote Data**: Cached for 1 minute
- **Intraday Data**: Cached for 1 minute
- **Daily Data**: Cached for 1 minute
- Fallback to MySQL/database if API unavailable
- Responses automatically saved to H2 for reference

### Architecture

```
Request → Controller 
         → Check H2 Cache (for symbol queries)
         → If no cache: Call Alpha Vantage Service
         → AlphaVantageService (with RestTemplate)
         → Deserialize JSON to DTOs
         → Convert to MarketData entity
         → Save to H2 and return
         → Cache for 1 minute
```

### API Response Format

All list-returning endpoints now use a standardized `ApiResponse<T>` wrapper format:

```json
{
  "success": true|false,
  "message": "Description of the response",
  "data": [ /* Array of requested objects */ ],
  "count": 0 /* Number of items in data array */
}
```

- **success**: Boolean indicating if the operation was successful
- **message**: Human-readable message describing the result
- **data**: Array containing the actual data (empty array when no data found)
- **count**: Total number of items in the data array

This format provides consistent responses across all endpoints, including appropriate messages when no data is available.

## Getting Started

### Prerequisites
- Java 17
- Maven 3.6+

### Running the Application
1. Clone the repository
2. Navigate to the project directory
3. Run `mvn spring-boot:run`

The application will start on port 8080.

## Database Setup and Access

### H2 Database Configuration

Market Mind uses **H2 in-memory database** for development and testing. The database is automatically configured and initialized when the application starts.

#### Default Configuration
- **Database URL:** `jdbc:h2:mem:testdb`
- **Username:** `sa`
- **Password:** `password`
- **Driver:** `org.h2.Driver`
- **JPA Dialect:** `org.hibernate.dialect.H2Dialect`
- **DDL Auto:** `create-drop` (recreates schema on each restart)

#### Configuration Location
Database settings are configured in `src/main/resources/application.properties`:

```properties
# H2 Database configuration
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=password
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.h2.console.enabled=true
spring.jpa.hibernate.ddl-auto=create-drop
server.servlet.context-path=/market-mind

# Alpha Vantage API Configuration
alpha-vantage.api-key=${ALPHA_VANTAGE_API_KEY:demo}

# Cache Configuration (60 seconds default TTL)
spring.cache.type=simple
spring.cache.cache-names=alphaVantageQuote,alphaVantageIntraDay,alphaVantageDaily
```

### Accessing H2 Console

The H2 web console is enabled by default and provides a web-based interface to interact with the database.

#### Steps to Access H2 Console:

1. **Start the Application:**
   ```bash
   mvn spring-boot:run
   ```

2. **Open Browser and Navigate to:**
   ```
   http://localhost:8080/market-mind/h2-console
   ```

3. **Login Credentials:**
   - **JDBC URL:** `jdbc:h2:mem:testdb`
   - **Username:** `sa`
   - **Password:** `password`

4. **Connect:** Click the "Connect" button to access the database.

#### H2 Console Features

- **Browse Tables:** View all tables including `MARKET_DATA`
- **Run SQL Queries:** Execute custom SQL queries directly
- **Export Data:** Export table data to CSV or SQL files
- **View Schema:** Inspect table structure and constraints

#### Example Queries

Once connected, you can run queries like:

```sql
-- View all market data
SELECT * FROM MARKET_DATA;

-- Count records by symbol
SELECT SYMBOL, COUNT(*) FROM MARKET_DATA GROUP BY SYMBOL;

-- Find latest records
SELECT * FROM MARKET_DATA ORDER BY TIMESTAMP DESC LIMIT 10;
```

### Database Schema

The `MARKET_DATA` table is automatically created with the following structure:

| Column | Type | Description |
|--------|------|-------------|
| `ID` | BIGINT | Primary key (auto-generated) |
| `SYMBOL` | VARCHAR(255) | Stock symbol (e.g., AAPL, GOOGL) |
| `TIMESTAMP` | TIMESTAMP | Date and time of the market data |
| `OPEN_PRICE` | DOUBLE | Opening price |
| `HIGH_PRICE` | DOUBLE | Highest price |
| `LOW_PRICE` | DOUBLE | Lowest price |
| `CLOSE_PRICE` | DOUBLE | Closing price |
| `VOLUME` | BIGINT | Trading volume |

### Data Persistence Notes

- **In-Memory Database:** Data is lost when the application restarts
- **Alpha Vantage Integration:** API responses are automatically saved to the database
- **CRUD Operations:** Manual data entry via POST endpoints is also supported
- **Caching:** Recent API responses are cached in memory for 1 minute

### API Endpoints Summary

**Database Operations:**
- `GET /market-mind/api/marketdata` - Get all market data from database
- `GET /market-mind/api/marketdata/{id}` - Get market data by ID from database
- `POST /market-mind/api/marketdata` - Create new market data in database
- `PUT /market-mind/api/marketdata/{id}` - Update market data in database
- `DELETE /market-mind/api/marketdata/{id}` - Delete market data from database
- `GET /market-mind/api/marketdata/symbol/{symbol}/range?start={start}&end={end}` - Query database by symbol and date range

**Alpha Vantage Real-Time Integration:**
- `GET /market-mind/api/marketdata/symbol/{symbol}` - Get real-time quote (cached, falls back to DB)
- `GET /market-mind/api/marketdata/intraday/{symbol}` - Get latest 1-minute intraday bar
- `GET /market-mind/api/marketdata/intraday/{symbol}/history` - Get full intraday history
- `GET /market-mind/api/marketdata/daily/{symbol}` - Get latest daily close
- `GET /market-mind/api/marketdata/daily/{symbol}/history` - Get full daily history

### H2 Console
Access the H2 database console at `http://localhost:8080/market-mind/h2-console` when the application is running.

## Testing

The application includes comprehensive JUnit 5 test coverage:

- **Controller Tests** (`MarketDataControllerTest`): 11 integration tests for REST endpoints using MockMvc
- **Service Tests** (`MarketDataServiceTest`): 10 unit tests for business logic
- **Repository Tests** (`MarketDataRepositoryTest`): 10 integration tests for data access layer
- **Application Tests** (`MarketMindApplicationTests`): 1 context loading test
- **Total:** 32 tests covering both Phase I CRUD and Phase II Alpha Vantage integration

Run tests with:
```bash
mvn test
```

All tests use realistic mock data with major stock symbols (AAPL, GOOGL, MSFT, TSLA, AMZN, NVDA).

## Technology Stack

**Phase I & II:**
- **Java:** 17 (LTS)
- **Framework:** Spring Boot 3.2.0
- **ORM:** Spring Data JPA / Hibernate
- **Database:** H2 (in-memory)
- **HTTP Client:** Spring RestTemplate
- **Caching:** Spring Cache (ConcurrentHashMap)
- **JSON Processing:** Jackson
- **Logging:** SLF4J with Logback
- **Testing:** JUnit 5, Spring Boot Test, MockMvc
- **Build:** Maven 3.6+

**External Integration:**
- **API:** Alpha Vantage (Free Tier)
- **Rate Limit:** 5 requests/minute (handled via caching)
