package org.nycfl.certificates;

public enum EliminationRound implements LabeledEnum{
    FINALIST("Finalist"),
    SEMIFINALIST("Semi-Finalist"),
    QUARTER_FINALIST("Quarter Finalist"),
    OCTOFINALIST("Octofinalist"),
    DOUBLE_OCTOFINALIST("Double-OctoFinalist");

    public final String label;

    EliminationRound(String label) {
        this.label = label;
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
