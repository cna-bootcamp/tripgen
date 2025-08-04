# Location Service 개발환경 데이터베이스 설치 가이드

## 개요

Location Service는 장소 검색, 상세 정보 및 지리정보 관리를 담당하는 서비스입니다. PostgreSQL 14 + PostGIS를 사용하여 공간 데이터와 지리정보를 효율적으로 처리하고 관리합니다.

### 주요 기능
- 장소 기본 정보 및 지리좌표 관리
- PostGIS를 활용한 공간 검색 및 거리 계산
- 장소 상세 정보 (영업시간, 리뷰, 사진 등)
- AI 추천 정보 캐싱
- 외부 API (Google, Kakao) 데이터 통합

## 사전 요구사항

### 필수 소프트웨어
- Docker Desktop
- kubectl
- 최소 3GB 여유 디스크 공간
- 최소 1.5GB 여유 메모리

### 확인 명령어
```bash
# Docker 상태 확인
docker --version
kubectl version --client
kubectl cluster-info
```

## 설치 절차

### 1단계: 네임스페이스 확인

```bash
# 개발 네임스페이스 확인 (없으면 생성)
kubectl get namespace tripgen-dev || kubectl create namespace tripgen-dev
```

### 2단계: Kubernetes 매니페스트 파일 생성

#### location-db-configmap.yaml
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: location-db-config
  namespace: tripgen-dev
data:
  POSTGRES_DB: "tripgen_location_db"
  POSTGRES_USER: "tripgen_location"
  POSTGRES_SCHEMA: "tripgen_location"
  PGDATA: "/var/lib/postgresql/data/pgdata"
```

#### location-db-secret.yaml
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: location-db-secret
  namespace: tripgen-dev
type: Opaque
data:
  # Base64 인코딩된 값 (실제 운영시 변경 필요)
  POSTGRES_PASSWORD: dHJpcGdlbl9sb2NhdGlvbl8xMjM=  # tripgen_location_123
```

#### location-db-pvc.yaml
```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: location-db-pvc
  namespace: tripgen-dev
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 15Gi  # PostGIS 및 공간 데이터를 위한 추가 공간
  storageClassName: standard
```

#### location-db-deployment.yaml
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: location-db
  namespace: tripgen-dev
  labels:
    app: location-db
    component: database
    service: location-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app: location-db
  template:
    metadata:
      labels:
        app: location-db
        component: database
        service: location-service
    spec:
      containers:
      - name: postgres-postgis
        image: postgis/postgis:14-3.2-alpine  # PostGIS 포함 이미지
        ports:
        - containerPort: 5432
          name: postgres
        env:
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: location-db-secret
              key: POSTGRES_PASSWORD
        envFrom:
        - configMapRef:
            name: location-db-config
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
        - name: init-sql
          mountPath: /docker-entrypoint-initdb.d
        resources:
          requests:
            memory: "768Mi"
            cpu: "500m"
          limits:
            memory: "1.5Gi"
            cpu: "1000m"
        livenessProbe:
          exec:
            command:
            - /bin/sh
            - -c
            - exec pg_isready -U tripgen_location -d tripgen_location_db -h 127.0.0.1 -p 5432
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          successThreshold: 1
          failureThreshold: 6
        readinessProbe:
          exec:
            command:
            - /bin/sh
            - -c
            - exec pg_isready -U tripgen_location -d tripgen_location_db -h 127.0.0.1 -p 5432
          initialDelaySeconds: 5
          periodSeconds: 10
          timeoutSeconds: 5
          successThreshold: 1
          failureThreshold: 3
      volumes:
      - name: postgres-storage
        persistentVolumeClaim:
          claimName: location-db-pvc
      - name: init-sql
        configMap:
          name: location-db-init-sql
      restartPolicy: Always
```

#### location-db-service.yaml
```yaml
apiVersion: v1
kind: Service
metadata:
  name: location-db-service
  namespace: tripgen-dev
  labels:
    app: location-db
    component: database
    service: location-service
spec:
  type: ClusterIP
  ports:
  - port: 5432
    targetPort: 5432
    protocol: TCP
    name: postgres
  selector:
    app: location-db
```

### 3단계: PostGIS 초기화 SQL 스크립트 준비

#### location-db-init-configmap.yaml
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: location-db-init-sql
  namespace: tripgen-dev
data:
  01-init-database.sql: |
    -- Database and Schema Creation
    CREATE SCHEMA IF NOT EXISTS tripgen_location;
    SET search_path TO tripgen_location;
    
    -- Extensions (PostGIS 이미지에 이미 설치됨)
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
    CREATE EXTENSION IF NOT EXISTS "postgis";
    CREATE EXTENSION IF NOT EXISTS "btree_gin";
    
    -- pg_trgm 확장 (텍스트 검색용)
    CREATE EXTENSION IF NOT EXISTS "pg_trgm";
    
  02-create-places-table.sql: |
    -- Set search path
    SET search_path TO tripgen_location;
    
    -- places 테이블 (장소 기본 정보)
    CREATE TABLE places (
        id                  BIGSERIAL PRIMARY KEY,
        place_id            VARCHAR(36) UNIQUE NOT NULL DEFAULT uuid_generate_v4()::text,
        external_id         VARCHAR(100),
        provider            VARCHAR(20) NOT NULL,
        name                VARCHAR(200) NOT NULL,
        name_en             VARCHAR(200),
        category            VARCHAR(50) NOT NULL,
        sub_category        VARCHAR(50),
        address             VARCHAR(500) NOT NULL,
        address_en          VARCHAR(500),
        location            GEOMETRY(POINT, 4326) NOT NULL,  -- WGS84 좌표계
        rating              DECIMAL(2,1),
        rating_count        INTEGER DEFAULT 0,
        price_level         INTEGER,
        phone               VARCHAR(30),
        website             VARCHAR(500),
        status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
        is_verified         BOOLEAN NOT NULL DEFAULT FALSE,
        last_updated        TIMESTAMP WITH TIME ZONE,
        created_by          VARCHAR(36),
        updated_by          VARCHAR(36),
        created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
        
        -- 제약조건
        CONSTRAINT chk_places_provider CHECK (provider IN ('GOOGLE', 'KAKAO', 'MANUAL')),
        CONSTRAINT chk_places_category CHECK (category IN ('RESTAURANT', 'TOURIST_ATTRACTION', 'LODGING', 'SHOPPING', 'ENTERTAINMENT', 'TRANSPORTATION', 'SERVICE', 'OTHER')),
        CONSTRAINT chk_places_rating CHECK (rating IS NULL OR (rating >= 0.0 AND rating <= 5.0)),
        CONSTRAINT chk_places_rating_count CHECK (rating_count >= 0),
        CONSTRAINT chk_places_price_level CHECK (price_level IS NULL OR (price_level >= 0 AND price_level <= 4)),
        CONSTRAINT chk_places_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'CLOSED', 'TEMPORARY_CLOSED')),
        CONSTRAINT chk_places_name_length CHECK (LENGTH(name) >= 1 AND LENGTH(name) <= 200)
    );
    
  03-create-place-details-table.sql: |
    -- Set search path
    SET search_path TO tripgen_location;
    
    -- place_details 테이블 (장소 상세 정보)
    CREATE TABLE place_details (
        id                  BIGSERIAL PRIMARY KEY,
        place_id            VARCHAR(36) NOT NULL,
        business_hours      JSONB,
        photos              JSONB,
        reviews             JSONB,
        amenities           JSONB,
        accessibility       JSONB,
        parking_info        JSONB,
        weather_impact      JSONB,
        visit_duration      INTEGER,
        best_visit_time     VARCHAR(50),
        crowd_level         JSONB,
        special_notes       TEXT,
        last_verified       TIMESTAMP WITH TIME ZONE,
        created_by          VARCHAR(36),
        updated_by          VARCHAR(36),
        created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
        
        -- 제약조건
        CONSTRAINT chk_place_details_visit_duration CHECK (visit_duration IS NULL OR (visit_duration >= 15 AND visit_duration <= 480)),
        CONSTRAINT chk_place_details_best_visit_time CHECK (best_visit_time IS NULL OR best_visit_time IN ('MORNING', 'AFTERNOON', 'EVENING', 'NIGHT', 'ANYTIME')),
        
        -- 외래키
        CONSTRAINT fk_place_details_place_id FOREIGN KEY (place_id) REFERENCES places(place_id) ON DELETE CASCADE
    );
    
  04-create-place-recommendations-table.sql: |
    -- Set search path
    SET search_path TO tripgen_location;
    
    -- place_recommendations 테이블 (AI 추천 정보 캐시)
    CREATE TABLE place_recommendations (
        id                      BIGSERIAL PRIMARY KEY,  
        recommendation_id       VARCHAR(36) UNIQUE NOT NULL DEFAULT uuid_generate_v4()::text,
        place_id                VARCHAR(36) NOT NULL,
        user_profile_hash       VARCHAR(64) NOT NULL,
        recommendation_reason   TEXT NOT NULL,
        useful_tips            TEXT,
        context_data           JSONB,
        generated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
        expires_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '24 hours'),
        access_count           INTEGER NOT NULL DEFAULT 0,
        last_accessed          TIMESTAMP WITH TIME ZONE,
        created_by             VARCHAR(36),
        updated_by             VARCHAR(36),
        created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
        
        -- 제약조건
        CONSTRAINT chk_place_recommendations_reason_length CHECK (LENGTH(recommendation_reason) >= 10),
        CONSTRAINT chk_place_recommendations_expires CHECK (expires_at > created_at),
        CONSTRAINT chk_place_recommendations_access_count CHECK (access_count >= 0),
        
        -- 외래키
        CONSTRAINT fk_place_recommendations_place_id FOREIGN KEY (place_id) REFERENCES places(place_id) ON DELETE CASCADE
    );
    
  05-create-indexes.sql: |
    -- Set search path
    SET search_path TO tripgen_location;
    
    -- places 인덱스
    CREATE UNIQUE INDEX idx_places_place_id ON places(place_id);
    CREATE INDEX idx_places_external_id_provider ON places(external_id, provider) WHERE external_id IS NOT NULL;
    CREATE INDEX idx_places_name ON places(name);
    CREATE INDEX idx_places_category ON places(category);
    CREATE INDEX idx_places_status ON places(status);
    CREATE INDEX idx_places_rating ON places(rating) WHERE rating IS NOT NULL;
    CREATE INDEX idx_places_is_verified ON places(is_verified);
    CREATE INDEX idx_places_created_at ON places(created_at);
    CREATE INDEX idx_places_last_updated ON places(last_updated) WHERE last_updated IS NOT NULL;
    
    -- 공간 인덱스 (PostGIS)
    CREATE INDEX idx_places_location_gist ON places USING GIST(location);
    
    -- 텍스트 검색 인덱스
    CREATE INDEX idx_places_name_trgm ON places USING GIN(name gin_trgm_ops);
    CREATE INDEX idx_places_address_trgm ON places USING GIN(address gin_trgm_ops);
    
    -- place_details 인덱스
    CREATE UNIQUE INDEX idx_place_details_place_id ON place_details(place_id);
    CREATE INDEX idx_place_details_visit_duration ON place_details(visit_duration) WHERE visit_duration IS NOT NULL;
    CREATE INDEX idx_place_details_best_visit_time ON place_details(best_visit_time) WHERE best_visit_time IS NOT NULL;
    CREATE INDEX idx_place_details_last_verified ON place_details(last_verified) WHERE last_verified IS NOT NULL;
    CREATE INDEX idx_place_details_business_hours_gin ON place_details USING GIN(business_hours);
    CREATE INDEX idx_place_details_amenities_gin ON place_details USING GIN(amenities);
    
    -- place_recommendations 인덱스
    CREATE UNIQUE INDEX idx_place_recommendations_recommendation_id ON place_recommendations(recommendation_id);
    CREATE INDEX idx_place_recommendations_place_id ON place_recommendations(place_id);
    CREATE INDEX idx_place_recommendations_user_profile_hash ON place_recommendations(user_profile_hash);
    CREATE INDEX idx_place_recommendations_expires_at ON place_recommendations(expires_at);
    CREATE INDEX idx_place_recommendations_active ON place_recommendations(id) WHERE expires_at > CURRENT_TIMESTAMP;
    CREATE INDEX idx_place_recommendations_context_gin ON place_recommendations USING GIN(context_data);
    
    -- 복합 인덱스
    CREATE INDEX idx_places_category_status_rating ON places(category, status, rating DESC) WHERE status = 'ACTIVE';
    CREATE INDEX idx_place_recommendations_place_user ON place_recommendations(place_id, user_profile_hash);
    
  06-create-functions.sql: |
    -- Set search path
    SET search_path TO tripgen_location;
    
    -- 자동 갱신 트리거 함수들
    CREATE OR REPLACE FUNCTION update_places_updated_at()
    RETURNS TRIGGER AS $$
    BEGIN
        NEW.updated_at = CURRENT_TIMESTAMP;
        RETURN NEW;
    END;
    $$ LANGUAGE plpgsql;
    
    CREATE OR REPLACE FUNCTION update_place_details_updated_at()
    RETURNS TRIGGER AS $$
    BEGIN
        NEW.updated_at = CURRENT_TIMESTAMP;
        RETURN NEW;
    END;
    $$ LANGUAGE plpgsql;
    
    CREATE OR REPLACE FUNCTION update_place_recommendations_updated_at()
    RETURNS TRIGGER AS $$
    BEGIN
        NEW.updated_at = CURRENT_TIMESTAMP;
        RETURN NEW;
    END;
    $$ LANGUAGE plpgsql;
    
    -- 장소 정보 검증 함수
    CREATE OR REPLACE FUNCTION validate_place_location()
    RETURNS TRIGGER AS $$
    BEGIN
        -- 좌표 유효성 검사
        IF ST_X(NEW.location) < -180 OR ST_X(NEW.location) > 180 OR
           ST_Y(NEW.location) < -90 OR ST_Y(NEW.location) > 90 THEN
            RAISE EXCEPTION '유효하지 않은 좌표입니다: lat=%, lng=%', ST_Y(NEW.location), ST_X(NEW.location);
        END IF;
        
        -- 한국 내 좌표인지 확인하여 provider 검증
        IF ST_Contains(ST_GeomFromText('POLYGON((124 33, 132 33, 132 39, 124 39, 124 33))', 4326), NEW.location) THEN
            -- 한국 내 좌표면서 GOOGLE provider인 경우 경고
            IF NEW.provider = 'GOOGLE' THEN
                RAISE NOTICE '한국 내 장소는 KAKAO provider 사용을 권장합니다: %', NEW.name;
            END IF;
        END IF;
        
        RETURN NEW;
    END;
    $$ LANGUAGE plpgsql;
    
    -- 장소 검색을 위한 함수
    CREATE OR REPLACE FUNCTION search_places_nearby(
        center_lat DECIMAL,
        center_lng DECIMAL,
        radius_meters INTEGER DEFAULT 1000,
        search_category VARCHAR DEFAULT NULL,
        search_text VARCHAR DEFAULT NULL,
        limit_count INTEGER DEFAULT 20
    )
    RETURNS TABLE(
        place_id VARCHAR,
        name VARCHAR,
        category VARCHAR,
        address VARCHAR,
        distance_meters INTEGER,
        rating DECIMAL,
        rating_count INTEGER
    ) AS $$
    BEGIN
        RETURN QUERY
        SELECT 
            p.place_id,
            p.name,
            p.category,
            p.address,
            ST_Distance(p.location, ST_SetSRID(ST_MakePoint(center_lng, center_lat), 4326))::INTEGER as distance_meters,
            p.rating,
            p.rating_count
        FROM places p
        WHERE p.status = 'ACTIVE'
          AND ST_DWithin(p.location, ST_SetSRID(ST_MakePoint(center_lng, center_lat), 4326), radius_meters * 0.00001)
          AND (search_category IS NULL OR p.category = search_category)
          AND (search_text IS NULL OR (
              p.name ILIKE '%' || search_text || '%' OR 
              p.address ILIKE '%' || search_text || '%'
          ))
        ORDER BY ST_Distance(p.location, ST_SetSRID(ST_MakePoint(center_lng, center_lat), 4326))
        LIMIT limit_count;
    END;
    $$ LANGUAGE plpgsql;
    
    -- 만료된 데이터 정리 함수
    CREATE OR REPLACE FUNCTION cleanup_expired_location_data()
    RETURNS VOID AS $$
    BEGIN
        -- 만료된 추천 정보 삭제 (7일 후)
        DELETE FROM place_recommendations 
        WHERE expires_at < CURRENT_TIMESTAMP - INTERVAL '7 days';
        
        -- 오래된 장소 정보 중 비활성화된 것 정리 (1년 후)
        UPDATE places 
        SET status = 'INACTIVE' 
        WHERE status = 'ACTIVE' 
          AND last_updated < CURRENT_TIMESTAMP - INTERVAL '1 year'
          AND rating_count = 0;
          
        -- 중복 장소 정리 (같은 좌표, 같은 이름)
        WITH duplicate_places AS (
            SELECT place_id, 
                   ROW_NUMBER() OVER (
                       PARTITION BY name, ST_SnapToGrid(location, 0.0001) 
                       ORDER BY rating_count DESC, created_at DESC
                   ) as rn
            FROM places 
            WHERE status = 'ACTIVE'
        )
        UPDATE places 
        SET status = 'INACTIVE' 
        WHERE place_id IN (
            SELECT place_id FROM duplicate_places WHERE rn > 1
        );
    END;
    $$ LANGUAGE plpgsql;
    
  07-create-triggers.sql: |
    -- Set search path
    SET search_path TO tripgen_location;
    
    -- 트리거 생성
    CREATE TRIGGER trg_places_updated_at
        BEFORE UPDATE ON places
        FOR EACH ROW
        EXECUTE FUNCTION update_places_updated_at();
    
    CREATE TRIGGER trg_place_details_updated_at
        BEFORE UPDATE ON place_details
        FOR EACH ROW
        EXECUTE FUNCTION update_place_details_updated_at();
    
    CREATE TRIGGER trg_place_recommendations_updated_at
        BEFORE UPDATE ON place_recommendations
        FOR EACH ROW
        EXECUTE FUNCTION update_place_recommendations_updated_at();
    
    CREATE TRIGGER trg_places_validate_location
        BEFORE INSERT OR UPDATE ON places
        FOR EACH ROW
        EXECUTE FUNCTION validate_place_location();
    
  08-insert-sample-data.sql: |
    -- Set search path
    SET search_path TO tripgen_location;
    
    -- 샘플 장소 데이터 (뮌헨 주요 관광지)
    INSERT INTO places (
        name, name_en, category, address, address_en, location, rating, rating_count, 
        provider, is_verified, created_by, updated_by
    ) VALUES 
    -- 뮌헨 중앙역
    (
        'München Hauptbahnhof', 'Munich Central Station', 'TRANSPORTATION',
        'Bayerstraße 10A, 80335 München, Germany', 'Bayerstraße 10A, 80335 Munich, Germany',
        ST_SetSRID(ST_MakePoint(11.5581, 48.1402), 4326), 4.2, 15847,
        'GOOGLE', TRUE, uuid_generate_v4()::text, uuid_generate_v4()::text
    ),
    -- 마리엔플라츠
    (
        'Marienplatz', 'Marienplatz', 'TOURIST_ATTRACTION',
        'Marienplatz, 80331 München, Germany', 'Marienplatz, 80331 Munich, Germany',
        ST_SetSRID(ST_MakePoint(11.5755, 48.1374), 4326), 4.6, 28392,
        'GOOGLE', TRUE, uuid_generate_v4()::text, uuid_generate_v4()::text
    ),
    -- 뉴슈반슈타인 성
    (
        'Schloss Neuschwanstein', 'Neuschwanstein Castle', 'TOURIST_ATTRACTION',
        'Neuschwansteinstraße 20, 87645 Schwangau, Germany', 'Neuschwansteinstraße 20, 87645 Schwangau, Germany',
        ST_SetSRID(ST_MakePoint(10.7498, 47.5576), 4326), 4.5, 45672,
        'GOOGLE', TRUE, uuid_generate_v4()::text, uuid_generate_v4()::text
    ),
    -- 호프브로이하우스
    (
        'Hofbräuhaus München', 'Hofbrauhaus Munich', 'RESTAURANT',
        'Platzl 9, 80331 München, Germany', 'Platzl 9, 80331 Munich, Germany',
        ST_SetSRID(ST_MakePoint(11.5797, 48.1378), 4326), 4.3, 38291,
        'GOOGLE', TRUE, uuid_generate_v4()::text, uuid_generate_v4()::text
    ),
    -- 영국 정원
    (
        'Englischer Garten', 'English Garden', 'TOURIST_ATTRACTION',
        'München, Germany', 'Munich, Germany',
        ST_SetSRID(ST_MakePoint(11.5820, 48.1642), 4326), 4.7, 22103,
        'GOOGLE', TRUE, uuid_generate_v4()::text, uuid_generate_v4()::text
    );
    
    -- 각 장소에 대한 상세 정보 샘플 데이터
    WITH sample_places AS (
        SELECT place_id FROM places WHERE name = 'Marienplatz'
    )
    INSERT INTO place_details (
        place_id, business_hours, photos, reviews, amenities, 
        visit_duration, best_visit_time, crowd_level, special_notes,
        created_by, updated_by
    ) 
    SELECT 
        sp.place_id,
        '{
            "monday": {"open": "00:00", "close": "23:59"},
            "tuesday": {"open": "00:00", "close": "23:59"},
            "wednesday": {"open": "00:00", "close": "23:59"},
            "thursday": {"open": "00:00", "close": "23:59"},
            "friday": {"open": "00:00", "close": "23:59"},
            "saturday": {"open": "00:00", "close": "23:59"},
            "sunday": {"open": "00:00", "close": "23:59"}
        }'::jsonb,
        '[
            "https://example.com/marienplatz1.jpg",
            "https://example.com/marienplatz2.jpg"
        ]'::jsonb,
        '[
            {
                "author": "TripAdvisor User",
                "rating": 5,
                "text": "뮌헨의 심장부, 꼭 방문해야 할 곳",
                "date": "2024-01-15"
            }
        ]'::jsonb,
        '[
            "공중화장실",
            "스트리트 퍼포먼스",
            "가이드 투어"
        ]'::jsonb,
        90,
        'MORNING',
        '{
            "morning": "medium",
            "afternoon": "high",
            "evening": "medium",
            "night": "low"
        }'::jsonb,
        '글로켄슈필 공연이 매일 11시, 12시, 17시에 있습니다. 주말에는 매우 혼잡하니 평일 오전 방문을 추천합니다.',
        uuid_generate_v4()::text,
        uuid_generate_v4()::text
    FROM sample_places sp;
    
    -- 샘플 AI 추천 데이터
    WITH sample_places AS (
        SELECT place_id FROM places WHERE name = 'Hofbräuhaus München'
    )
    INSERT INTO place_recommendations (
        place_id, user_profile_hash, recommendation_reason, useful_tips, 
        context_data, created_by, updated_by
    ) 
    SELECT 
        sp.place_id,
        'hash_user_profile_001',
        '호프브로이하우스는 1589년에 설립된 전통 맥주집으로, 전통 독일 음식과 맥주를 즐길 수 있는 곣입니다. 여행객이 맛집 탐방을 선호하신다면 꼭 방문해야 할 대표적인 장소입니다.',
        '저녁 7시 이후에는 매우 혼잡하니 오후 시간대에 방문하는 것을 추천합니다. 전통 독일 소시지와 프리체 조합을 꼭 맛보세요. 예약은 받지 않으니 일찍 가셔야 합니다.',
        '{
            "user_preferences": ["맛집탐방", "전통체험"],
            "visit_season": "spring",
            "group_size": 2
        }'::jsonb,
        uuid_generate_v4()::text,
        uuid_generate_v4()::text
    FROM sample_places sp;
    
  99-comments-and-analyze.sql: |
    -- Set search path
    SET search_path TO tripgen_location;
    
    -- 테이블 코멘트
    COMMENT ON TABLE places IS '장소 기본 정보 및 지리정보 관리';
    COMMENT ON TABLE place_details IS '장소 상세 정보 (영업시간, 리뷰, 편의시설 등)';
    COMMENT ON TABLE place_recommendations IS 'AI 추천 정보 캐시 (24시간 TTL)';
    
    -- 컬럼 코멘트
    COMMENT ON COLUMN places.place_id IS '외부 서비스 참조용 비즈니스 키';
    COMMENT ON COLUMN places.location IS 'PostGIS POINT 타입 (WGS84 좌표계)';
    COMMENT ON COLUMN places.provider IS '데이터 제공자 (GOOGLE/KAKAO/MANUAL)';
    COMMENT ON COLUMN place_details.business_hours IS '영업시간 정보 (JSONB)';
    COMMENT ON COLUMN place_details.crowd_level IS '시간대별 혼잡도 정보 (JSONB)';
    COMMENT ON COLUMN place_recommendations.user_profile_hash IS '사용자 프로필 해시 (캐시 키)';
    
    -- PostGIS 버전 확인
    SELECT PostGIS_Version();
    
    -- 성능 통계 수집
    ANALYZE places, place_details, place_recommendations;
```

### 4단계: 리소스 배포

```bash
# 1. ConfigMap과 Secret 생성
kubectl apply -f location-db-configmap.yaml
kubectl apply -f location-db-secret.yaml
kubectl apply -f location-db-init-configmap.yaml

# 2. PVC 생성
kubectl apply -f location-db-pvc.yaml

# 3. Deployment와 Service 생성
kubectl apply -f location-db-deployment.yaml
kubectl apply -f location-db-service.yaml

# 4. 배포 상태 확인
kubectl get all -n tripgen-dev -l app=location-db
```

## PostGIS 확장 설치

### PostGIS 설치 확인

```bash
# Pod 상태 확인
kubectl get pods -n tripgen-dev -l app=location-db

# Pod 로그 확인 (초기화 과정 모니터링)
kubectl logs -n tripgen-dev -l app=location-db -f

# 데이터베이스 접속 테스트
kubectl exec -it -n tripgen-dev deployment/location-db -- psql -U tripgen_location -d tripgen_location_db
```

### 수동 PostGIS 설치 확인

```bash
# Pod 내부 접속
kubectl exec -it -n tripgen-dev deployment/location-db -- bash

# 데이터베이스 접속
psql -U tripgen_location -d tripgen_location_db

# PostGIS 확장 확인
\dx

# PostGIS 버전 확인
SET search_path TO tripgen_location;
SELECT PostGIS_Version();
SELECT PostGIS_Full_Version();

# 스키마 및 테이블 확인
\dt tripgen_location.*

# 샘플 데이터 확인
SET search_path TO tripgen_location;
SELECT place_id, name, category, ST_AsText(location) as coordinates FROM places;
SELECT place_id, LENGTH(business_hours::text) as business_hours_size FROM place_details;
SELECT recommendation_id, LENGTH(recommendation_reason) as reason_length FROM place_recommendations;
```

## 연결 테스트

### 1. 포트 포워딩 설정

```bash
# 로컬에서 데이터베이스 접근
kubectl port-forward -n tripgen-dev service/location-db-service 5435:5432
```

### 2. 외부 클라이언트 연결

```bash
# psql 클라이언트 연결
psql -h localhost -p 5435 -U tripgen_location -d tripgen_location_db

# 연결 정보
# Host: localhost
# Port: 5435
# Database: tripgen_location_db
# Username: tripgen_location
# Password: tripgen_location_123
# Schema: tripgen_location
```

### 3. 애플리케이션 연결 설정

#### Spring Boot application.yml
```yaml
spring:
  datasource:
    url: jdbc:postgresql://location-db-service.tripgen-dev.svc.cluster.local:5432/tripgen_location_db
    username: tripgen_location
    password: tripgen_location_123
    driver-class-name: org.postgresql.Driver
  jpa:
    database-platform: org.hibernate.spatial.dialect.postgis.PostgisPG10Dialect
    properties:
      hibernate:
        default_schema: tripgen_location
        dialect: org.hibernate.spatial.dialect.postgis.PostgisPG10Dialect
```

## 공간 인덱스 설정

### PostGIS 공간 기능 테스트

```sql
SET search_path TO tripgen_location;

-- 기본 공간 연산 테스트
-- 마리엔플라츠 기준 1km 반경 내 장소 검색
SELECT 
    place_id, 
    name, 
    category,
    ST_Distance(
        location, 
        ST_SetSRID(ST_MakePoint(11.5755, 48.1374), 4326)
    ) * 111000 as distance_meters  -- 도 단위를 미터로 변환
FROM places 
WHERE ST_DWithin(
    location, 
    ST_SetSRID(ST_MakePoint(11.5755, 48.1374), 4326), 
    0.01  -- 약 1km
)
ORDER BY ST_Distance(location, ST_SetSRID(ST_MakePoint(11.5755, 48.1374), 4326));

-- 한국 내 좌표 테스트 (서울 상암동)
SELECT ST_Contains(
    ST_GeomFromText('POLYGON((124 33, 132 33, 132 39, 124 39, 124 33))', 4326),
    ST_SetSRID(ST_MakePoint(126.9780, 37.5665), 4326)
) as is_in_korea;

-- 공간 인덱스 사용 확인
EXPLAIN ANALYZE SELECT * FROM places 
WHERE ST_DWithin(location, ST_SetSRID(ST_MakePoint(11.5755, 48.1374), 4326), 0.01);
```

### 사용자 정의 공간 검색 함수 테스트

```sql
SET search_path TO tripgen_location;

-- 주변 장소 검색 함수 테스트
SELECT * FROM search_places_nearby(
    48.1374,     -- 위도 (lat)
    11.5755,     -- 경도 (lng)
    1000,        -- 반경 (미터)
    'RESTAURANT', -- 카테고리 필터
    NULL,        -- 텍스트 검색
    10           -- 결과 수 제한
);

-- 텍스트 검색 포함
SELECT * FROM search_places_nearby(
    48.1374,     -- 위도
    11.5755,     -- 경도
    2000,        -- 2km 반경
    NULL,        -- 모든 카테고리
    'Hofbräu',    -- 'Hofbräu' 포함 검색
    5            -- 상위 5개
);
```

## 문제 해결

### 일반적인 문제

#### 1. PostGIS 확장 로드 실패

```bash
# PostGIS 이미지 확인
kubectl describe pod -n tripgen-dev -l app=location-db | grep Image

# 확장 수동 설치
kubectl exec -it -n tripgen-dev deployment/location-db -- psql -U tripgen_location -d tripgen_location_db
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
```

#### 2. 공간 인덱스 성능 문제

```sql
SET search_path TO tripgen_location;

-- GIST 인덱스 확인
SELECT indexname, indexdef FROM pg_indexes 
WHERE tablename = 'places' AND indexdef LIKE '%GIST%';

-- 인덱스 재구성
REINDEX INDEX idx_places_location_gist;

-- 인덱스 사용 통계
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read 
FROM pg_stat_user_indexes 
WHERE schemaname = 'tripgen_location' AND indexname LIKE '%gist%';
```

#### 3. 텍스트 간색 성능 문제

```sql
SET search_path TO tripgen_location;

-- pg_trgm 확장 확인
SELECT * FROM pg_extension WHERE extname = 'pg_trgm';

-- 텍스트 검색 인덱스 확인
SELECT indexname, indexdef FROM pg_indexes 
WHERE tablename = 'places' AND indexdef LIKE '%gin_trgm_ops%';

-- 텍스트 검색 성능 테스트
EXPLAIN ANALYZE SELECT * FROM places WHERE name ILIKE '%München%';
```

### 데이터 정합성 검사

```sql
SET search_path TO tripgen_location;

-- 좌표 유효성 검사
SELECT place_id, name, ST_X(location) as lng, ST_Y(location) as lat 
FROM places 
WHERE ST_X(location) < -180 OR ST_X(location) > 180 
   OR ST_Y(location) < -90 OR ST_Y(location) > 90;

-- 중복 장소 검사
SELECT name, ST_AsText(location), COUNT(*) 
FROM places 
GROUP BY name, ST_SnapToGrid(location, 0.0001) 
HAVING COUNT(*) > 1;

-- 외래키 무결성 확인
SELECT COUNT(*) FROM place_details pd 
WHERE NOT EXISTS (SELECT 1 FROM places p WHERE p.place_id = pd.place_id);

SELECT COUNT(*) FROM place_recommendations pr 
WHERE NOT EXISTS (SELECT 1 FROM places p WHERE p.place_id = pr.place_id);

-- 만료된 추천 정보 확인
SELECT COUNT(*) as expired_recommendations 
FROM place_recommendations 
WHERE expires_at < CURRENT_TIMESTAMP;
```

### 공간 데이터 최적화

#### VACUUM 및 ANALYZE

```sql
SET search_path TO tripgen_location;

-- 공간 인덱스 통계 업데이트
VACUUM ANALYZE places;
VACUUM ANALYZE place_details;
VACUUM ANALYZE place_recommendations;

-- 공간 인덱스 사용량 확인
SELECT 
    schemaname, 
    tablename, 
    attname, 
    n_distinct, 
    correlation 
FROM pg_stats 
WHERE schemaname = 'tripgen_location' AND tablename = 'places';
```

#### 성능 튜닝

```sql
-- PostGIS를 위한 특별 설정
ALTER SYSTEM SET shared_buffers = '512MB';
ALTER SYSTEM SET effective_cache_size = '1GB'; 
ALTER SYSTEM SET work_mem = '16MB';  -- 공간 연산용
ALTER SYSTEM SET maintenance_work_mem = '256MB';
ALTER SYSTEM SET random_page_cost = 1.1;  -- SSD 환경
SELECT pg_reload_conf();
```

## 개발환경 특화 설정

### 개발용 유틸리티 함수

```sql
SET search_path TO tripgen_location;

-- 개발용 데이터 정리 함수
CREATE OR REPLACE FUNCTION reset_location_dev_data()
RETURNS VOID AS $$
BEGIN
    DELETE FROM place_recommendations;
    DELETE FROM place_details;
    DELETE FROM places;
    
    -- 시퀀스 리셋
    ALTER SEQUENCE places_id_seq RESTART WITH 1;
    ALTER SEQUENCE place_details_id_seq RESTART WITH 1;
    ALTER SEQUENCE place_recommendations_id_seq RESTART WITH 1;
END;
$$ LANGUAGE plpgsql;

-- 샘플 데이터 생성 함수
CREATE OR REPLACE FUNCTION create_sample_location_data()
RETURNS VARCHAR AS $$
DECLARE
    new_place_id VARCHAR;
BEGIN
    -- 샘플 장소 생성 (서울 강남역)
    INSERT INTO places (
        name, category, address, location, provider, rating, rating_count
    ) VALUES (
        '강남역',
        'TRANSPORTATION',
        '서울특별시 강남구 강남대로 390',
        ST_SetSRID(ST_MakePoint(127.0276, 37.4979), 4326),
        'KAKAO',
        4.3,
        1250
    ) RETURNING place_id INTO new_place_id;
    
    -- 관련 상세 정보 생성
    INSERT INTO place_details (place_id, visit_duration, best_visit_time)
    VALUES (new_place_id, 30, 'ANYTIME');
    
    RETURN new_place_id;
END;
$$ LANGUAGE plpgsql;
```

### 대용량 데이터 테스트

```sql
SET search_path TO tripgen_location;

-- 대럵적인 장소 데이터 생성 (테스트용)
DO $$
DECLARE
    i INTEGER;
    lat DECIMAL;
    lng DECIMAL;
BEGIN
    FOR i IN 1..1000 LOOP
        -- 람덤 좌표 생성 (한국 내)
        lat := 33 + (39 - 33) * RANDOM();
        lng := 124 + (132 - 124) * RANDOM();
        
        INSERT INTO places (
            name, category, address, location, provider, rating
        ) VALUES (
            '테스트 장소 ' || i,
            CASE WHEN RANDOM() < 0.3 THEN 'RESTAURANT'
                 WHEN RANDOM() < 0.6 THEN 'TOURIST_ATTRACTION'
                 ELSE 'SHOPPING'
            END,
            '테스트 주소 ' || i,
            ST_SetSRID(ST_MakePoint(lng, lat), 4326),
            'MANUAL',
            1.0 + 4.0 * RANDOM()
        );
    END LOOP;
END $$;

-- 성능 테스트
EXPLAIN ANALYZE SELECT COUNT(*) FROM places 
WHERE ST_DWithin(location, ST_SetSRID(ST_MakePoint(127.0276, 37.4979), 4326), 0.1);
```

### 모니터링 쿼리

```sql
SET search_path TO tripgen_location;

-- 지리적 분포 현황
SELECT 
    category,
    COUNT(*) as count,
    AVG(rating) as avg_rating,
    MIN(ST_Y(location)) as min_lat,
    MAX(ST_Y(location)) as max_lat,
    MIN(ST_X(location)) as min_lng,
    MAX(ST_X(location)) as max_lng
FROM places 
WHERE status = 'ACTIVE'
GROUP BY category
ORDER BY count DESC;

-- 제공자별 통계
SELECT 
    provider,
    COUNT(*) as total_places,
    COUNT(*) FILTER (WHERE is_verified = true) as verified_places,
    AVG(rating) as avg_rating
FROM places 
GROUP BY provider;

-- 추천 정보 통계
SELECT 
    DATE(created_at) as date,
    COUNT(*) as total_recommendations,
    COUNT(*) FILTER (WHERE expires_at > CURRENT_TIMESTAMP) as active_recommendations,
    AVG(access_count) as avg_access_count
FROM place_recommendations 
WHERE created_at >= CURRENT_DATE - INTERVAL '7 days'
GROUP BY DATE(created_at)
ORDER BY date DESC;
```

---

**관련 문서**:
- [User Service 데이터베이스 설치](db-user-dev.md)
- [Trip Service 데이터베이스 설치](db-trip-dev.md)
- [AI Service 데이터베이스 설치](db-ai-dev.md)
- [전체 개발환경 구성 가이드](../README-dev.md)

**PostGIS 관련 문서**:
- [PostGIS 공식 문서](https://postgis.net/documentation/)
- [PostgreSQL Spatial 강의](https://postgis.net/workshops/postgis-intro/)
- [Spring Data JPA Spatial 가이드](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#spatial)