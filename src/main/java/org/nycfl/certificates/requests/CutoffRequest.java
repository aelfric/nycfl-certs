package org.nycfl.certificates.requests;

import jakarta.validation.constraints.NotNull;

public record CutoffRequest(@NotNull int cutoff) {
}
