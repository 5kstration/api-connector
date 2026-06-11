# API Connector

Spring Boot 기반 외부 데이터 수집 마이크로서비스입니다. 청년정책 API, 보험 공공데이터 API, 카드 상품 크롤링 데이터를 수집하여 MongoDB에 원본(raw) 형태로 저장하고, 저장된 원본 데이터를 Python 스크립트로 정제해 AI-Service RDS(PostgreSQL)로 동기화합니다.

## 기술 스택

- Java 17
- Spring Boot 3.5.14
- Spring Web
- Spring Data MongoDB
- Spring Validation
- MongoDB
- Jsoup
- Python 3
- PyMongo / SQLAlchemy / psycopg2
- Springdoc OpenAPI / Swagger UI
- Actuator / Prometheus
- JUnit 5

## 현재 구현 상태

### 외부 원본 데이터 수집

- 온통청년 청년정책 API 원본 데이터를 수집합니다. (`YOUTH_CENTER`)
- 보험 관련 공공데이터 API 원본 데이터를 수집합니다.
  - 실손보험 API (`INDEMNITY_INSURANCE`)
  - 우체국 보험 베스트 API (`POST_INSURANCE_BEST`)
  - 우체국 보험 상품 API (`POST_INSURANCE_PRODUCT`)
  - 우체국 보험 보장내용 API (`POST_INSURANCE_COVERAGE`)
  - 시민안전보험 API (`SAFE_INSURANCE`)
- 토스 카드라운지 페이지를 Jsoup 기반으로 크롤링하여 카드 상품 원본 데이터를 수집합니다. (`TOSS_CARD_LOUNGE`)
- 수집 데이터는 정제하지 않고 `raw_externals` 컬렉션에 JSON 원본 형태로 저장합니다.
- 같은 `sourceCode + rawHash` 조합이 이미 존재하면 중복 저장하지 않습니다.

### 원본 데이터 조회

- MongoDB `raw_externals` 컬렉션을 페이지 기반으로 조회합니다.
- `sourceCode`, `category`, `externalId`, `status` 조건 필터링을 지원합니다.
- MongoDB ObjectId 기준 단건 조회를 지원합니다.

### 외부 출처 메타데이터

- 외부 출처 정보는 `external_api_metas` 컬렉션에 저장됩니다.
- seed JSON 파일을 통해 애플리케이션 시작 시 외부 출처 메타데이터를 upsert합니다.
- 각 수집 완료 후 `lastSyncedAt`을 갱신합니다.

### 스케줄 동기화

- `@Scheduled` 기반으로 청년정책, 보험, 카드, AI RDS 동기화를 주기 실행합니다.
- cron과 timezone은 설정값으로 주입합니다.
- 기본 timezone은 `Asia/Seoul`입니다.

### AI RDS 동기화

- `/internal/v1/ai-rds-sync/run` API 또는 스케줄러를 통해 Python 스크립트를 실행합니다.
- MongoDB `raw_externals`에서 `POLICY`, `CARD`, `INSURANCE` 원본 데이터를 읽습니다.
- 데이터를 정제하여 AI-Service RDS(PostgreSQL) 테이블로 upsert합니다.
- 동시에 한 번만 실행되도록 중복 실행을 방지합니다.

## 주요 API

| Method | Endpoint | 설명 |
|---|---|---|
| POST | `/internal/v1/sync/youth-policies` | 청년정책 API 원본 데이터 수동 적재 |
| POST | `/internal/v1/sync/insurance-products` | 보험 API 원본 데이터 수동 적재 |
| POST | `/internal/v1/sync/card-products` | 카드 크롤링 원본 데이터 수동 적재 |
| GET | `/internal/v1/raw-externals` | 원본 외부 데이터 목록 조회 |
| GET | `/internal/v1/raw-externals/{id}` | 원본 외부 데이터 단건 조회 |
| POST | `/internal/v1/ai-rds-sync/run` | MongoDB raw 데이터를 AI-Service RDS로 수동 동기화 |

## 로컬 실행

### 필수 인프라

- MongoDB
- PostgreSQL (AI RDS 동기화 실행 시)
- Python 3 (AI RDS 동기화 실행 시)

`application.yml`에는 실제 운영 비밀값을 넣지 않습니다. 로컬/운영 실행 시 환경변수, Secret/ConfigMap 또는 별도 설정 파일을 통해 주입합니다.

```powershell
$env:SERVER_PORT="8081"
$env:SPRING_DATA_MONGODB_URI="mongodb://localhost:27017/api_connector"
```

### 외부 API 설정

청년정책:

```powershell
$env:EXTERNAL_API_YOUTH_CENTER_BASE_URL="<Youth Center base URL>"
$env:EXTERNAL_API_YOUTH_CENTER_POLICY_PATH="<Youth policy path>"
$env:EXTERNAL_API_YOUTH_CENTER_API_KEY="<Youth Center API key>"
$env:EXTERNAL_API_YOUTH_CENTER_PAGE_SIZE="100"
$env:EXTERNAL_API_YOUTH_CENTER_RTN_TYPE="json"
$env:EXTERNAL_API_YOUTH_CENTER_LIST_PAGE_TYPE="1"
```

보험:

```powershell
$env:EXTERNAL_API_INSURANCE_DATA_GO_KR_SERVICE_KEY="<data.go.kr service key>"
$env:EXTERNAL_API_INSURANCE_INDEMNITY_BASE_URL="<indemnity base URL>"
$env:EXTERNAL_API_INSURANCE_INDEMNITY_PATH="<indemnity path>"
$env:EXTERNAL_API_INSURANCE_POST_BEST_BASE_URL="<post best base URL>"
$env:EXTERNAL_API_INSURANCE_POST_BEST_PATH="<post best path>"
$env:EXTERNAL_API_INSURANCE_POST_PRODUCT_BASE_URL="<post product base URL>"
$env:EXTERNAL_API_INSURANCE_POST_PRODUCT_PATH="<post product path>"
$env:EXTERNAL_API_INSURANCE_POST_COVERAGE_BASE_URL="<post coverage base URL>"
$env:EXTERNAL_API_INSURANCE_POST_COVERAGE_PATH="<post coverage path>"
$env:EXTERNAL_API_INSURANCE_SAFE_INSURANCE_BASE_URL="<safe insurance base URL>"
$env:EXTERNAL_API_INSURANCE_SAFE_INSURANCE_PATH="<safe insurance path>"
```

카드:

```powershell
$env:EXTERNAL_API_CARD_TOSS_CARD_LOUNGE_URL="<Toss Card Lounge URL>"
```

### Seed 파일 설정

외부 출처 메타데이터와 시민안전보험 지역 목록을 파일로 주입할 수 있습니다.

```powershell
$env:EXTERNAL_API_META_SEED_FILE="file:C:/path/to/external-api-metas.json"
$env:EXTERNAL_API_INSURANCE_SAFE_INSURANCE_REGION_FILE="file:C:/path/to/safe-insurance-seoul-regions.json"
```

### 스케줄 설정

```powershell
$env:SYNC_YOUTH_POLICY_CRON="0 0 3 * * *"
$env:SYNC_YOUTH_POLICY_ZONE="Asia/Seoul"
$env:SYNC_INSURANCE_PRODUCT_CRON="0 30 3 * * *"
$env:SYNC_INSURANCE_PRODUCT_ZONE="Asia/Seoul"
$env:SYNC_CARD_PRODUCT_CRON="0 0 4 * * *"
$env:SYNC_CARD_PRODUCT_ZONE="Asia/Seoul"
$env:SYNC_AI_RDS_CRON="0 0 9 * * *"
$env:SYNC_AI_RDS_ZONE="Asia/Seoul"
```

### AI RDS 동기화 설정

Python 의존성 설치:

```powershell
pip install -r scripts/ai_rds_sync/requirements.txt
```

RDS 및 스크립트 실행 설정:

```powershell
$env:MONGODB_URI="mongodb://localhost:27017/api_connector"
$env:DB_HOST="localhost"
$env:DB_PORT="5432"
$env:DB_NAME="<ai service db name>"
$env:DB_USER="<db user>"
$env:DB_PASSWORD="<db password>"
$env:AI_RDS_SYNC_LIMIT="1000"
$env:AI_RDS_SYNC_CREATE_TABLES="false"
$env:AI_RDS_SYNC_PYTHON_COMMAND="python"
$env:AI_RDS_SYNC_WORKING_DIRECTORY="scripts/ai_rds_sync"
$env:AI_RDS_SYNC_SCRIPT_PATH="scripts/ai_rds_sync/sync_mongo_to_rds.py"
$env:AI_RDS_SYNC_TIMEOUT_MINUTES="30"
```

실행:

```powershell
.\gradlew.bat bootRun
```

## DB 적재 방식

### raw_externals

외부 API 응답 또는 크롤링 결과가 정제 전 원본으로 저장됩니다.

```text
id            = MongoDB ObjectId
sourceCode    = 외부 출처 코드
category      = 데이터 분류 (POLICY, CARD, INSURANCE)
endpoint      = 호출한 API path 또는 크롤링 URL
requestParams = 외부 API 호출에 사용한 요청 파라미터
externalId    = 외부 API/크롤링 데이터의 고유 ID, 없으면 null
rawPayload    = 외부 응답 원본 JSON
rawHash       = 원본 중복 저장 방지를 위한 SHA-256 hash
fetchedAt     = 수집 일시
status        = 수집 상태 (예: SUCCESS)
```

중복 방지 기준:

```text
sourceCode + rawHash
```

### external_api_metas

외부 API 또는 크롤링 출처의 메타데이터가 저장됩니다.

```text
id           = MongoDB ObjectId
sourceCode   = 외부 출처 코드
sourceName   = 사람이 읽기 쉬운 출처 이름
sourceType   = 수집 방식 (API, CRAWLING 등)
category     = 데이터 분류 (POLICY, CARD, INSURANCE)
baseUrl      = 외부 API 또는 크롤링 대상 기본 URL
authType     = 인증 방식 (NONE, API_KEY, SERVICE_KEY 등)
enabled      = 동기화 대상 여부
syncCycle    = 동기화 주기 cron
lastSyncedAt = 마지막 동기화 일시
createdAt    = 생성 일시
updatedAt    = 업데이트 일시
```

## Swagger

서버 실행 후 아래 경로에서 API 명세를 확인할 수 있습니다.

```text
http://<api-connector-host>:8081/swagger-ui.html
```

로컬 실행 예시:

```text
http://localhost:8081/swagger-ui.html
```

## 테스트

```powershell
.\gradlew.bat test
```
