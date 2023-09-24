package org.nycfl.certificates;

import io.restassured.specification.RequestSpecification;
import io.smallrye.jwt.build.Jwt;

import java.util.Collections;
import java.util.Set;

import static io.restassured.RestAssured.given;

public class TestUtils {
    public static String getToken(Set<String> roles) {
        return Jwt.preferredUserName("frank")
            .groups(roles)
            .issuer("https://server.example.com")
            .audience("https://service.example.com")
            .jws()
            .keyId("1")
            .sign();
    }

    public static RequestSpecification givenASuperUser() {
        return given()
            .auth()
            .oauth2(getToken(Collections.singleton("superuser")));
    }

    public static RequestSpecification givenARegularUser() {
        return given()
            .auth()
            .oauth2(getToken(Collections.singleton("basicuser")));
    }
}
