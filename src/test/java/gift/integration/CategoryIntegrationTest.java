package gift.integration;

import gift.model.CategoryRepository;
import gift.model.GiftDelivery;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
@Tag("integration")
@Testcontainers
class CategoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private CategoryRepository categoryRepository;

    @MockitoBean
    private GiftDelivery giftDelivery;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @AfterEach
    void tearDown() {
        categoryRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("PostgreSQL: 유효한 이름으로 카테고리를 생성하면 200 OK와 생성된 카테고리를 반환한다")
    void create_validName_returnsCreatedCategory() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "식품"))
        .when()
            .post("/api/categories")
        .then()
            .statusCode(200)
            .body("id", notNullValue())
            .body("name", equalTo("식품"));
    }

    @Test
    @DisplayName("PostgreSQL: 생성된 카테고리는 목록 조회 시 포함된다")
    void create_thenRetrieve_containsCreatedCategory() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "식품"))
        .when()
            .post("/api/categories")
        .then()
            .statusCode(200);

        given()
        .when()
            .get("/api/categories")
        .then()
            .statusCode(200)
            .body("name", hasItem("식품"));
    }
}
