package org.nycfl.certificates.s3;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.nycfl.certificates.util.RestAssuredJsonbExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.File;
import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nycfl.certificates.TestUtils.givenASuperUser;

@QuarkusTest
@TestHTTPEndpoint(S3SyncResource.class)
@ExtendWith(RestAssuredJsonbExtension.class)
class S3SyncResourceTest {
    @Inject
    S3Client s3Client;

    @Test
    @DisplayName("can upload a file and list uploaded files")
    void upload() throws Exception {
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
            .containsAll(
                List.of(
                    new S3SyncResource.PublicListing(
                        new URI("https://static.nycfl.tech/duo.csv").toURL(),
                        "duo.csv",
                        "duo"
                    ),
                    new S3SyncResource.PublicListing(
                        new URI("https://static.nycfl.tech/JV-OI.csv").toURL(),
                        "JV-OI.csv",
                        "JV-OI"
                    )
                )
            );
    }

    @Test
    @DisplayName("can get search for files by prefix")
    void filter() throws Exception {
        List<String> keys = List.of(
            "tournaments/1/DUO.pdf",
            "tournaments/1/Public Forum Debate.pdf",
            "tournaments/2/Dramatic Performance.pdf"
        );
        keys.forEach(
            name -> s3Client.putObject(
                b -> b
                    .bucket("nycfl-certs")
                    .key(name),
                RequestBody.fromString("data")
            )
        );
        assertThat(givenASuperUser()
            .queryParam("prefix", "tournaments/1")
            .when()
            .get("")
            .as(S3SyncResource.PublicListing[].class))
            .hasSize(2)
            .containsExactlyInAnyOrder(
                new S3SyncResource.PublicListing(
                    new URI("https://static.nycfl.tech/tournaments/1/DUO.pdf").toURL(),
                    "tournaments/1/DUO.pdf",
                    "DUO"
                ),
                new S3SyncResource.PublicListing(
                    new URI("https://static.nycfl.tech/tournaments/1/Public%20Forum%20Debate.pdf").toURL(),
                    "tournaments/1/Public Forum Debate.pdf",
                    "Public Forum Debate"
                )
            );


    }

}