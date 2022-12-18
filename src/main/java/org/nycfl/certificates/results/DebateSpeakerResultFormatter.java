package org.nycfl.certificates.results;

import io.quarkus.qute.TemplateExtension;
import org.nycfl.certificates.Event;
import org.nycfl.certificates.Ordinals;

public class DebateSpeakerResultFormatter implements ResultFormatter{

    @Override
    public String getPlacementString(Result result) {
        Event event = result.event;
        if(result.place< event.getPlacementCutoff()){
            return Ordinals.ofInt(result.place) + " Place";
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
        } else if (place==2){
            return "silver";
        } else if (place == 3){
            return "bronze";
        } else {
            return "red";
        }
    }

    @TemplateExtension(namespace = "Speaker", matchName = "cleanName")
    static String cleanName(String val) {
        return val.replace("Speaker Awards","").trim();
    }
}
