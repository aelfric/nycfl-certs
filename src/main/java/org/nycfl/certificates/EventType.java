package org.nycfl.certificates;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public enum EventType {
    SPEECH(SpeechResultParser::new),
    DEBATE_PF(DebatePFResultParser::new),
    DEBATE_LD(DebateLDResultParser::new),
    DEBATE_CX(SpeechResultParser::new);

    private final Supplier<ResultParser> parserSupplier;

    EventType(Supplier<ResultParser> parserSupplier) {
        this.parserSupplier = parserSupplier;
    }

    public List<Result> parseResults(Map<String, School> schoolsMap,
                                     EliminationRound eliminationRound,
                                     InputStream inputStream) {
        ResultParser resultParser = parserSupplier.get();
        return resultParser.parseResultsCSV(schoolsMap,
                eliminationRound,
                inputStream);
    }
}
