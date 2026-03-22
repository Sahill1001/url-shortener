# System Design & Architecture

## High-Level System Architecture

```
┌────────────────────────────────────────────────────────────────────┐
│                         Client Layer                               │
│                   (Web Browsers, Mobile Apps, APIs)               │
└────────────────────────┬─────────────────────────────────────────┘
                         │
┌────────────────────────▼─────────────────────────────────────────┐
│                    Load Balancer / CDN                            │
│    (Nginx, HAProxy, CloudFlare, or CloudFront)                    │
└────────────────────────┬─────────────────────────────────────────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
┌───────▼──────┐  ┌─────▼──────┐  ┌─────▼──────┐
│  App Server  │  │ App Server │  │ App Server │
│   Instance   │  │  Instance  │  │  Instance  │
│   :8080      │  │   :8080    │  │   :8080    │
│              │  │            │  │            │
│ REST API     │  │ REST API   │  │ REST API   │
│ Controllers  │  │ Controllers│  │ Controllers│
└───┬──────┬───┘  └──┬──────┬──┘  └──┬──────┬──┘
    │      │        │      │        │      │
┌───▼──────▼┬─────┬─▼──────▼┬─────┬▼──────▼───┐
│  Services │     │ Cache   │     │  Kafka    │
│  & Logic  │     │(Redis)  │     │ Producer  │
└───┬───────┴─────┴─────────┴─────┴───────────┘
    │
    └──────────────────────────────────┬──────────────────────┐
                                       │                      │
                              ┌────────▼────────┐  ┌──────────▼────────┐
                              │   PostgreSQL    │  │  Kafka Cluster    │
                              │   (Primary DB)  │  │   (Event Stream)  │
                              │                 │  │                   │
                              │ ┌─────────────┐ │  │ ┌──────────────┐ │
                              │ │   Master    │ │  │ │   Broker 1   │ │
                              │ └─────────────┘ │  │ │   Broker 2   │ │
                              │ ┌─────────────┐ │  │ │   Broker 3   │ │
                              │ │  Replicas   │ │  │ └──────────────┘ │
                              │ └─────────────┘ │  │                   │
                              └─────────────────┘  │ ┌──────────────┐  │
                                                   │ │  Consumer    │  │
                                                   │ │  (Analytics) │  │
                                                   │ └──────────────┘  │
                                                   └───────────────────┘
```

---

## Component Breakdown

### 1. REST API Controllers
**File:** `RedirectController.java`, `UrlApiController.java`

**Purpose:** 
- Handle HTTP requests
- Route to appropriate services
- Extract request data
- Format responses

**Responsibilities:**
- `POST /api/v1/urls` - Create shortened URL
- `GET /{shortCode}` - Redirect
- `GET /api/v1/urls/{shortCode}/stats` - Get statistics
- `DELETE /api/v1/urls/{shortCode}` - Delete URL

**Performance Characteristics:**
- Request processing: <1ms
- Response serialization: <1ms

---

### 2. Service Layer
**File:** `UrlService.java`

**Purpose:**
- Implement business logic
- Orchestrate components
- Handle validation
- Manage transactions

**Key Methods:**
```
shortenUrl(request, clientIp)
├── Check rate limit (Redis)
├── Generate unique short code (Snowflake + Base62)
├── Persist to database
├── Cache result
└── Return response

getOriginalUrl(shortCode, clientIp, userAgent)
├── Check cache first (Redis)
├── If miss, query database
├── Validate expiration
├── Publish event (Kafka)
├── Update cache
└── Return URL
```

**Design Pattern:** Dependency Injection
- All dependencies injected via constructor
- Easy to test and mock
- Follows Single Responsibility Principle

---

### 3. Caching Layer (Redis)
**File:** `UrlCacheManager.java`

**Purpose:**
- Reduce database queries
- Improve response times
- Provide temporary storage

**Cache Key Structure:**
```
url:{shortCode}           → Url entity (JSON)
url:{shortCode}:clicks    → Click counter
ratelimit:{ipAddress}     → Request count
```

**TTL (Time-To-Live):**
- URLs: 24 hours (configurable)
- Rate limits: 1 minute
- Click counters: 24 hours

**Cache Strategy:**
- **Cache-Aside Pattern**: Check cache, if miss query DB
- **Lazy Loading**: Populate cache on first read
- **Eviction Policy**: LRU (Least Recently Used)
- **Fail-Open**: If Redis down, fall back to database

**Performance Impact:**
- Cache hit: ~1-2ms (vs 30-50ms database)
- Hit rate: 90-95% after warmup
- Effective latency reduction: 95%

---

### 4. Database Layer (PostgreSQL)
**File:** `UrlRepository.java`, `UrlAnalyticsRepository.java`

**Tables:**

#### `urls` Table
```sql
CREATE TABLE urls (
    id BIGINT PRIMARY KEY,                    -- Snowflake ID
    short_code VARCHAR(10) UNIQUE NOT NULL,   -- Base62 encoded
    original_url TEXT NOT NULL,               -- Full URL
    created_at TIMESTAMP DEFAULT NOW(),       -- Creation timestamp
    expires_at TIMESTAMP,                     -- Optional expiration
    click_count BIGINT DEFAULT 0              -- Analytics counter
);
```

**Indexes:**
```sql
idx_urls_short_code      -- Fast lookup by short code
idx_urls_expires_at      -- Identify expired URLs
idx_urls_created_at      -- Time-based queries
```

#### `analytics` Table
```sql
CREATE TABLE analytics (
    id BIGSERIAL PRIMARY KEY,
    short_code VARCHAR(10) NOT NULL,
    click_time TIMESTAMP NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT
);
```

**Characteristics:**
- Write-optimized (for click events)
- Append-only pattern
- High query volume expected
- Can be partitioned by date for scalability

**Query Patterns:**
- Write: ~5-10ms (Kafka consumer batch)
- Read: ~30-50ms (single query)
- Aggregation: ~100-500ms (analytics)

---

### 5. Rate Limiting (Redis)
**File:** `RateLimiter.java`

**Algorithm:** Token Bucket (simplified)

**Configuration:**
- **Limit**: 100 requests per IP per minute
- **Window**: 1 minute
- **Storage**: Redis (distributed)

**Implementation:**
```
Per IP:
1. INCR key (increment counter)
2. If first request, SET TTL (1 minute)
3. If counter > 100:
   → Throw RateLimitExceededException
4. Otherwise, allow request
```

**Benefits:**
- Simple to understand
- Distributed (works with multiple instances)
- Memory efficient
- Fair (per-IP tracking)

---

### 6. ID Generation (Snowflake)
**File:** `SnowflakeIdGenerator.java`

**Problem Solved:**
- Need globally unique IDs
- UUID is too long (36+ chars) → Bad for URLs
- Database sequences don't work distributed

**Solution: Twitter Snowflake Algorithm**

```
64-bit ID Structure:
┌─────┬─────────────┬─────┬──────┬────────┐
│  1  │  41 bits    │  5  │  5   │  12    │
├─────┼─────────────┼─────┼──────┼────────┤
│Sign │ Timestamp   │ DC  │Mach  │Sequence│
│     │ (ms)        │ ID  │ ID   │        │
└─────┴─────────────┴─────┴──────┴────────┘
```

**Properties:**
- **Unique**: Across all instances globally
- **Monotonic**: Time-ordered (good for indexing)
- **Distributed**: No central coordination
- **Fast**: Generated in microseconds
- **Compact**: 64-bit = ~18 digit decimal

**Capacity:**
- Timestamp: 41 bits = ~69 years
- Datacenters: 5 bits = 32 centers
- Machines: 5 bits = 32 machines per datacenter
- Sequence: 12 bits = 4096 IDs per millisecond

**Max Throughput:**
- Per node: 4,096 IDs/ms = 4.1M IDs/sec
- Cluster (32 nodes): ~131M IDs/sec

---

### 7. URL Encoding (Base62)
**File:** `Base62Encoder.java`

**Problem:**
- JSON/database stores 64-bit numbers as 20 decimal digits
- Decimal too long for URLs: "1609459200000" (13 chars)
- Solution: Convert to Base62 (0-9, A-Z, a-z)

**Conversion Algorithm:**
```
Base62 has 62 symbols (0-9, A-Z, a-z)
- Divide ID by 62 repeatedly
- Map remainders to symbols
- Result: Short alphanumeric code

Example:
ID: 1234567890
÷62: Quotient 19908094, Remainder 22 (W)
÷62: Quotient 321099,   Remainder 0 (0)
...
Result: "1LY7VK" (6 chars vs 10 decimal)
```

**Advantages:**
- Shorter URLs (6-10 chars typical)
- Human-readable
- Case-sensitive (62 vs 36 symbols)
- URL-safe (no special chars)

**Collision Resistance:**
- Base62^10 = ~8.4 trillion combinations
- Sufficient for years at high scale

---

### 8. Kafka Event Streaming
**Files:** `UrlClickEventProducer.java`, `UrlClickEventConsumer.java`

**Purpose:**
- Decouple redirect logic from analytics
- Enable asynchronous processing
- Provide scalable event distribution

**Event Flow:**
```
Redirect Request
    ↓
Publish to Kafka (Non-blocking, ~1ms)
    ↓
Return 301 Immediately
    ↓
Kafka Consumer (Async)
    │
    ├─ Parse event
    ├─ Store to DB (~5-10ms)
    └─ Update analytics
```

**Event Schema:**
```json
{
  "short_code": "abc123xyz",
  "timestamp": 1711098600000,
  "ip_address": "192.168.1.1",
  "user_agent": "Mozilla/5.0..."
}
```

**Topic Configuration:**
- **Topic**: `url-click-events`
- **Partitions**: 3 (scalable)
- **Replication Factor**: 3 (durable)
- **Retention**: 7 days
- **Compression**: snappy

**Consumer Configuration:**
- **Group ID**: `url-shortener-analytics`
- **Concurrency**: 3 consumers
- **Batch Size**: 100 messages
- **Auto-commit**: Disabled (manual commits)

**Benefits:**
- **Resilience**: If DB is slow, events queue
- **Scalability**: Multiple consumers process events
- **Decoupling**: Analytics independent from API
- **Durability**: Messages persisted on disk

---

## Request Processing Flows

### Flow 1: Create Shortened URL

```
POST /api/v1/urls
{
  "originalUrl": "https://example.com/very/long/path",
  "customShortCode": "mylink"
}

Step 1: Extract Request (UrlApiController)
  ↓
Step 2: Rate Limit Check (RateLimiter)
  ├─ Key: ratelimit:192.168.1.1
  ├─ INCR count
  └─ If > 100: Reject with 429

Step 3: Validate URL (Validation)
  ├─ Check format (regex pattern)
  └─ Reject if invalid: 400

Step 4: Generate Short Code (SnowflakeIdGenerator)
  ├─ If custom: Use provided
  └─ Else: Generate unique via Snowflake

Step 5: Persist to Database (UrlRepository)
  ├─ INSERT into urls table
  ├─ Set ID, short_code, original_url, created_at
  └─ Return created entity

Step 6: Cache (UrlCacheManager)
  ├─ SET url:{shortCode} = Url (JSON)
  ├─ TTL: 24 hours
  └─ Ignore cache failures

Step 7: Response (UrlApiController)
  ├─ Build CreateUrlResponse
  └─ Return 201 CREATED

Total Latency: 5-20ms
  - Database write: 5-10ms
  - Cache write: 1-2ms
  - Serialization: 1-2ms
```

### Flow 2: Redirect to Original URL (Critical Path)

```
GET /abc123xyz

Step 1: Extract Short Code (RedirectController)
  ↓
Step 2: Extract Client IP (IpAddressExtractor)
  ├─ Check X-Forwarded-For
  ├─ Check X-Real-IP
  └─ Fall back to remote addr

Step 3: Check Cache (UrlCacheManager)
  ├─ Key: url:{shortCode}
  ├─ Hit: Return Url (1-2ms) ✓ FAST PATH
  └─ Miss: Continue to DB (5-10ms)

Step 4: Query Database (UrlRepository)
  ├─ SELECT * FROM urls WHERE short_code = ?
  ├─ Use index: FAST
  └─ Return Url entity

Step 5: Check Expiration (UrlService)
  ├─ If expires_at < NOW()
  └─ Throw UrlExpiredException (410)

Step 6: Publish Event (UrlClickEventProducer)
  ├─ Create UrlClickEvent
  ├─ Send to Kafka topic
  └─ Return immediately (async, ~1ms)

Step 7: Cache Update (UrlCacheManager)
  ├─ Cache missing URL (for future hits)
  ├─ INCR url:{shortCode}:clicks
  └─ Ignore failures (fail-open)

Step 8: Return Redirect (RedirectController)
  ├─ HTTP 301 Moved Permanently
  ├─ Location: original_url
  └─ Empty body

Total Latency:
  - Cache Hit: ~2-5ms (P99: <10ms) ✓
  - Cache Miss: ~50-80ms (P99: <100ms)
  - Average: ~8-15ms (90%+ cache hit)
```

### Flow 3: Analytics Processing (Asynchronous)

```
Kafka Consumer (url-shortener-analytics group)

Loop:
  ├─ Poll messages (batch: 100)
  ├─ Process each UrlClickEvent:
  │   ├─ Create UrlAnalytics entity
  │   ├─ Set click_time, ip_address, user_agent
  │   └─ INSERT into analytics table
  ├─ Batch commit offsets
  └─ Continue loop

Latency: 100-500ms for batch
  - Network: ~10ms
  - Database insert: ~400ms (100 records)
  - Offset commit: ~50ms

Non-blocking: Doesn't affect redirect latency
```

---

## Performance Optimization Techniques

### 1. Connection Pooling
```yaml
# HikariCP Configuration (PostgreSQL)
maximum-pool-size: 20  # Max concurrent connections
minimum-idle: 5        # Min idle connections
connection-timeout: 20s # Wait time for connection
idle-timeout: 5m       # Idle connection timeout
max-lifetime: 20m      # Connection max age
```

### 2. Database Query Optimization
```sql
-- Index for fast short code lookup
CREATE INDEX idx_urls_short_code ON urls(short_code);

-- Index for expiration check
CREATE INDEX idx_urls_expires_at ON urls(expires_at);

-- Query optimization
SELECT * FROM urls WHERE short_code = ? USING INDEX
-- Uses index: Cost ~0.3ms vs ~50ms full table scan
```

### 3. Network Optimization
```
- Keep-Alive: HTTP/1.1 persistent connections
- Compression: Gzip for responses > 1KB
- Content-Type: application/json (lightweight)
- TCP Tuning: Reduce network latency
```

### 4. Serialization
```
- JSON: Human-readable, ~300 bytes per URL
- Protocol Buffers: More efficient, but less human-readable
- Trade-off: Chose JSON for simplicity
```

### 5. Async Processing
```
- Kafka: Non-blocking analytics publish (~1ms)
- Thread Pool: Parallel event processing
- Batch: Process multiple events together
```

---

## Scalability Strategies

### Vertical Scaling (Single Instance)
- **CPU**: Increase thread pool size
- **RAM**: Cache more URLs in Redis
- **Storage**: Use SSD for database
- **Network**: Upgrade bandwidth

### Horizontal Scaling (Multiple Instances)

#### 1. Application Servers
```
Load Balancer (Round-robin)
    ↓
├─ API Server 1 :8080
├─ API Server 2 :8080
├─ API Server 3 :8080
└─ API Server N :8080

Benefits:
- 3x throughput
- Load distribution
- High availability
- No server affinity needed (stateless)
```

#### 2. Database
```
PostgreSQL Setup:
├─ Primary (Master) - Write operations
├─ Replicas (Read-only) - Read operations
│   ├─ Replica 1 - Analytics read
│   ├─ Replica 2 - Stats read
│   └─ Replica 3 - Backup
└─ Standby - Failover

Sharding (Future):
├─ Shard 0: short_code a-m
├─ Shard 1: short_code n-z
└─ Shard 2: short_code 0-9

Benefits: Distribute data & load
```

#### 3. Redis Clustering
```
Redis Cluster:
├─ Node 1 (Slots 0-5460)
├─ Node 2 (Slots 5461-10922)
├─ Node 3 (Slots 10923-16383)
└─ + Replicas for each

Benefits:
- Store more data
- Handle more requests
- Auto-failover
```

#### 4. Kafka Scaling
```
Kafka Cluster (3+ brokers):
├─ Broker 1
├─ Broker 2
└─ Broker 3

Topic: url-click-events
├─ Partitions: 3+ (parallel processing)
├─ Replication: 3 (durability)
└─ Consumers: 3+ (parallelism)
```

---

## Data Flow Diagrams

### Shortening Flow
```
INPUT     → VALIDATION → ID GEN → ENCODING → DB WRITE → CACHE → OUTPUT
1000 req  →  100 req    → 100 req → 100 req  → 100 req  → 100req → 100req
/sec      → (reject)    /sec     /sec       /sec       /sec    /sec
```

### Redirect Flow
```
INPUT     → IP EXTRACT → CACHE CHECK → EVENT PUBLISH → OUTPUT
10K req   → 10K req     → 9.5K hits   → 10K msg       → 10K redir
/sec      → /sec        → 500 miss    → /sec (async)  → /sec
                        → DB QUERY
                        → 500 req/sec
```

---

## Capacity Planning

### Storage Requirements

```
Per 1 Million URLs:
├─ PostgreSQL (urls table):
│  ├─ Fixed: 24 bytes per URL (ID, timestamp, flags)
│  ├─ Variable: ~100 bytes (average URL length)
│  └─ Total: ~124 bytes per URL = 124 MB + indexes

├─ Analytics (1M URLs × 10 clicks each = 10M records):
│  ├─ Per record: ~40 bytes
│  └─ Total: 400 MB

├─ Redis Cache (Hot set ~10% of active URLs):
│  ├─ Cached URLs: 100K × 300 bytes = 30 MB
│  └─ Counters: 100K × 8 bytes = 1 MB

├─ Kafka (7-day retention):
│  ├─ Events: 10M/day × 7 = 70M events
│  ├─ Per event: ~200 bytes (JSON)
│  └─ Total: ~14 GB

Total Disk: ~600 MB (1M URLs + 7-day analytics + Kafka)
```

### Memory Requirements

```
Per Application Instance:
├─ JVM Base: 256 MB
├─ Spring Framework: 100 MB
├─ Connection Pools: 50 MB
│  ├─ PostgreSQL (20 conns): 20 × 2MB = 40MB
│  └─ Redis: Negligible
├─ Caches & Buffers: 100 MB
└─ Total: ~500 MB minimum, 1-2 GB recommended

Per Redis Instance:
├─ Metadata: 10 MB
├─ Data (1M URLs): 30 MB
└─ Total: ~100 MB minimum, 512 MB recommended
```

### CPU/Throughput

```
Per CPU Core:
├─ HTTP Request Processing: ~1,000-5,000 req/sec
├─ Redis Lookups: ~100,000 req/sec (mostly network bound)
├─ Database: ~100-500 req/sec (mostly I/O bound)

Recommendations:
├─ Small Scale (< 100K URLs): 1-2 cores
├─ Medium Scale (100K-1M URLs): 4 cores
├─ Large Scale (1M-10M URLs): 8-16 cores
└─ High Scale (> 10M URLs): Distribute across nodes
```

---

## Monitoring Key Metrics

```
Application:
├─ Request Rate (req/sec)
├─ Response Time (P50, P95, P99)
├─ Error Rate (%)
├─ Cache Hit Rate (%)
└─ Throughput (Mbps)

Database:
├─ Connection Pool Usage (%)
├─ Query Latency (ms)
├─ Slow Queries (> 100ms)
├─ Disk Usage (GB)
└─ Replication Lag (sec)

Redis:
├─ Cache Hit Rate (%)
├─ Memory Usage (MB)
├─ Evictions (per sec)
├─ Command Latency (ms)
└─ Connected Clients

Kafka:
├─ Message Throughput (msg/sec)
├─ Consumer Lag (messages)
├─ Broker CPU (%)
├─ Network I/O (Mbps)
└─ Replication Status
```

---

**Created:** March 2026
**Version:** 1.0
**Status:** Production-Ready
