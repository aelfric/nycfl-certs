package org.nycfl.certificates;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public enum EventType implements LabeledEnum {
    SPEECH(SpeechResultParser::new, "Speech"),
    DEBATE_PF(DebatePFResultParser::new, "Public Forum Debate"),
    DEBATE_LD(DebateLDResultParser::new, "Lincoln-Douglas Debate"),
    DEBATE_CX(DebatePFResultParser::new, "Policy Debate");

    private final String label;
    private final Supplier<ResultParser> parserSupplier;

    EventType(Supplier<ResultParser> parserSupplier, String debate) {
        this.parserSupplier = parserSupplier;
        label = debate;
    }

    public List<Result> parseResults(Map<String, School> schoolsMap,
                                     EliminationRound eliminationRound,
                                     InputStream inputStream) {
        ResultParser resultParser = parserSupplier.get();
        return resultParser.parseResultsCSV(schoolsMap,
                eliminationRound,
                inputStream);
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
