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
    private static final String[] SUFFIXES = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };

    public static String fallbackOrdinal(int i) {
        return switch (i % 100) {
            case 11, 12, 13 -> i + "th";
            default -> i + SUFFIXES[i % 10];
        };
    }

    public static String ofInt(int i){
        try {
            return NUMBER_ORDINALS[i];
        } catch (ArrayIndexOutOfBoundsException e){
            return fallbackOrdinal(i);
        }
    }
}
