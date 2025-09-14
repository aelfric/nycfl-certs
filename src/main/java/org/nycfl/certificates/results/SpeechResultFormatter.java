package org.nycfl.certificates.results;

import org.nycfl.certificates.Event;
import org.nycfl.certificates.Ordinals;

import java.util.Locale;

public class SpeechResultFormatter implements ResultFormatter{

    @Override
    public String getPlacementString(Result result) {
        Event event = result.event;
        if(result.place< event.getPlacementCutoff()){
            return Ordinals.ofInt(result.place) + " Place";
        } else if (result.place < event.getCertificateCutoff()){
            return result.eliminationRound.label;
        } else {
            return "";
        }

    }

    @Override
    public String getCertificateColor(Result result) {
        int place = result.place;
        Event event = result.event;
        String round = result.eliminationRound.label.toLowerCase(Locale.ROOT);
        if(place>=event.getPlacementCutoff()){
            return "black " + round;
        }
        return switch (place) {
            case 1 -> "gold " + round;
            case 2 -> "silver " + round;
            case 3 -> "bronze " + round;
            default -> "red " + round;
        };
    }
}
