# spring-gift-test

## 프로젝트 소개
Spring Boot 기반 선물하기(Gift) API의 인수 테스트 프로젝트입니다.
Cucumber + RestAssured를 활용한 BDD 스타일 인수 테스트를 작성합니다.

## 기술 스택
- Java 21, Spring Boot 3.5.8, Spring Data JPA, PostgreSQL 17
- Docker Compose (`spring-boot-docker-compose`로 자동 관리)
- 테스트: Cucumber 7.22.0, RestAssured, JUnit 5, Mockito

## 사전 요구사항
- Java 21
- Docker (Docker Desktop 또는 Colima 등)

## 실행 환경 구성

### DB 구조

개발과 테스트 DB를 Spring 프로파일로 분리하여 독립적으로 운영한다.

```
개발 (기본 프로파일)              테스트 (cucumber 프로파일)
─────────────────────           ──────────────────────────
docker-compose.yml              docker-compose-test.yml
  └─ db (port 5432)               └─ test-db (port 5433)
  └─ DB: gift                      └─ DB: gift_test
  └─ volume: gift-data             └─ volume 없음 (휘발성)
  └─ ddl-auto: update              └─ ddl-auto: create-drop
```

`spring-boot-docker-compose`가 앱/테스트 시작 시 Docker 컨테이너를 자동으로 기동하고 datasource를 구성한다.
별도로 `docker compose up`을 실행할 필요 없다.

### 개발 서버 실행

```bash
./gradlew bootRun
```

- `docker-compose.yml`의 PostgreSQL(포트 5432)이 자동 시작된다.
- 데이터는 Docker volume(`gift-data`)에 영속 저장된다.

### 테스트 실행

```bash
# 전체 테스트 (JUnit + Cucumber)
./gradlew test

# Cucumber BDD 테스트만
./gradlew cucumberTest

# 기존 JUnit 인수 테스트만
./gradlew test --tests "gift.ui.*"

# 특정 Cucumber 테스트 클래스 실행 (IDE에서도 동일)
./gradlew test --tests "gift.CucumberTest"
```

- `docker-compose-test.yml`의 PostgreSQL(포트 5433)이 자동 시작된다.
- `lifecycle-management=start-only`이므로 테스트 종료 후에도 컨테이너가 유지되어 반복 실행이 빠르다.
- 컨테이너를 수동으로 정리하려면: `docker-compose -f docker-compose-test.yml down`

## 테스트 구조

```
src/test/
├── java/gift/
│   ├── CucumberTest.java                  # Cucumber 실행 진입점 (@Suite)
│   ├── CucumberSpringConfiguration.java   # Spring 컨텍스트 + MockitoBean 설정
│   ├── steps/
│   │   ├── SharedContext.java             # 시나리오 간 상태 공유 (@ScenarioScope)
│   │   ├── Hooks.java                     # @Before(포트 설정 + 데이터 정리)
│   │   ├── CategorySteps.java             # 카테고리 관련 Step Definitions
│   │   ├── ProductSteps.java              # 상품 관련 Step Definitions
│   │   └── GiftSteps.java                 # 선물 관련 Step Definitions
│   └── ui/                                # JUnit + RestAssured 인수 테스트
│       ├── CategoryRestControllerTest.java
│       ├── ProductRestControllerTest.java
│       └── GiftRestControllerTest.java
└── resources/features/
    ├── category.feature                   # 카테고리 생성/조회 시나리오 (6개)
    ├── product.feature                    # 상품 등록/조회 시나리오 (2개)
    └── gift.feature                       # 선물 보내기 시나리오 (5개)
```

### 테스트 시나리오

시나리오 생명주기:

```
시나리오 시작
├─ @Before: DB 전체 삭제 (자식→부모 순) + RestAssured.port 설정
├─ SharedContext 새 인스턴스 생성
├─ Step 클래스들 새 인스턴스 생성
├─ Background 실행 (Given 단계)
├─ Scenario 본문 실행 (When/Then)
└─ 시나리오 종료
```

### 카테고리 관리 (category.feature)
- 유효한 이름으로 카테고리를 생성하면 200 OK와 생성된 카테고리를 반환한다
- 생성된 카테고리는 목록 조회 시 포함된다
- 빈 이름으로 카테고리를 생성하면 빈 이름으로 저장된다
- name 필드 누락 시 null로 저장된다
- 카테고리 목록을 조회한다
- 동일한 이름의 카테고리를 여러 개 생성할 수 있다

### 상품 관리 (product.feature)
- 유효한 상품을 등록한다
- 상품 목록을 조회한다

### 선물 보내기 (gift.feature)
- 유효한 요청으로 선물을 보내면 200 OK와 재고가 차감된다
- 존재하지 않는 옵션으로 선물을 보내면 500 에러가 발생한다
- 재고보다 많은 수량을 요청하면 500 에러가 발생한다
- Member-Id 헤더가 없으면 400 에러가 발생한다
- 재고와 동일한 수량을 요청하면 200 OK와 재고가 0이 된다

## 테스트 전략

### 데이터 설정 (Given)

인수 테스트는 사용자 관점에서 작성하므로, 테스트 데이터도 **API 호출**로 생성하는 것을 원칙으로 한다.
API가 존재하지 않는 경우에만 Repository 직접 접근을 허용한다.

| 데이터 | 방식 | 이유 |
|--------|------|------|
| 카테고리 | API 호출 (`POST /api/categories`) | API 존재 |
| 상품 | API 호출 (`POST /api/products`) | API 존재 |
| 옵션 | Repository 직접 접근 | 생성 API 없음 |
| 회원 | Repository 직접 접근 | 생성 API 없음 |

### 검증 (Then)

검증 방법은 다음 우선순위를 따른다.

| 우선순위 | 방법 | 설명 | 적용 예시 |
|---------|------|------|----------|
| 1순위 | 조회 API 활용 | 생성 후 조회 API로 결과를 확인한다 | 카테고리 생성 → 목록 조회로 확인 |
| 2순위 | 실패 시나리오 간접 검증 | 동일 행위를 반복하여 실패하는지 확인한다 | 재고 소진 후 추가 선물 시 실패 |
| 3순위 | DB 직접 확인 | Repository로 DB를 직접 조회하여 검증한다 | 선물 후 옵션 재고 확인 (조회 API 없음) |

### 데이터 격리

- `@Before` 훅에서 `deleteAllInBatch()`로 매 시나리오 시작 전 데이터를 정리한다
  - 이전 시나리오가 실패하더라도 다음 시나리오는 항상 깨끗한 DB에서 시작된다
  - 외래키 제약 순서: Option → Product → Category → Member
- `@ScenarioScope`로 SharedContext를 시나리오마다 새로 생성하여 상태 누출을 방지한다
- RestAssured는 별도 스레드에서 HTTP 요청을 보내므로 `@Transactional` 롤백이 동작하지 않아 수동 삭제가 필요하다

## Spring 프로파일 설정

| 프로파일 | 설정 파일 | Docker Compose 파일 | DB | 용도 |
|---------|----------|--------------------|----|------|
| 기본 | `application.properties` | `docker-compose.yml` | PostgreSQL (port 5432, `gift`) | 개발 |
| cucumber | `application-cucumber.properties` | `docker-compose-test.yml` | PostgreSQL (port 5433, `gift_test`) | 테스트 |

테스트 클래스에 `@ActiveProfiles("cucumber")`가 적용되어 있어 테스트 시 자동으로 테스트 전용 DB를 사용한다.
