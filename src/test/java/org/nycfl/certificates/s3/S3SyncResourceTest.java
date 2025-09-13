package org.nycfl.certificates.s3;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.nycfl.certificates.TestUtils.givenASuperUser;

@QuarkusTest
@TestHTTPEndpoint(S3SyncResource.class)
class S3SyncResourceTest {

    @Test
    @DisplayName("can upload a file and list uploaded files")
    void upload() {
        givenASuperUser()
            .multiPart(new File("src/test/resources/JV-OI.csv"))
            .when()
            .post("/upload")
            .then()
            .statusCode(201);

        givenASuperUser()
            .multiPart(new File("src/test/resources/duo.csv"))
            .when()
            .post("/upload")
            .then()
            .statusCode(201);


        givenASuperUser()
            .when()
            .get("")
            .then()
            .body("size()", equalTo(2))
            .body("url", hasItems(
                "https://static.nycfl.tech/duo.csv",
                "https://static.nycfl.tech/JV-OI.csv"
            ))
            .body("objectName", hasItems(
                "duo.csv",
                "JV-OI.csv"
            ));
    }

}