# API Examples - URL Shortener Service

This document provides practical examples for using the URL Shortener API with different tools.

## Table of Contents
- [cURL Examples](#curl-examples)
- [Postman Collection](#postman-collection)
- [JavaScript/Node.js Examples](#javascriptnodejs-examples)
- [Python Examples](#python-examples)
- [Java Examples](#java-examples)

---

## cURL Examples

### 1. Create a Shortened URL (Simple)

```bash
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -d '{
    "originalUrl": "https://www.example.com/very/long/url/path"
  }'
```

**Response:**
```json
{
  "shortUrl": "http://localhost:8080/a1b2c3d",
  "shortCode": "a1b2c3d",
  "originalUrl": "https://www.example.com/very/long/url/path",
  "createdAt": "2024-03-22T10:30:00",
  "expiresAt": null
}
```

---

### 2. Create with Custom Short Code

```bash
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -d '{
    "originalUrl": "https://github.com/sahiljuneja",
    "customShortCode": "myprofile"
  }'
```

---

### 3. Create with Expiration Date

```bash
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -d '{
    "originalUrl": "https://event.example.com",
    "customShortCode": "event2024",
    "expiresAt": "2024-12-31T23:59:59"
  }'
```

---

### 4. Redirect to Original URL

```bash
# Follow the redirect
curl -L http://localhost:8080/myprofile

# Show HTTP headers without following redirect
curl -i http://localhost:8080/myprofile
```

**Response Headers:**
```
HTTP/1.1 301 Moved Permanently
Location: https://github.com/sahiljuneja
Content-Length: 0
```

---

### 5. Get URL Statistics

```bash
curl http://localhost:8080/api/v1/urls/myprofile/stats
```

**Response:**
```json
{
  "shortCode": "myprofile",
  "originalUrl": "https://github.com/sahiljuneja",
  "clickCount": 42,
  "createdAt": "2024-03-22T10:30:00",
  "expiresAt": null
}
```

---

### 6. Delete a URL

```bash
curl -X DELETE http://localhost:8080/api/v1/urls/myprofile
```

**Response:**
```
HTTP/1.1 204 No Content
```

---

### 7. Health Check

```bash
curl http://localhost:8080/api/v1/urls/health
```

**Response:**
```
"URL Shortener API is running"
```

---

### 8. Error Cases

#### Invalid URL Format
```bash
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -d '{
    "originalUrl": "not-a-valid-url"
  }'
```

**Response (400):**
```json
{
  "error": "VALIDATION_ERROR",
  "message": "Invalid URL format",
  "timestamp": "2024-03-22T10:30:00",
  "status": 400,
  "path": "/api/v1/urls"
}
```

#### URL Not Found
```bash
curl http://localhost:8080/api/v1/urls/nonexistent/stats
```

**Response (404):**
```json
{
  "error": "NOT_FOUND",
  "message": "Short URL not found: nonexistent",
  "timestamp": "2024-03-22T10:30:00",
  "status": 404,
  "path": "/api/v1/urls/nonexistent/stats"
}
```

#### Rate Limit Exceeded
```bash
# Make 101 requests in 1 minute from same IP
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "https://example.com"}' \
  --repeat 101
```

**Response (429):**
```json
{
  "error": "TOO_MANY_REQUESTS",
  "message": "Too many requests. Maximum 100 requests per minute allowed.",
  "timestamp": "2024-03-22T10:30:00",
  "status": 429,
  "path": "/api/v1/urls"
}
```

#### Expired URL
```bash
# Try to access an expired URL
curl http://localhost:8080/expired_code
```

**Response (410):**
```json
{
  "error": "GONE",
  "message": "This short URL has expired",
  "timestamp": "2024-03-22T10:30:00",
  "status": 410,
  "path": "/expired_code"
}
```

---

## Postman Collection

### Import URL
```
Import from JSON below into Postman
```

### Collection JSON

```json
{
  "info": {
    "name": "URL Shortener API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Create Shortened URL",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"originalUrl\": \"https://www.example.com/very/long/path\",\n  \"customShortCode\": \"example\"\n}"
        },
        "url": {
          "raw": "http://localhost:8080/api/v1/urls",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "v1", "urls"]
        }
      }
    },
    {
      "name": "Get URL Stats",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "http://localhost:8080/api/v1/urls/{{shortCode}}/stats",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "v1", "urls", "{{shortCode}}", "stats"]
        }
      }
    },
    {
      "name": "Redirect URL",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "http://localhost:8080/{{shortCode}}",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["{{shortCode}}"]
        }
      }
    },
    {
      "name": "Delete URL",
      "request": {
        "method": "DELETE",
        "header": [],
        "url": {
          "raw": "http://localhost:8080/api/v1/urls/{{shortCode}}",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "v1", "urls", "{{shortCode}}"]
        }
      }
    },
    {
      "name": "Health Check",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "http://localhost:8080/api/v1/urls/health",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "v1", "urls", "health"]
        }
      }
    }
  ],
  "variable": [
    {
      "key": "shortCode",
      "value": "example"
    }
  ]
}
```

---

## JavaScript/Node.js Examples

### 1. Create Shortened URL

```javascript
const axios = require('axios');

async function shortenUrl(originalUrl, customCode) {
  try {
    const response = await axios.post(
      'http://localhost:8080/api/v1/urls',
      {
        originalUrl: originalUrl,
        customShortCode: customCode
      },
      {
        headers: {
          'Content-Type': 'application/json'
        }
      }
    );

    console.log('Short URL created:', response.data.shortUrl);
    return response.data;
  } catch (error) {
    console.error('Error:', error.response?.data?.message);
  }
}

// Usage
shortenUrl('https://example.com/very/long/url', 'mylink');
```

### 2. Get URL Statistics

```javascript
async function getUrlStats(shortCode) {
  try {
    const response = await axios.get(
      `http://localhost:8080/api/v1/urls/${shortCode}/stats`
    );

    console.log('Statistics:', response.data);
    console.log(`Clicks: ${response.data.clickCount}`);
    return response.data;
  } catch (error) {
    if (error.response?.status === 404) {
      console.error('URL not found');
    } else {
      console.error('Error:', error.message);
    }
  }
}

// Usage
getUrlStats('mylink');
```

### 3. Batch URL Creation

```javascript
async function batchShortenUrls(urls) {
  const results = [];

  for (const url of urls) {
    try {
      const response = await axios.post(
        'http://localhost:8080/api/v1/urls',
        { originalUrl: url }
      );
      results.push({
        original: url,
        short: response.data.shortUrl,
        status: 'success'
      });
    } catch (error) {
      results.push({
        original: url,
        status: 'failed',
        error: error.response?.data?.message
      });
    }
    
    // Rate limit: wait 100ms between requests
    await new Promise(resolve => setTimeout(resolve, 100));
  }

  return results;
}

// Usage
const urls = [
  'https://example1.com',
  'https://example2.com',
  'https://example3.com'
];
batchShortenUrls(urls).then(results => console.log(results));
```

### 4. Performance Testing

```javascript
const axios = require('axios');

async function performanceTest() {
  const iterations = 100;
  const times = [];

  console.log(`Running ${iterations} requests...`);

  for (let i = 0; i < iterations; i++) {
    const start = Date.now();
    
    try {
      await axios.get('http://localhost:8080/testcode', {
        maxRedirects: 0,
        validateStatus: (status) => status < 500
      });
    } catch (error) {
      // Ignore redirect error
    }

    const duration = Date.now() - start;
    times.push(duration);
  }

  const sorted = times.sort((a, b) => a - b);
  console.log(`
    Min: ${sorted[0]}ms
    Max: ${sorted[sorted.length - 1]}ms
    Avg: ${(times.reduce((a, b) => a + b) / times.length).toFixed(2)}ms
    P50: ${sorted[Math.floor(sorted.length * 0.5)]}ms
    P95: ${sorted[Math.floor(sorted.length * 0.95)]}ms
    P99: ${sorted[Math.floor(sorted.length * 0.99)]}ms
  `);
}

performanceTest();
```

---

## Python Examples

### 1. Create Shortened URL

```python
import requests

def shorten_url(original_url, custom_code=None):
    payload = {
        'originalUrl': original_url,
        'customShortCode': custom_code
    }
    
    response = requests.post(
        'http://localhost:8080/api/v1/urls',
        json=payload
    )
    
    if response.status_code == 201:
        data = response.json()
        print(f"Short URL: {data['shortUrl']}")
        return data
    else:
        print(f"Error: {response.status_code}")
        print(response.json())

# Usage
shorten_url('https://example.com/very/long/path', 'mylink')
```

### 2. Get URL Statistics

```python
import requests

def get_stats(short_code):
    response = requests.get(
        f'http://localhost:8080/api/v1/urls/{short_code}/stats'
    )
    
    if response.status_code == 200:
        stats = response.json()
        print(f"URL: {stats['originalUrl']}")
        print(f"Clicks: {stats['clickCount']}")
        print(f"Created: {stats['createdAt']}")
        return stats
    elif response.status_code == 404:
        print("URL not found")
    else:
        print(f"Error: {response.status_code}")

# Usage
get_stats('mylink')
```

### 3. Load Testing

```python
import requests
import time
from concurrent.futures import ThreadPoolExecutor, as_completed

def load_test(num_requests=1000, num_threads=10):
    def make_request(i):
        start = time.time()
        try:
            response = requests.get(
                'http://localhost:8080/testcode',
                allow_redirects=False
            )
            duration = (time.time() - start) * 1000
            return duration
        except Exception as e:
            print(f"Request {i} failed: {e}")
            return None

    with ThreadPoolExecutor(max_workers=num_threads) as executor:
        futures = [
            executor.submit(make_request, i) 
            for i in range(num_requests)
        ]
        
        durations = [f.result() for f in as_completed(futures) if f.result()]
    
    durations.sort()
    
    print(f"\nLoad Test Results (1000 requests):")
    print(f"  Min: {durations[0]:.2f}ms")
    print(f"  Max: {durations[-1]:.2f}ms")
    print(f"  Avg: {sum(durations)/len(durations):.2f}ms")
    print(f"  P50: {durations[len(durations)//2]:.2f}ms")
    print(f"  P95: {durations[int(len(durations)*0.95)]:.2f}ms")
    print(f"  P99: {durations[int(len(durations)*0.99)]:.2f}ms")

# Usage
load_test()
```

---

## Java Examples

### 1. Create Shortened URL

```java
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

public class UrlShortenerClient {
    private static final String API_URL = "http://localhost:8080/api/v1/urls";
    private static final HttpClient client = HttpClient.newHttpClient();

    public static void shortenUrl(String originalUrl, String customCode) {
        String json = String.format(
            "{\"originalUrl\": \"%s\", \"customShortCode\": \"%s\"}",
            originalUrl, customCode
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        try {
            HttpResponse<String> response = client.send(
                request, 
                HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 201) {
                System.out.println("Success: " + response.body());
            } else {
                System.out.println("Error: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        shortenUrl("https://example.com/very/long/path", "mylink");
    }
}
```

### 2. Using RestTemplate (Spring)

```java
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import java.util.HashMap;
import java.util.Map;

@Service
public class UrlShortenerService {
    private final RestTemplate restTemplate = new RestTemplate();
    private static final String API_URL = "http://localhost:8080/api/v1/urls";

    public String shortenUrl(String originalUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        Map<String, String> body = new HashMap<>();
        body.put("originalUrl", originalUrl);

        HttpEntity<Map<String, String>> request = 
            new HttpEntity<>(body, headers);

        var response = restTemplate.postForObject(
            API_URL, 
            request, 
            Map.class
        );

        return (String) response.get("shortUrl");
    }
}
```

---

## Testing with Apache Bench

```bash
# Install Apache Bench
# macOS: brew install httpd-benchmark
# Linux: apt-get install apache2-utils
# Windows: choco install apache-bench

# Test redirect endpoint (1000 requests, 10 concurrent)
ab -n 1000 -c 10 http://localhost:8080/mylink

# Output example:
# Benchmarking localhost (be patient).....done
# 
# Server Software:        Apache Tomcat/10.1.7
# Server Hostname:        localhost
# Server Port:            8080
# 
# Document Path:          /mylink
# Document Length:        0 bytes
# 
# Concurrency Level:      10
# Time taken for tests:   2.345 seconds
# Complete requests:      1000
# Failed requests:        0
# Requests per second:    426.43 [#/sec] (mean)
# Time per request:       23.45 [ms] (mean)
# Time per request:       2.345 [ms] (mean, across all concurrent requests)
# Transfer rate:          89.23 [Kbytes/sec] received
```

---

## Monitoring & Debugging

### Check API Health

```bash
# Every 5 seconds
watch -n 5 'curl -s http://localhost:8080/api/v1/urls/health'
```

### Monitor Redis Cache

```bash
# Connect to Redis
redis-cli -h localhost -p 6379

# Check cached URLs
KEYS url:*
GET url:mylink

# Monitor real-time commands
MONITOR
```

### View Kafka Events

```bash
# List topics
kafka-topics --bootstrap-server localhost:9092 --list

# View messages
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic url-click-events --from-beginning

# View consumer lag
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group url-shortener-analytics --describe
```

---

**Last Updated:** March 2026
