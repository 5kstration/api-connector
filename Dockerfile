# =========================================================================
# Stage 1: Build Stage
# =========================================================================
FROM gradle:8.12-jdk17-alpine AS builder

WORKDIR /build

# Gradle dependency cache is prepared before copying the whole source tree.
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon || true

COPY src src
RUN gradle bootJar -x test --no-daemon

# =========================================================================
# Stage 2: Runtime Stage
# =========================================================================
FROM eclipse-temurin:17-jre-alpine AS runner

WORKDIR /app

# Python is required because API-Connector runs the MongoDB -> AI RDS sync script.
RUN apk add --no-cache python3 py3-pip \
    && python3 -m venv /opt/venv

ENV PATH="/opt/venv/bin:${PATH}"

COPY scripts/ai_rds_sync /app/scripts/ai_rds_sync
RUN pip install --no-cache-dir -r /app/scripts/ai_rds_sync/requirements.txt

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /build/build/libs/*.jar app.jar

RUN mkdir -p /app/config \
    && chown -R appuser:appgroup /app /opt/venv

USER appuser

# Runtime Spring config and seed JSON files should be mounted into /app/config.
ENV SPRING_CONFIG_ADDITIONAL_LOCATION=file:/app/config/
ENV EXTERNAL_API_META_SEED_FILE=file:/app/config/external-api-metas.json
ENV EXTERNAL_API_INSURANCE_SAFE_INSURANCE_REGION_FILE=file:/app/config/safe-insurance-seoul-regions.json
ENV AI_RDS_SYNC_WORKING_DIRECTORY=/app/scripts/ai_rds_sync
ENV AI_RDS_SYNC_SCRIPT_PATH=/app/scripts/ai_rds_sync/sync_mongo_to_rds.py
ENV AI_RDS_SYNC_PYTHON_COMMAND=python

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD wget -q -O - http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-Dsun.net.inetaddr.ttl=60", \
            "-jar", \
            "app.jar"]
