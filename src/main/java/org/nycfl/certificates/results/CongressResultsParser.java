package org.nycfl.certificates.results;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.nycfl.certificates.EliminationRound;
import org.nycfl.certificates.School;

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
        for (CSVRecord csvRecord : getRecords(parse)) {
            Result result = new Result();
            result.name = csvRecord.get("Code");
            result.code = csvRecord.get("Code");
            result.place =
                    Integer.parseInt(getOrAlternateColumn(csvRecord, "Ranking",
                        "Place")
                        .replace("T-", ""));
            result.eliminationRound = eliminationRound;
            result.school = schoolsMap.computeIfAbsent(
                    csvRecord.get("School"),
                    School::fromName);
            result.count = 1;
            results.add(result);
        }
        return results;
    }
}
