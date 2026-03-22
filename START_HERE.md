# URL Shortener - Quick Start Guide

Welcome to the Production-Grade Distributed URL Shortener! This guide will get you up and running in minutes.

## 🚀 Quick Start (5 minutes)

### Prerequisites
- Docker & Docker Compose installed
- Java 21+ (optional, Docker handles it)
- Git (to clone the repo)

### Step 1: Start Services

```bash
cd url-shortener
docker-compose up -d
```

**Expected Output:**
```
Creating url-shortener-postgres ... done
Creating url-shortener-redis ... done
Creating url-shortener-zookeeper ... done
Creating url-shortener-kafka ... done
Creating url-shortener-kafka-ui ... done
```

Wait for all services to show as "healthy":
```bash
docker-compose ps
```

### Step 2: Build & Run Application

```bash
# Build
mvn clean package -DskipTests

# Run
mvn spring-boot:run

# Or use Java directly
java -jar target/url-shortener-0.0.1-SNAPSHOT.jar
```

**Expected Output:**
```
Started UrlShortenerApplication in 3.454 seconds
```

### Step 3: Create Your First Short URL

```bash
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -d '{
    "originalUrl": "https://github.com/sahiljuneja",
    "customShortCode": "myprofile"
  }'
```

**Response:**
```json
{
  "shortUrl": "http://localhost:8080/myprofile",
  "shortCode": "myprofile",
  "originalUrl": "https://github.com/sahiljuneja",
  "createdAt": "2024-03-22T10:30:00",
  "expiresAt": null
}
```

### Step 4: Test the Redirect

```bash
# Follow the redirect
curl -L http://localhost:8080/myprofile

# Or visit in browser
# http://localhost:8080/myprofile
```

**Result:** You're redirected to the original GitHub URL! ✅

---

## 📁 Project Structure

```
url-shortener/
├── README.md                    # Full documentation
├── API_EXAMPLES.md              # API usage examples
├── SYSTEM_DESIGN.md             # Architecture deep dive
├── DEPLOYMENT_GUIDE.md          # Production deployment
├── docker-compose.yml           # Local infrastructure
├── init-db.sql                  # Database schema
├── Dockerfile                   # Container image
├── pom.xml                      # Dependencies
└── src/main/java/com/sahil/
    ├── UrlShortenerApplication.java
    ├── controller/              # REST endpoints
    ├── service/                 # Business logic
    ├── repository/              # Data access
    ├── model/                   # Entities
    ├── dto/                     # Transfer objects
    ├── util/                    # Helper utilities
    ├── cache/                   # Redis caching
    ├── kafka/                   # Event streaming
    ├── config/                  # Configuration
    └── exception/               # Error handling
```

---

## 📚 Documentation Map

Choose what you need:

| Document | Purpose | Audience |
|----------|---------|----------|
| [README.md](README.md) | Complete system overview | Everyone |
| [API_EXAMPLES.md](API_EXAMPLES.md) | How to use the API | API consumers |
| [SYSTEM_DESIGN.md](SYSTEM_DESIGN.md) | Architecture & technical details | Engineers, Architects |
| [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) | Production deployment | DevOps, SRE |
| This file | Quick start guide | New users |

---

## 🎯 API Quick Reference

### Create Short URL
```bash
POST /api/v1/urls
{
  "originalUrl": "https://example.com/very/long/url",
  "customShortCode": "mylink"  # Optional
}
```

### Redirect
```bash
GET /{shortCode}
# Returns 301 Moved Permanently to original URL
```

### Get Statistics
```bash
GET /api/v1/urls/{shortCode}/stats
```

### Delete URL
```bash
DELETE /api/v1/urls/{shortCode}
```

See [API_EXAMPLES.md](API_EXAMPLES.md) for detailed examples!

---

## 🏗️ System Architecture

```
┌─────────────────────────────────┐
│      Your Application           │
│    (Browser, Mobile, API)       │
└──────────────┬──────────────────┘
               │
        ┌──────▼──────┐
        │ App Server  │  (Spring Boot)
        │  :8080      │
        └──────┬──────┘
               │
        ┌──────┴──────────┬──────────────┐
        │                 │              │
    ┌───▼──┐        ┌────▼──┐     ┌─────▼──┐
    │  🗄️  │        │  💾   │     │  📨   │
    │ Redis│       │ Postgres     │ Kafka  │
    │      │        │       │     │       │
    └──────┘        └───────┘     └───────┘
    (Cache)      (Database)    (Analytics)
    ~1-2ms       ~30-50ms      (Async)
```

**Key Points:**
✅ **Fast Redirects**: Redis cache (~1ms)  
✅ **Scalable**: Distributes load across instances  
✅ **Async Analytics**: Kafka processes without blocking  
✅ **Highly Available**: Replication & failover  

---

## 🚦 Health Checks

Monitor the system:

```bash
# API Health
curl http://localhost:8080/api/v1/urls/health

# PostgreSQL
docker exec url-shortener-postgres pg_isready -U postgres

# Redis
docker exec url-shortener-redis redis-cli PING

# Kafka
docker exec url-shortener-kafka kafka-broker-api-versions \
  --bootstrap-server localhost:9092
```

---

## 🔍 Performance Benchmarks

**Expected Performance:**

| Operation | Latency | Notes |
|-----------|---------|-------|
| Create URL | 5-20ms | Database write + cache |
| **Redirect (Cache Hit)** | **1-5ms** | ✅ Fast! |
| Redirect (Cache Miss) | 30-80ms | Database query |
| Get Stats | 10-50ms | Database query |
| Average (90% hit rate) | **8-15ms** | Production target |

**Throughput:**
- URLs created: 1,000+ req/sec
- Redirects: 10,000+ req/sec (with caching)

---

## 🛠️ Development Workflow

### Make Changes

1. Edit Java files in `src/main/java/com/sahil/`
2. Restart application:
   ```bash
   # Stop with Ctrl+C
   # Then run again
   mvn spring-boot:run
   ```

### Debug

```bash
# View detailed logs
docker logs -f url-shortener-postgres
docker logs -f url-shortener-redis
docker logs -f url-shortener-kafka

# Connect to database
docker exec -it url-shortener-postgres psql -U postgres -d url_shortener

# Query short URLs
SELECT short_code, original_url, click_count FROM urls LIMIT 5;
```

### Test Changes

```bash
# Create a test URL
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "https://example.com"}'

# Get statistics
curl http://localhost:8080/api/v1/urls/{shortCode}/stats

# Simulate clicks (generates Kafka events)
for i in {1..10}; do curl -L http://localhost:8080/{shortCode}; done
```

---

## 🐳 Docker Commands

```bash
# Start services
docker-compose up -d

# Stop services
docker-compose down

# View logs
docker-compose logs -f

# Scale application
docker-compose up -d --scale app=3

# Clean up volumes (careful!)
docker-compose down -v
```

---

## 📊 Monitoring Dashboard

Access Kafka UI (optional):
- **URL**: http://localhost:8081
- **View**: Message topics and consumer groups
- **Use**: Monitor `url-click-events` topic

---

## 🎓 Next Steps

1. **Understand the System**
   - Read [SYSTEM_DESIGN.md](SYSTEM_DESIGN.md) for deep dive
   - Understand Base62 encoding and Snowflake IDs

2. **Use the API**
   - Follow [API_EXAMPLES.md](API_EXAMPLES.md)
   - Try examples in Postman or cURL
   - Build client libraries

3. **Deploy to Production**
   - Follow [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)
   - Set up monitoring & logging
   - Configure backups

4. **Scale the System**
   - Add more app instances
   - Set up database replication
   - Create Redis cluster
   - Scale Kafka consumers

---

## ❓ FAQ

### Q: Why is my redirect slow?

**A:** First request hits database (30-50ms). Subsequent requests use cache (~1-2ms). Warm up the cache:
```bash
# Make a request that gets cached
curl http://localhost:8080/mylink
```

### Q: How do I change the rate limit?

**A:** Edit `RateLimiter.java`:
```java
private static final int MAX_REQUESTS = 100;  // Change this
private static final long WINDOW_MINUTES = 1;  // And this
```

### Q: Can I use a different database?

**A:** Yes! Edit `pom.xml` to use MySQL, MongoDB, etc. Update `application.yaml` connection string.

### Q: How do I scale to millions of URLs?

**A:** See [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) section "Scalability Strategies"

### Q: Is the data persisted?

**A:** Yes! PostgreSQL is persisted in Docker volumes. Check `docker-compose.yml`:
```yaml
volumes:
  - postgres_data:/var/lib/postgresql/data
```

---

## 🐛 Troubleshooting

### Services won't start
```bash
# Check what's using ports
lsof -i :5432  # PostgreSQL
lsof -i :6379  # Redis
lsof -i :9092  # Kafka
lsof -i :8080  # Application

# Remove port conflicts and restart
docker-compose down
docker-compose up -d
```

### Database errors
```bash
# Reinitialize database
docker-compose down -v  # WARNING: Deletes data!
docker-compose up -d
```

### Connection refused
```bash
# Ensure all services are healthy
docker-compose ps

# If not healthy, check logs
docker-compose logs postgres
docker-compose logs redis
docker-compose logs kafka
```

---

## 📞 Support

- **Issues?** Check the documentations
- **Questions?** See FAQ above
- **Bugs?** Open an issue on GitHub
- **Contributions?** Pull requests welcome!

---

## 📦 What's Included?

✅ Complete production-ready source code  
✅ Docker Compose infrastructure  
✅ Database schema & initialization scripts  
✅ API documentation with examples  
✅ System design documentation  
✅ Deployment guide for production  
✅ Performance tuning guide  
✅ Exception handling & error responses  
✅ Logging & monitoring setup  
✅ Rate limiting & access control  

---

## 📊 Metrics & Monitoring

Track these KPIs:

```
Request Rate:         curl http://localhost:8080/actuator/prometheus
Response Time (P99):  Should be < 100ms
Cache Hit Rate:       > 80%
Error Rate:           < 1%
Throughput:           > 1000 req/sec (redirects)
```

---

## 🎉 Success Criteria

You've successfully set up the system when:

✅ Services start with `docker-compose up -d`  
✅ Application starts: `mvn spring-boot:run`  
✅ Health check returns success: `curl .../health`  
✅ Can create a short URL  
✅ Can redirect to original URL  
✅ Can view statistics  
✅ Kafka events are being published  

**Congratulations!** You now have a production-grade URL shortener! 🎊

---

## 📚 Learn More

- [README.md](README.md) - Full documentation
- [SYSTEM_DESIGN.md](SYSTEM_DESIGN.md) - Technical deep dive
- [API_EXAMPLES.md](API_EXAMPLES.md) - API usage
- [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) - Production setup

---

**Last Updated:** March 2026  
**Version:** 1.0.0  
**Status:** Production-Ready
