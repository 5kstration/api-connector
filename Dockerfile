# =========================================================================
# Stage 1: Build Stage
# =========================================================================
FROM gradle:8.12-jdk17-alpine AS builder

WORKDIR /build

# Gradle 캐시 효율화를 위해 빌드 설정 파일을 먼저 복사합니다.
COPY build.gradle settings.gradle ./

# 의존성 캐시를 미리 구성합니다. 실제 컴파일 실패와 무관한 캐시 준비 단계입니다.
RUN gradle dependencies --no-daemon || true

# 전체 소스 복사 후 Spring Boot 실행 JAR를 생성합니다.
COPY src src
RUN gradle bootJar -x test --no-daemon

# =========================================================================
# Stage 2: Runtime Stage
# =========================================================================
FROM eclipse-temurin:17-jre-alpine AS runner

WORKDIR /app

# 컨테이너 보안을 위해 non-root 사용자로 실행합니다.
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /build/build/libs/*.jar app.jar

RUN mkdir -p /app/config \
    && chown -R appuser:appgroup /app

USER appuser

# 운영 설정과 seed JSON은 이미지에 포함하지 않고 /app/config에 마운트합니다.
ENV SPRING_CONFIG_ADDITIONAL_LOCATION=file:/app/config/
ENV EXTERNAL_API_META_SEED_FILE=file:/app/config/external-api-metas.json
ENV SAFE_INSURANCE_REGION_FILE=file:/app/config/safe-insurance-seoul-regions.json

# API-Connector 기본 포트는 8081입니다.
EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD wget -q -O - http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-Dsun.net.inetaddr.ttl=60", \
            "-jar", \
            "app.jar"]
