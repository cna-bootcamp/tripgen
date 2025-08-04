# User Service Database Installation Guide - Development Environment

## 1. Overview

### 1.1 Service Information
- **Service Name**: User Service
- **Database Type**: PostgreSQL 16 (Alpine)
- **Database Name**: tripgen_user
- **Schema**: user_schema
- **Container**: postgresql.tripgen-dev.svc.cluster.local
- **Port**: 5432

### 1.2 Requirements
- Kubernetes cluster (AKS) with tripgen-dev namespace
- kubectl configured with proper permissions
- PostgreSQL client tools (optional for verification)

## 2. Installation Steps

### 2.1 Create Database Secret
```bash
# Create database credentials secret
kubectl create secret generic user-db-secret \
  --from-literal=username=user_service \
  --from-literal=password=UserServiceDev2025! \
  --from-literal=database=tripgen_user \
  -n tripgen-dev
```

### 2.2 Create ConfigMap for Database Initialization
```bash
# Create ConfigMap with initialization scripts
kubectl create configmap user-db-init \
  --from-file=init.sql=user-db-init.sql \
  -n tripgen-dev
```

### 2.3 Database Initialization Script
Create file `user-db-init.sql`:

```sql
-- Connect to main database to create service database
\c postgres;

-- Create service database
CREATE DATABASE tripgen_user;

-- Connect to service database
\c tripgen_user;

-- Create schema
CREATE SCHEMA IF NOT EXISTS user_schema;

-- Set default search path
ALTER DATABASE tripgen_user SET search_path TO user_schema, public;

-- Create service user
CREATE USER user_service WITH PASSWORD 'UserServiceDev2025!';

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE tripgen_user TO user_service;
GRANT ALL PRIVILEGES ON SCHEMA user_schema TO user_service;
ALTER SCHEMA user_schema OWNER TO user_service;

-- Switch to service user context
SET ROLE user_service;

-- Create tables
CREATE TABLE user_schema.users (
    id                  BIGSERIAL PRIMARY KEY,
    user_id            VARCHAR(36) UNIQUE NOT NULL,
    username           VARCHAR(50) UNIQUE NOT NULL,
    password           VARCHAR(255) NOT NULL,
    name               VARCHAR(100) NOT NULL,
    email              VARCHAR(255) UNIQUE NOT NULL,
    phone              VARCHAR(20),
    avatar_url         VARCHAR(500),
    status             VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    login_attempts     INTEGER NOT NULL DEFAULT 0,
    locked_until       TIMESTAMP WITH TIME ZONE,
    last_login_at      TIMESTAMP WITH TIME ZONE,
    created_by         VARCHAR(36),
    updated_by         VARCHAR(36),
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'LOCKED', 'DELETED')),
    CONSTRAINT chk_users_login_attempts CHECK (login_attempts >= 0),
    CONSTRAINT chk_users_username_length CHECK (LENGTH(username) >= 5),
    CONSTRAINT chk_users_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT chk_users_name_length CHECK (LENGTH(name) >= 2)
);

-- Create indexes
CREATE UNIQUE INDEX idx_users_user_id ON user_schema.users(user_id);
CREATE UNIQUE INDEX idx_users_username ON user_schema.users(username);
CREATE UNIQUE INDEX idx_users_email ON user_schema.users(email);
CREATE INDEX idx_users_username_status ON user_schema.users(username, status) WHERE status = 'ACTIVE';
CREATE INDEX idx_users_locked_until ON user_schema.users(locked_until) WHERE locked_until IS NOT NULL;
CREATE INDEX idx_users_last_login_at ON user_schema.users(last_login_at) WHERE last_login_at IS NOT NULL;
CREATE INDEX idx_users_created_at ON user_schema.users(created_at);

-- Create trigger function for updated_at
CREATE OR REPLACE FUNCTION user_schema.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger
CREATE TRIGGER update_users_updated_at BEFORE UPDATE
    ON user_schema.users FOR EACH ROW
    EXECUTE FUNCTION user_schema.update_updated_at_column();

-- Add comments
COMMENT ON TABLE user_schema.users IS 'User basic information';
COMMENT ON COLUMN user_schema.users.user_id IS 'UUID for inter-service connection';
COMMENT ON COLUMN user_schema.users.username IS 'Username for login (min 5 chars)';
COMMENT ON COLUMN user_schema.users.password IS 'BCrypt encrypted password';
COMMENT ON COLUMN user_schema.users.status IS 'Account status (ACTIVE/INACTIVE/SUSPENDED/LOCKED/DELETED)';
COMMENT ON COLUMN user_schema.users.login_attempts IS 'Login attempt count (account locked after 5 failures)';
COMMENT ON COLUMN user_schema.users.locked_until IS 'Account unlock time';

-- Insert test data for development
INSERT INTO user_schema.users (user_id, username, password, name, email, phone, status)
VALUES 
('550e8400-e29b-41d4-a716-446655440001', 'testuser1', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Test User 1', 'test1@tripgen.com', '010-1234-5678', 'ACTIVE'),
('550e8400-e29b-41d4-a716-446655440002', 'testuser2', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Test User 2', 'test2@tripgen.com', '010-2345-6789', 'ACTIVE'),
('550e8400-e29b-41d4-a716-446655440003', 'testadmin', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Admin User', 'admin@tripgen.com', '010-0000-0000', 'ACTIVE');

-- Note: Default password for all test users is 'password123'
```

### 2.4 Create Database Initialization Job
```yaml
# user-db-init-job.yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: user-db-init
  namespace: tripgen-dev
spec:
  template:
    spec:
      restartPolicy: Never
      containers:
      - name: postgres-init
        image: postgres:16-alpine
        resources:
          requests:
            cpu: 100m
            memory: 128Mi
          limits:
            cpu: 500m
            memory: 256Mi
        env:
        - name: PGPASSWORD
          valueFrom:
            secretKeyRef:
              name: postgresql-secret
              key: password
        command: ["/bin/bash"]
        args:
          - -c
          - |
            echo "Initializing User Service database..."
            psql -h postgresql.tripgen-dev.svc.cluster.local -U postgres -f /scripts/init.sql
            echo "Database initialization completed."
        volumeMounts:
        - name: init-scripts
          mountPath: /scripts
      volumes:
      - name: init-scripts
        configMap:
          name: user-db-init
```

### 2.5 Execute Database Initialization
```bash
# Apply the initialization job
kubectl apply -f user-db-init-job.yaml

# Monitor job execution
kubectl logs -f job/user-db-init -n tripgen-dev

# Verify job completion
kubectl get jobs -n tripgen-dev
```

## 3. Verification

### 3.1 Database Connection Test
```bash
# Connect to PostgreSQL pod
kubectl exec -it postgresql-0 -n tripgen-dev -- psql -U postgres

# Inside PostgreSQL, verify database creation
\l

# Connect to user database
\c tripgen_user

# Verify schema
\dn

# Verify tables
\dt user_schema.*

# Verify test data
SELECT user_id, username, email, status FROM user_schema.users;

# Exit
\q
```

### 3.2 Service Connection Configuration
Add the following environment variables to User Service deployment:

```yaml
env:
- name: DB_HOST
  value: postgresql.tripgen-dev.svc.cluster.local
- name: DB_PORT
  value: "5432"
- name: DB_NAME
  value: tripgen_user
- name: DB_SCHEMA
  value: user_schema
- name: DB_USERNAME
  valueFrom:
    secretKeyRef:
      name: user-db-secret
      key: username
- name: DB_PASSWORD
  valueFrom:
    secretKeyRef:
      name: user-db-secret
      key: password
```

### 3.3 Connection String
```
postgresql://user_service:UserServiceDev2025!@postgresql.tripgen-dev.svc.cluster.local:5432/tripgen_user?schema=user_schema
```

## 4. Maintenance

### 4.1 Manual Backup
```bash
# Backup user database
kubectl exec postgresql-0 -n tripgen-dev -- pg_dump -U postgres tripgen_user > user-db-backup-$(date +%Y%m%d).sql
```

### 4.2 Restore from Backup
```bash
# Restore user database
kubectl exec -i postgresql-0 -n tripgen-dev -- psql -U postgres tripgen_user < user-db-backup-20250730.sql
```

### 4.3 Database Monitoring
```bash
# Check database size
kubectl exec postgresql-0 -n tripgen-dev -- psql -U postgres -d tripgen_user -c "SELECT pg_database_size('tripgen_user');"

# Check active connections
kubectl exec postgresql-0 -n tripgen-dev -- psql -U postgres -c "SELECT count(*) FROM pg_stat_activity WHERE datname = 'tripgen_user';"

# Check table sizes
kubectl exec postgresql-0 -n tripgen-dev -- psql -U postgres -d tripgen_user -c "SELECT schemaname, tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size FROM pg_tables WHERE schemaname = 'user_schema' ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;"
```

## 5. Troubleshooting

### 5.1 Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Database not found | Initialization failed | Re-run initialization job |
| Permission denied | User permissions not set | Check and reset user permissions |
| Connection refused | PostgreSQL not running | Check pod status and restart if needed |
| Schema not found | Wrong search path | Verify search_path setting |

### 5.2 Debug Commands
```bash
# Check PostgreSQL pod logs
kubectl logs postgresql-0 -n tripgen-dev

# Check initialization job logs
kubectl logs job/user-db-init -n tripgen-dev

# Describe pod for events
kubectl describe pod postgresql-0 -n tripgen-dev
```

## 6. Development Notes

- Default test password: 'password123' (BCrypt hashed)
- All timestamps use UTC timezone
- Account locks after 5 failed login attempts
- Email validation uses regex pattern
- Username must be at least 5 characters