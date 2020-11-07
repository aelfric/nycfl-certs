package org.nycfl.certificates;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CongressResultsParser implements ResultParser {
    @Override
    public List<Result> parseResultsCSV(Map<String, School> schoolsMap,
                                        EliminationRound eliminationRound,
                                        InputStream inputStream) {
        CSVParser parse = getParser(inputStream);
        List<Result> results = new ArrayList<>();
        for (CSVRecord record : getRecords(parse)) {
            Result result = new Result();
            result.name = record.get("Code");
            result.code = record.get("Code");
            result.place =
                    Integer.parseInt(getOrAlternateColumn(record, "Ranking",
                        "Place")
                        .replace("T-", ""));
            result.eliminationRound = eliminationRound;
            result.school = schoolsMap.computeIfAbsent(
                    record.get("School"),
                    School::fromName);
            result.count = 1;
            results.add(result);
        }
        return results;
    }
}
