package org.nycfl.certificates;

public class Ordinals {
    private Ordinals(){

    }

    private static final String[] NUMBER_ORDINALS = {
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

    public static String ofInt(int i){
        return NUMBER_ORDINALS[i];
    }
}
