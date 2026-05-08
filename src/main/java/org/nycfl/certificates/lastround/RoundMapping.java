package org.nycfl.certificates.lastround;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import org.nycfl.certificates.EliminationRound;

@Entity
public class RoundMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    String roundRaw;

    @JsonbTransient
    @ManyToOne
    private EventMapping eventMapping;

    @JsonbProperty("roundRaw")
    public String getRoundRaw() {
        return roundRaw;
    }

    @JsonbProperty("roundType")
    public EliminationRound getRoundEnum() {
        return roundEnum;
    }

    public void setRoundEnum(EliminationRound roundEnum) {
        this.roundEnum = roundEnum;
    }

    @Enumerated(EnumType.STRING)
    EliminationRound roundEnum;

    public RoundMapping setRoundRaw(String round) {
        this.roundRaw = round;
        return this;
    }

    public RoundMapping setEventMapping(EventMapping eventMapping) {
        this.eventMapping = eventMapping;
        return this;
    }
}
