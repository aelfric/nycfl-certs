package org.nycfl.certificates;

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
