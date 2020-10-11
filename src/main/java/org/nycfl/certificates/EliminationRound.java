package org.nycfl.certificates;

public enum EliminationRound {
    FINALIST("Finalist"),
    SEMIFINALIST("Semi-Finalist"),
    QUARTER_FINALIST("Quarter Finalist"),
    OCTOFINALIST("Octofinalist"),
    DOUBLE_OCTOFINALIST("Double-OctoFinalist");

    public final String label;

    EliminationRound(String label) {
        this.label = label;
    }
}
