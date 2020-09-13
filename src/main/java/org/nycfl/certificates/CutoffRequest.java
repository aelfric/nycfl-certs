package org.nycfl.certificates;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;

public class CutoffRequest {
    final int cutoff;

    @JsonbCreator
    public CutoffRequest(@JsonbProperty("cutoff") int cutoff) {
        this.cutoff = cutoff;
    }
}
