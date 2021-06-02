package org.nycfl.certificates;

import java.util.Locale;

public class DebateResultFormatter implements ResultFormatter{

    @Override
    public String getPlacementString(Result result) {
        Event event = result.event;

        if (result.place <= event.getCertificateCutoff()) {
            if (result.eliminationRound == EliminationRound.FINALIST) {
                boolean useNumbers = event.getPlacementCutoff() > 1;
                return result.place == 1 ?
                    useNumbers ?
                        Ordinals.ofInt(result.place) + " Place" :
                        "Champion" :
                    useNumbers ?
                        Ordinals.ofInt(result.place) + " Place" :
                        "Finalist";
            } else {
                return result.eliminationRound.label;
            }
        } else {
            return "";
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
