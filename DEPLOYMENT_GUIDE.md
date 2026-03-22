# Deployment Guide - Production Setup

This guide covers deploying the URL Shortener to production environments.

## Table of Contents
1. [Pre-Deployment Checklist](#pre-deployment-checklist)
2. [Local Development Setup](#local-development-setup)
3. [Docker Deployment](#docker-deployment)
4. [Kubernetes Deployment](#kubernetes-deployment)
5. [Cloud Platforms](#cloud-platforms)
6. [Monitoring & Logging](#monitoring--logging)
7. [Backup & Disaster Recovery](#backup--disaster-recovery)
8. [Performance Tuning](#performance-tuning)

---

## Pre-Deployment Checklist

- [ ] Java 21+ installed
- [ ] Maven 3.8+available
- [ ] Docker & Docker Compose installed
- [ ] PostgreSQL credentials configured
- [ ] Redis password configured
- [ ] Kafka brokers accessible
- [ ] TLS certificates obtained
- [ ] DNS records configured
- [ ] Load balancer configured
- [ ] Monitoring stack ready
- [ ] Backup strategy defined
- [ ] Disaster recovery plan created

---

## Local Development Setup

### Step 1: Clone Repository

```bash
git clone https://github.com/yourusername/url-shortener.git
cd url-shortener
```

### Step 2: Start Infrastructure

```bash
# Using Docker Compose (Recommended)
docker-compose up -d

# Wait for all services to be healthy
docker-compose ps

# Expected output showing all services "Up" with health status
```

### Step 3: Build Application

```bash
# Build with Maven
mvn clean package -DskipTests

# Or use local JDK
./mvnw clean package -DskipTests
```

### Step 4: Configure Environment

Update `src/main/resources/application.yaml`:

```bash
# For local development
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/url_shortener
    username: postgres
    password: postgres
  data:
    redis:
      host: localhost
      port: 6379
      password: redis
  kafka:
    bootstrap-servers: localhost:9092
```

### Step 5: Run Application

```bash
# Via Maven
mvn spring-boot:run

# Or via JAR
java -jar target/url-shortener-0.0.1-SNAPSHOT.jar

# Expected: "Started UrlShortenerApplication in X.XXX seconds"
```

### Step 6: Verify

```bash
# Health check
curl http://localhost:8080/api/v1/urls/health

# Response
# "URL Shortener API is running"
```

---

## Docker Deployment

### Single Container Deployment

#### Step 1: Build Docker Image

```bash
# Build image
docker build -t url-shortener:latest .

# Verify image
docker images | grep url-shortener
```

#### Step 2: Run Container

```bash
docker run -d \
  --name url-shortener \
  --network url-shortener-network \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/url_shortener \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  -e SPRING_DATA_REDIS_HOST=redis \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  --restart unless-stopped \
  url-shortener:latest
```

#### Step 3: Monitor Logs

```bash
docker logs -f url-shortener
```

### Multi-Container Orchestration

#### Step 1: Update Docker Compose

```yaml
# docker-compose.yml for production
version: '3.8'

services:
  # Production database
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: url_shortener
      POSTGRES_USER: prod_user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    restart: always

  # Production Redis
  redis:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD}
    restart: always

  # Production Kafka
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    restart: always

  # Application instances
  app1:
    image: url-shortener:latest
    ports:
      - "8080:8080"
    depends_on:
      - postgres
      - redis
      - kafka
    restart: always

  app2:
    image: url-shortener:latest
    ports:
      - "8081:8080"
    depends_on:
      - postgres
      - redis
      - kafka
    restart: always

volumes:
  postgres_data:
```

#### Step 2: Deploy

```bash
# Set environment variables
export DB_PASSWORD=your_secure_password
export REDIS_PASSWORD=your_redis_password

# Start all services
docker-compose -f docker-compose.prod.yml up -d

# Scale application
docker-compose -f docker-compose.prod.yml up -d --scale app=3
```

---

## Kubernetes Deployment

### Step 1: Create Kubernetes Manifests

#### deployment.yaml

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: url-shortener
  labels:
    app: url-shortener
spec:
  replicas: 3
  selector:
    matchLabels:
      app: url-shortener
  template:
    metadata:
      labels:
        app: url-shortener
    spec:
      containers:
      - name: url-shortener
        image: your-registry/url-shortener:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_DATASOURCE_URL
          value: jdbc:postgresql://postgres-service:5432/url_shortener
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: password
        - name: SPRING_DATA_REDIS_HOST
          value: redis-service
        - name: SPRING_KAFKA_BOOTSTRAP_SERVERS
          value: kafka-service:9092
        resources:
          requests:
            cpu: "500m"
            memory: "512Mi"
          limits:
            cpu: "2000m"
            memory: "2Gi"
        livenessProbe:
          httpGet:
            path: /api/v1/urls/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /api/v1/urls/health
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
```

#### service.yaml

```yaml
apiVersion: v1
kind: Service
metadata:
  name: url-shortener-service
spec:
  selector:
    app: url-shortener
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: LoadBalancer
```

#### ingress.yaml

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: url-shortener-ingress
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - urls.example.com
    secretName: url-shortener-tls
  rules:
  - host: urls.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: url-shortener-service
            port:
              number: 80
```

### Step 2: Deploy to Kubernetes

```bash
# Create namespace
kubectl create namespace url-shortener

# Create secrets
kubectl create secret generic db-credentials \
  --from-literal=username=prod_user \
  --from-literal=password=your_secure_password \
  -n url-shortener

# Deploy
kubectl apply -f deployment.yaml -n url-shortener
kubectl apply -f service.yaml -n url-shortener
kubectl apply -f ingress.yaml -n url-shortener

# Verify deployment
kubectl get pods -n url-shortener
kubectl get svc -n url-shortener
```

### Step 3: Scale Application

```bash
# Scale to 5 replicas
kubectl scale deployment url-shortener --replicas=5 -n url-shortener

# Auto-scale based on CPU
kubectl autoscale deployment url-shortener \
  --min=3 --max=10 --cpu-percent=70 \
  -n url-shortener
```

---

## Cloud Platforms

### AWS Deployment

#### Using AWS RDS, ElastiCache, and ECS

```bash
# 1. Create RDS PostgreSQL
aws rds create-db-instance \
  --db-instance-identifier url-shortener-db \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --master-username postgres \
  --master-user-password your-password

# 2. Create ElastiCache Redis
aws elasticache create-cache-cluster \
  --cache-cluster-id url-shortener-cache \
  --cache-node-type cache.t3.micro \
  --engine redis

# 3. Create MSK Kafka cluster
aws kafka create-cluster \
  --cluster-name url-shortener-kafka \
  --broker-node-group-info ...

# 4. Deploy to ECS
aws ecs create-service \
  --cluster url-shortener \
  --service-name url-shortener-api \
  --task-definition url-shortener:1 \
  --desired-count 3
```

### Google Cloud Deployment

#### Using Cloud Run, Cloud SQL, and Memorystore

```bash
# 1. Build and push image
gcloud builds submit --tag gcr.io/PROJECT_ID/url-shortener

# 2. Deploy to Cloud Run
gcloud run deploy url-shortener \
  --image gcr.io/PROJECT_ID/url-shortener \
  --platform managed \
  --region us-central1

# 3. Create Cloud SQL PostgreSQL
gcloud sql instances create url-shortener-db \
  --database-version=POSTGRES_15 \
  --tier=db-f1-micro

# 4. Create Memorystore Redis
gcloud redis instances create url-shortener-cache \
  --region=us-central1 \
  --size=1
```

### Azure Deployment

#### Using Azure Container Instances and Managed Services

```bash
# 1. Create container registry
az acr create \
  --resource-group myGroup \
  --name urlshortenerregistry

# 2. Build and push
az acr build \
  --registry urlshortenerregistry \
  --image url-shortener:latest .

# 3. Deploy to Container Instances
az container create \
  --resource-group myGroup \
  --name url-shortener \
  --image urlshortenerregistry.azurecr.io/url-shortener:latest \
  --environment-variables \
    SPRING_DATASOURCE_URL=... \
    SPRING_DATA_REDIS_HOST=...
```

---

## Monitoring & Logging

### Prometheus Metrics

```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'url-shortener'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'
```

### ELK Stack Setup

```bash
# Docker Compose for ELK
docker run -d \
  -p 9200:9200 \
  -e "discovery.type=single-node" \
  docker.elastic.co/elasticsearch/elasticsearch:8.0.0

docker run -d \
  -p 5601:5601 \
  docker.elastic.co/kibana/kibana:8.0.0

# Application logging to ELK
# Configure logback-spring.xml with Logstash appender
```

### Key Metrics to Monitor

```
HTTP Metrics:
- request_count_total
- request_duration_seconds
- request_errors_total

Database Metrics:
- db_query_duration_seconds
- db_connection_pool_size
- db_connection_pool_active

Cache Metrics:
- cache_hits_total
- cache_misses_total
- cache_evictions_total

Business Metrics:
- urls_created_total
- urls_redirected_total
- avg_redirect_latency_ms
```

---

## Backup & Disaster Recovery

### Database Backups

```bash
# Manual backup
pg_dump -h localhost -U postgres url_shortener > backup.sql

# Automated daily backups
0 2 * * * pg_dump -h localhost -U postgres url_shortener | \
  gzip > /backups/url_shortener_$(date +%Y%m%d).sql.gz

# Replication setup
# Primary and Standby for automatic failover
```

### Redis Persistence

```bash
# RDB (Point-in-time snapshots)
redis-cli BGSAVE  # Background save

# AOF (Append-only file)
redis-cli CONFIG SET appendonly yes
redis-cli CONFIG REWRITE
```

### Data Recovery

```bash
# Restore from PostgreSQL backup
psql -h localhost -U postgres url_shortener < backup.sql

# Restore Redis snapshot
redis-server --appendonly no
# Copy dump.rdb to Redis data directory
redis-server
redis-cli CONFIG REWRITE
```

---

## Performance Tuning

### JVM Configuration

```bash
# Memory tuning
java -Xms1g -Xmx2g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+ParallelRefProcEnabled \
  -jar url-shortener.jar
```

### Database Tuning

```sql
-- Connection pool
max_connections = 200
idle_in_transaction_session_timeout = '5min'

-- Performance
shared_buffers = '256MB'
effective_cache_size = '1GB'
work_mem = '4MB'

-- Query optimization
max_parallel_workers_per_gather = 4
max_worker_processes = 4
```

### Redis Tuning

```bash
# Maximum memory
redis-cli CONFIG SET maxmemory 512mb

# Eviction policy
redis-cli CONFIG SET maxmemory-policy allkeys-lru

# Persistence
redis-cli CONFIG SET save "900 1 300 10 60 10000"
```

### Network Tuning

```bash
# Increase file descriptors
ulimit -n 65536

# TCP tuning
sysctl -w net.ipv4.tcp_max_syn_backlog=5000
sysctl -w net.core.somaxconn=5000
```

---

## Troubleshooting

### Database Connection Issues

```bash
# Test connection
psql -h localhost -U postgres -d url_shortener

# Check connection pool
SELECT count(*) FROM pg_stat_activity;

# Show slow queries
SELECT query, calls, mean_time FROM pg_stat_statements 
ORDER BY mean_time DESC LIMIT 10;
```

### Redis Issues

```bash
# Test connectivity
redis-cli ping

# Check memory usage
redis-cli INFO memory

# Monitor commands
redis-cli MONITOR
```

### Kafka Issues

```bash
# Check broker status
kafka-broker-api-versions --bootstrap-server localhost:9092

# Check consumer lag
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group url-shortener-analytics --describe

# Produce test message
kafka-console-producer --broker-list localhost:9092 \
  --topic url-click-events
```

---

**Last Updated:** March 2026
