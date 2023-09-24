package org.nycfl.certificates.results;

import org.nycfl.certificates.EliminationRound;
import org.nycfl.certificates.Event;
import org.nycfl.certificates.Ordinals;

import java.util.Locale;
import java.util.Map;

public class DebateResultFormatter implements ResultFormatter{
    static final Map<Integer, String> PLACE_MAP = Map.of(
        1,"Champion",
        2, "Finalist");

    @Override
    public String getPlacementString(Result result) {
        Event event = result.event;
        if (result.place > event.getCertificateCutoff()) {
            return "";
        }

        if (result.eliminationRound == EliminationRound.FINALIST) {
            if (event.getPlacementCutoff() > 1) {
                return Ordinals.ofInt(result.place) + " Place";
            } else {
                return PLACE_MAP.get(result.place);
            }
        } else {
            return result.eliminationRound.label;
        }
    }

    @Override
    public String getCertificateColor(Result result) {
        if (result.eliminationRound == EliminationRound.FINALIST) {
            if (result.place == 1) {
                return "gold";
            } else {
                return "silver";
            }
        } else if (result.eliminationRound == EliminationRound.SEMIFINALIST) {
            return "bronze";
        } else if (result.eliminationRound == EliminationRound.QUARTER_FINALIST) {
            return "red";
        } else {
            return "black " + result.eliminationRound.label.toLowerCase(Locale.ROOT);
        }
    }
}
