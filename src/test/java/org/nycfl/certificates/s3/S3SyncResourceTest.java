package org.nycfl.certificates.s3;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.nycfl.certificates.util.RestAssuredJsonbExtension;

import java.io.File;
import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nycfl.certificates.TestUtils.givenASuperUser;

@QuarkusTest
@TestHTTPEndpoint(S3SyncResource.class)
@ExtendWith(RestAssuredJsonbExtension.class)
class S3SyncResourceTest {

    @Test
    @DisplayName("can upload a file and list uploaded files")
    void upload() throws Exception{
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


        assertThat(givenASuperUser()
            .when()
            .get("")
            .as(S3SyncResource.PublicListing[].class))
            .hasSize(2)
            .containsAll(
                List.of(
                    new S3SyncResource.PublicListing(
                        new URI("https://static.nycfl.tech/duo.csv").toURL(),
                        "duo.csv"
                    ),
                    new S3SyncResource.PublicListing(
                        new URI("https://static.nycfl.tech/JV-OI.csv").toURL(),
                        "JV-OI.csv"
                    )
                )
            );
    }

}