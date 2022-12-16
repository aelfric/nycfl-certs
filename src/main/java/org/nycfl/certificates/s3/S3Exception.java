package org.nycfl.certificates.s3;

public class S3Exception extends RuntimeException{
    public S3Exception(Exception ex) {
        super(ex);
    }
}
