package org.nycfl.certificates;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record TournamentStub(@Schema(required = true) long id, @Schema(required = true) String name) {
}
