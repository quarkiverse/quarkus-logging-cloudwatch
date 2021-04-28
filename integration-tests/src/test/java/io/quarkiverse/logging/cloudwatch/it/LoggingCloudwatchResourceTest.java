package io.quarkiverse.logging.cloudwatch.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class LoggingCloudwatchResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/logging-cloudwatch")
                .then()
                .statusCode(200)
                .body(is("Hello logging-cloudwatch"));
    }
}
