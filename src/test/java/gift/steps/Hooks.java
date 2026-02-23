package gift.steps;

import gift.model.CategoryRepository;
import gift.model.ProductRepository;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

public class Hooks {

    @LocalServerPort
    private int port;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Before
    public void setUp() {
        RestAssured.port = port;
    }

    @After
    public void tearDown() {
        productRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
    }
}
