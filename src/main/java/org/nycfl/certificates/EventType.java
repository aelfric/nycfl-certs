package org.nycfl.certificates;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public enum EventType implements LabeledEnum {
    SPEECH("Speech", SpeechResultParser::new, SpeechResultFormatter::new),
    DEBATE_PF("Public Forum Debate", DebatePFResultParser::new,
        DebateResultFormatter::new),
    DEBATE_LD("Lincoln-Douglas Debate", DebateLDResultParser::new,
        DebateResultFormatter::new),
    DEBATE_CX("Policy Debate", DebatePFResultParser::new,
        DebateResultFormatter::new);

    private final String label;
    private final Supplier<ResultParser> parserSupplier;
    private final Supplier<ResultFormatter> formatterSupplier;

    @SuppressWarnings("CdiInjectionPointsInspection")
    EventType(String label, Supplier<ResultParser> parserSupplier,
              Supplier<ResultFormatter> formatterSupplier) {
        this.label = label;
        this.parserSupplier = parserSupplier;
        this.formatterSupplier = formatterSupplier;
    }

    public List<Result> parseResults(Map<String, School> schoolsMap,
                                     EliminationRound eliminationRound,
                                     InputStream inputStream) {
        ResultParser resultParser = parserSupplier.get();
        return resultParser.parseResultsCSV(schoolsMap,
            eliminationRound,
            inputStream);
    }

    public String formatPlacementString(Result result) {
        ResultFormatter resultFormatter = formatterSupplier.get();
        return resultFormatter.getPlacementString(result);
    }

    public String getCertificateColor(Result result) {
        ResultFormatter resultFormatter = formatterSupplier.get();
        return resultFormatter.getCertificateColor(result);
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
