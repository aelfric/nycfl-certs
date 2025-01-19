package org.nycfl.certificates.s3;

import java.io.Serial;

public class S3Exception extends RuntimeException{

    @Serial
    private static final long serialVersionUID = -345255109320232214L;

    public S3Exception(Exception ex) {
        super(ex);
    }
}
