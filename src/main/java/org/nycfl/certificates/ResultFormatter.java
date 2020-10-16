package org.nycfl.certificates;

public interface ResultFormatter {
    String getPlacementString(Result result);
    String getCertificateColor(Result result);
}
