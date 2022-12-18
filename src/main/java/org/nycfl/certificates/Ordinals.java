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
        "Twenty-First",
        "Twenty-Second",
        "Twenty-Third",
        "Twenty-Fourth",
        "Twenty-Fifth",
        "Twenty-Sixth",
        "Twenty-Seventh",
        "Twenty-Eighth",
        "Twenty-Ninth",
        "Thirtieth",
    };

    public static String ofInt(int i){
        return NUMBER_ORDINALS[i];
    }
}
