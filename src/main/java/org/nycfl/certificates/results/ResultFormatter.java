package org.nycfl.certificates.results;

public interface ResultFormatter {
    String getPlacementString(Result result);
    String getCertificateColor(Result result);
}
