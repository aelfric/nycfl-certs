package org.nycfl.certificates;

public class DebateResultFormatter implements ResultFormatter{

    @Override
    public String getPlacementString(Result result) {
        Event event = result.event;
        if(result.place == 1){
            return "Champion";
        } else if (result.place < event.getCertificateCutoff()){
            return result.eliminationRound.label;
        } else {
            return "";
        }

    }

    @Override
    public String getCertificateColor(Result result) {
        if(result.place ==1){
            return "gold";
        } if (result.place ==2){
            return "silver";
        } if (result.eliminationRound == EliminationRound.SEMIFINALIST){
            return "bronze";
        } else if (result.eliminationRound == EliminationRound.QUARTER_FINALIST) {
            return "red";
        } else {
            return "black";
        }
    }
}
