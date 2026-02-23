# spring-gift-test

## 프로젝트 소개
Spring Boot 기반 선물하기(Gift) API의 인수 테스트 프로젝트입니다.
Cucumber + RestAssured를 활용한 BDD 스타일 인수 테스트를 작성합니다.

## 기술 스택
- Java 21, Spring Boot 3.5.8, Spring Data JPA, H2
- 테스트: Cucumber 7.22.0, RestAssured, JUnit 5, Mockito

## 테스트 구조

```
src/test/
├── java/gift/
│   ├── CucumberTest.java                  # Cucumber 실행 진입점 (@Suite)
│   ├── CucumberSpringConfiguration.java   # Spring 컨텍스트 + MockitoBean 설정
│   ├── steps/
│   │   ├── SharedContext.java             # 시나리오 간 상태 공유 (@ScenarioScope)
│   │   ├── Hooks.java                     # @Before(포트 설정), @After(데이터 정리)
│   │   ├── CategorySteps.java             # 카테고리 관련 Step Definitions
│   │   ├── ProductSteps.java              # 상품 관련 Step Definitions
│   │   └── GiftSteps.java                 # 선물 관련 Step Definitions
│   └── ui/                                # 기존 JUnit + RestAssured 테스트 (레거시)
│       ├── CategoryRestControllerTest.java
│       ├── ProductRestControllerTest.java
│       └── GiftRestControllerTest.java
└── resources/features/
    ├── category.feature                   # 카테고리 생성/조회 시나리오 (6개)
    ├── product.feature                    # 상품 등록/조회 시나리오 (2개)
    └── gift.feature                       # 선물 보내기 시나리오 (5개)
```

## 테스트 시나리오

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

## 실행 방법

```bash
# 전체 테스트 실행
./gradlew test

# Cucumber 테스트만 실행
./gradlew test --tests "gift.CucumberTest"

# 기존 JUnit 테스트만 실행
./gradlew test --tests "gift.ui.*"
```
