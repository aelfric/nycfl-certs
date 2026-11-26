package org.nycfl.certificates.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.json.bind.annotation.JsonbDateFormat;
import org.nycfl.certificates.Tournament;

import java.time.LocalDate;

public record CreateTournamentRequest(
    @NotNull String name,
    @NotNull String host,
    @NotNull @JsonbDateFormat(value = "yyyy-MM-dd")
    LocalDate date
) {

    public Tournament toTournament() {
        Tournament tournament = new Tournament();
        tournament.setName(name);
        tournament.setHost(host);
        tournament.setDate(date);
        return tournament;
    }
}
