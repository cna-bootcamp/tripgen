# User Service Cache Installation Guide - Development Environment

## 1. Overview

### 1.1 Service Information
- **Service Name**: User Service Cache
- **Cache Type**: Redis 7.2 (Alpine)
- **Database Number**: 1
- **Container**: redis.tripgen-dev.svc.cluster.local
- **Port**: 6379
- **Persistence**: Disabled (Development Environment)

### 1.2 Cache Strategy
- **Primary Use Cases**:
  - JWT token blacklist
  - User session management
  - Login attempt tracking
  - User profile caching
  - Permission caching

## 2. Installation Steps

### 2.1 Redis Configuration
Create Redis configuration for User Service:

```bash
# Create ConfigMap for Redis configuration
kubectl create configmap user-redis-config \
  --from-literal=redis.conf="
# Redis configuration for User Service
port 6379
bind 0.0.0.0
protected-mode yes
requirepass UserCacheDev2025!

# Memory management
maxmemory 200mb
maxmemory-policy allkeys-lru

# Persistence disabled for dev environment
save \"\"
appendonly no

# Database selection
databases 16

# Connection settings
timeout 300
tcp-keepalive 60

# Logging
loglevel notice
" \
  -n tripgen-dev
```

### 2.2 Cache Key Patterns
Define cache key patterns for User Service:

```yaml
# user-cache-keys.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: user-cache-keys
  namespace: tripgen-dev
data:
  cache-keys.properties: |
    # JWT Token Blacklist
    jwt.blacklist.pattern=jwt:blacklist:{jti}
    jwt.blacklist.ttl=86400
    
    # User Session
    session.pattern=session:{userId}
    session.ttl=3600
    
    # Login Attempts
    login.attempts.pattern=login:attempts:{username}
    login.attempts.ttl=1800
    
    # User Profile Cache
    user.profile.pattern=user:profile:{userId}
    user.profile.ttl=600
    
    # User Permissions Cache
    user.permissions.pattern=user:permissions:{userId}
    user.permissions.ttl=300
    
    # Email Verification Tokens
    email.verification.pattern=email:verify:{token}
    email.verification.ttl=3600
    
    # Password Reset Tokens
    password.reset.pattern=password:reset:{token}
    password.reset.ttl=900
```

### 2.3 Initialize Redis Database
Create initialization script:

```bash
# user-redis-init.sh
#!/bin/bash

echo "Initializing User Service Redis cache..."

# Connect to Redis with password
redis-cli -h redis.tripgen-dev.svc.cluster.local -a UserCacheDev2025! <<EOF
# Select database 1 for User Service
SELECT 1

# Clear any existing data (development only)
FLUSHDB

# Set initial configuration keys
SET config:service:name "user-service"
SET config:cache:version "1.0.0"
SET config:initialized:at "$(date -u +%Y-%m-%dT%H:%M:%SZ)"

# Create test data for development
# Test user session
SETEX session:550e8400-e29b-41d4-a716-446655440001 3600 '{
  "userId": "550e8400-e29b-41d4-a716-446655440001",
  "username": "testuser1",
  "roles": ["USER"],
  "loginTime": "2025-01-30T10:00:00Z",
  "ipAddress": "127.0.0.1"
}'

# Test user profile cache
SETEX user:profile:550e8400-e29b-41d4-a716-446655440001 600 '{
  "userId": "550e8400-e29b-41d4-a716-446655440001",
  "username": "testuser1",
  "name": "Test User 1",
  "email": "test1@tripgen.com",
  "avatarUrl": null,
  "status": "ACTIVE"
}'

# Test permissions cache
SETEX user:permissions:550e8400-e29b-41d4-a716-446655440001 300 '["VIEW_TRIPS", "CREATE_TRIP", "EDIT_OWN_TRIP", "DELETE_OWN_TRIP"]'

# Show cache statistics
INFO keyspace
EOF

echo "User Service Redis cache initialization completed."
```

### 2.4 Create Initialization Job
```yaml
# user-redis-init-job.yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: user-redis-init
  namespace: tripgen-dev
spec:
  template:
    spec:
      restartPolicy: Never
      containers:
      - name: redis-init
        image: redis:7.2-alpine
        resources:
          requests:
            cpu: 100m
            memory: 64Mi
          limits:
            cpu: 200m
            memory: 128Mi
        command: ["/scripts/init.sh"]
        volumeMounts:
        - name: init-script
          mountPath: /scripts
      volumes:
      - name: init-script
        configMap:
          name: user-redis-init-script
          defaultMode: 0755
```

### 2.5 Service Connection Configuration
Add the following environment variables to User Service:

```yaml
# Redis connection environment variables
env:
- name: REDIS_HOST
  value: redis.tripgen-dev.svc.cluster.local
- name: REDIS_PORT
  value: "6379"
- name: REDIS_PASSWORD
  valueFrom:
    secretKeyRef:
      name: user-redis-secret
      key: password
- name: REDIS_DATABASE
  value: "1"
- name: REDIS_SSL_ENABLED
  value: "false"
- name: REDIS_TIMEOUT
  value: "5000"
- name: REDIS_CONNECTION_POOL_SIZE
  value: "10"
```

## 3. Cache Implementation Guide

### 3.1 Spring Boot Configuration
```yaml
# application-dev.yml
spring:
  redis:
    host: ${REDIS_HOST}
    port: ${REDIS_PORT}
    password: ${REDIS_PASSWORD}
    database: ${REDIS_DATABASE}
    timeout: ${REDIS_TIMEOUT}
    lettuce:
      pool:
        max-active: 10
        max-idle: 5
        min-idle: 2
        max-wait: -1ms
  cache:
    type: redis
    redis:
      time-to-live: 600000  # 10 minutes default
      cache-null-values: false
      use-key-prefix: true
      key-prefix: "user:"
```

### 3.2 Cache Usage Examples

#### JWT Token Blacklist
```java
// Add token to blacklist
redisTemplate.opsForValue().set(
    "jwt:blacklist:" + jti,
    "true",
    Duration.ofSeconds(86400)
);

// Check if token is blacklisted
Boolean isBlacklisted = redisTemplate.hasKey("jwt:blacklist:" + jti);
```

#### User Session Management
```java
// Store user session
String sessionKey = "session:" + userId;
redisTemplate.opsForValue().set(
    sessionKey,
    sessionData,
    Duration.ofHours(1)
);

// Retrieve user session
UserSession session = redisTemplate.opsForValue().get(sessionKey);
```

#### Login Attempt Tracking
```java
// Increment login attempts
String attemptKey = "login:attempts:" + username;
Long attempts = redisTemplate.opsForValue().increment(attemptKey);
redisTemplate.expire(attemptKey, Duration.ofMinutes(30));

// Check login attempts
String attempts = redisTemplate.opsForValue().get(attemptKey);
```

## 4. Monitoring and Maintenance

### 4.1 Monitor Cache Usage
```bash
# Connect to Redis
kubectl exec -it redis-0 -n tripgen-dev -- redis-cli -a UserCacheDev2025!

# Select User Service database
SELECT 1

# Check memory usage
INFO memory

# Check key count
DBSIZE

# Check key patterns
KEYS *

# Monitor real-time commands
MONITOR
```

### 4.2 Cache Statistics
```bash
# Get cache hit/miss ratio
kubectl exec redis-0 -n tripgen-dev -- redis-cli -a UserCacheDev2025! --stat

# Get slow queries
kubectl exec redis-0 -n tripgen-dev -- redis-cli -a UserCacheDev2025! SLOWLOG GET 10

# Get client connections
kubectl exec redis-0 -n tripgen-dev -- redis-cli -a UserCacheDev2025! CLIENT LIST
```

### 4.3 Cache Cleanup
```bash
# Clear all User Service cache (DB 1)
kubectl exec redis-0 -n tripgen-dev -- redis-cli -a UserCacheDev2025! -n 1 FLUSHDB

# Remove specific pattern
kubectl exec redis-0 -n tripgen-dev -- redis-cli -a UserCacheDev2025! -n 1 --scan --pattern "session:*" | xargs redis-cli -a UserCacheDev2025! -n 1 DEL
```

## 5. Performance Tuning

### 5.1 Memory Optimization
| Setting | Value | Description |
|---------|-------|-------------|
| maxmemory | 200MB | Maximum memory for User Service cache |
| maxmemory-policy | allkeys-lru | Evict least recently used keys |
| maxmemory-samples | 5 | LRU sample size |

### 5.2 Connection Pool Settings
| Setting | Value | Description |
|---------|-------|-------------|
| max-active | 10 | Maximum active connections |
| max-idle | 5 | Maximum idle connections |
| min-idle | 2 | Minimum idle connections |
| max-wait | -1 | No wait timeout |

## 6. Troubleshooting

### 6.1 Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Connection timeout | Network issue or Redis down | Check pod status and network |
| Out of memory | Cache size exceeded | Adjust maxmemory or eviction policy |
| Slow queries | Large key operations | Optimize key structure and queries |
| High latency | Network or CPU issues | Check resource usage and network |

### 6.2 Debug Commands
```bash
# Check Redis logs
kubectl logs redis-0 -n tripgen-dev

# Check Redis configuration
kubectl exec redis-0 -n tripgen-dev -- redis-cli -a UserCacheDev2025! CONFIG GET "*"

# Test connectivity
kubectl exec -it deployment/user-service -n tripgen-dev -- redis-cli -h redis.tripgen-dev.svc.cluster.local -a UserCacheDev2025! ping
```

## 7. Best Practices

1. **Key Naming**: Use consistent patterns (service:type:identifier)
2. **TTL Management**: Set appropriate TTL for all keys
3. **Error Handling**: Implement cache-aside pattern with fallback
4. **Monitoring**: Regular monitoring of memory usage and hit rates
5. **Security**: Use strong passwords and network policies

## 8. Development Notes

- Redis persistence is disabled in development environment
- Data will be lost on pod restart
- Use Redis Commander or RedisInsight for GUI access
- Consider using Redis Sentinel for high availability in production