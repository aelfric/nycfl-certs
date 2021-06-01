package org.nycfl.certificates;

import java.util.Locale;

public class CongressPOResultFormatter implements ResultFormatter{

    @Override
    public String getPlacementString(Result result) {
        return "Presiding Officer - " + result.eliminationRound.roundName;
    }

    @Override
    public String getCertificateColor(Result result) {
        int place = result.place;
        Event event = result.event;
        String round = result.eliminationRound.label.toLowerCase(Locale.ROOT);
        if(place>=event.getPlacementCutoff()){
            return "";
        }
        return "po "+ round;
    }
}
