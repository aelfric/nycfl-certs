package org.nycfl.certificates;

import io.quarkus.qute.TemplateExtension;

import static io.quarkus.qute.TemplateExtension.ANY;

public enum CertificateType implements LabeledEnum {
    PLACEMENT("Placement"),
    DEBATE_SPEAKER("Top Speaker"),
    DEBATE_RECORD("Debate Record"),
    CONGRESS_PO("Presiding Officer"),
    QUALIFIER("Qualifier");

    private final String label;

    CertificateType(String label) {
        this.label = label;
    }

    @TemplateExtension(namespace = "CertificateType", matchName = ANY)
    static CertificateType getVal(String val) {
        return CertificateType.valueOf(val.toUpperCase());
    }

    @Override
    public String getLabel() {
        return this.label;
    }

    @Override
    public String getValue() {
        return this.name();
    }
}
