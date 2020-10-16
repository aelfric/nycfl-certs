package org.nycfl.certificates;

public class SpeechResultFormatter implements ResultFormatter{
    String[] NUMBER_ORDINALS = {
        "",
        "First",
        "Second",
        "Third",
        "Fourth",
        "Fifth",
        "Sixth",
        "Seventh",
        "Eighth",
        "Ninth",
        "Tenth",
        "Eleventh",
        "Twelfth",
        "Thirteenth",
        "Fourteenth",
        "Fifteenth",
        "Sixteenth",
        "Seventeenth",
        "Eighteenth",
        "Nineteenth",
        "Twentieth",
        "Twenty-First"
    };

    @Override
    public String getPlacementString(Result result) {
        Event event = result.event;
        if(result.place< event.getPlacementCutoff()){
            return NUMBER_ORDINALS[result.place] + " Place";
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
        if(place>=event.getPlacementCutoff()){
            return "black";
        }
        if(place==1){
            return "gold";
        } if (place==2){
            return "silver";
        } if (place == 3){
            return "bronze";
        } else {
            return "red";
        }
    }
}
