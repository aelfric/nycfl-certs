package org.nycfl.certificates;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SpeechResultParser implements ResultParser {
    @Override
    public List<Result> parseResultsCSV(Map<String, School> schoolsMap,
                                        EliminationRound eliminationRound,
                                        InputStream inputStream) {
        CSVParser parse = getParser(inputStream);
        List<Result> results = new ArrayList<>();
        List<String> headerNames = parse.getHeaderNames();
        for (CSVRecord record : getRecords(parse)) {
            Result result = new Result();
            if (headerNames.contains("Name 2")) {
                result.name =
                        record.get("Name 1") + " & " + record.get("Name 2");
                result.count = 2;
            } else {
                result.name = getOrAlternateColumn(record, "Name 1", "Name");
                result.count = 1;
            }
            result.code = record.get("Code");
            result.place =
                    Integer.parseInt(getOrAlternateColumn(record, "Ranking",
                        "Place")
                        .replace("T-", ""));
            result.eliminationRound = eliminationRound;
            result.school = schoolsMap.computeIfAbsent(
                    record.get("School"),
                    School::fromName);
            results.add(result);
        }
        return results;
    }
}
