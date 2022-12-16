package org.nycfl.certificates;

import io.quarkus.qute.TemplateExtension;

public class DebateSpeakerResultFormatter implements ResultFormatter{
    static final String[] NUMBER_ORDINALS = {
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
