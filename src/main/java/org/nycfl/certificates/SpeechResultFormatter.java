package org.nycfl.certificates;

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
        if(place==1){
            return "gold "+round;
        } if (place==2){
            return "silver " + round;
        } if (place == 3){
            return "bronze " + round;
        } else {
            return "red " + round;
        }
    }
}
