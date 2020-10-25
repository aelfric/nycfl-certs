package org.nycfl.certificates;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public enum EventType implements LabeledEnum {
    SPEECH("Speech",
        SpeechResultParser::new,
        SpeechResultFormatter::new,
        School::getName,
        false),
    CONGRESS("Congress",
        CongressResultsParser::new,
        SpeechResultFormatter::new,
        School::getName,
        false),
    DEBATE_PF("Public Forum Debate",
        DebatePFResultParser::new,
        DebateResultFormatter::new,
        School::getDebateCode, true),
    DEBATE_LD("Lincoln-Douglas Debate",
        DebateLDResultParser::new,
        DebateResultFormatter::new,
        School::getDebateCode, true),
    DEBATE_CX("Policy Debate",
        DebatePFResultParser::new,
        DebateResultFormatter::new,
        School::getDebateCode, true),
    DEBATE_SPEAKS("Debate Speaker Awards",
        DebateSpeakerResultParser::new,
        DebateSpeakerResultFormatter::new,
        School::getName,
        false);

    private final String label;
    private final Supplier<ResultParser> parserSupplier;
    private final Supplier<ResultFormatter> formatterSupplier;
    private final Function<School, String>
        schoolMapperKeyFn;
    private final boolean hasSpeakerAwards;

    @SuppressWarnings("CdiInjectionPointsInspection")
    EventType(String label,
              Supplier<ResultParser> parserSupplier,
              Supplier<ResultFormatter> formatterSupplier,
              Function<School, String> schoolMapperKeyFn,
              boolean hasSpeakerAwards) {
        this.label = label;
        this.parserSupplier = parserSupplier;
        this.formatterSupplier = formatterSupplier;
        this.schoolMapperKeyFn = schoolMapperKeyFn;
        this.hasSpeakerAwards = hasSpeakerAwards;
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

    public Function<School, String> schoolMapper() {
        return schoolMapperKeyFn;
    }

    public boolean hasSpeakerAwards() {
        return this.hasSpeakerAwards;
    }
}
