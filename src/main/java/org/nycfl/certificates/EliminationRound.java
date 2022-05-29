package org.nycfl.certificates;

public enum EliminationRound implements LabeledEnum{
    FINALIST("Finalist", "Finals"),
    SEMIFINALIST("Semi-Finalist", "Semi-Finals"),
    QUARTER_FINALIST("Quarter-Finalist", "Quarter-Finals"),
    OCTOFINALIST("Octo-Finalist", "Octo-Finals"),
    PLAY_IN("Play-In", "Play-In"),
    DOUBLE_OCTOFINALIST("Double Octo-Finalist", "Double Octo-Finals"),
    TRIPLE_OCTOFINALIST("Triple Octo-Finalist", "Triple Octo-Finals"),
    PRELIM("Prelim", "Prelims");

    public final String label;
    public final String roundName;

    EliminationRound(String label, String finals) {
        this.label = label;
        roundName = finals;
    }

    @Override
    public String getLabel() {
        return this.label;
    }

    @Override
    public String getValue() {
        return name();
    }
}
