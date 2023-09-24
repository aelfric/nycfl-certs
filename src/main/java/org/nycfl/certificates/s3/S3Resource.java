package org.nycfl.certificates.s3;


import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public abstract class S3Resource {

    @ConfigProperty(name = "bucket.name")
    String bucketName;

    protected PutObjectRequest buildPublicPutRequest(String fileName) {
        return PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .acl(ObjectCannedACL.PUBLIC_READ)
                .build();
    }

    protected File uploadToTemp(InputStream data) {
        File tempPath;
        try {
            tempPath = File.createTempFile("uploadS3Tmp", ".tmp");
            Files.copy(data, tempPath.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) {
            throw new S3Exception(ex);
        }

        return tempPath;
    }
}