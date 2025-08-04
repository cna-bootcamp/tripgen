# Trip Service Dockerfile
# Clean Architecture 기반 Trip Service 컨테이너화

# Build stage
FROM gradle:8.5-jdk21-alpine AS build

WORKDIR /app

# Copy build files
COPY build.gradle .
COPY gradle.properties .
COPY settings.gradle .

# Copy source code
COPY src ./src

# Build application
RUN gradle clean build -x test --no-daemon

# Runtime stage
FROM openjdk:21-jdk-slim

# Install dependencies and create user
RUN apt-get update && \
    apt-get install -y curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    groupadd -r tripgen && \
    useradd -r -g tripgen -d /app -s /bin/false tripgen

WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/build/libs/trip-service.jar app.jar

# Create directories and set permissions
RUN mkdir -p /app/logs /app/temp && \
    chown -R tripgen:tripgen /app

# Switch to non-root user
USER tripgen

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8082/actuator/health || exit 1

# Environment variables
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:G1HeapRegionSize=16m -XX:+UseStringDeduplication" \
    SPRING_PROFILES_ACTIVE=prod \
    TZ=Asia/Seoul

# Expose port
EXPOSE 8082

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# Labels for metadata
LABEL maintainer="TripGen Development Team <dev@tripgen.com>" \
      version="1.0.0" \
      description="Trip Service - Clean Architecture based microservice for trip management" \
      service.name="trip-service" \
      service.port="8082"