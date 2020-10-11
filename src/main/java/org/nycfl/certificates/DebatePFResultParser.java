package org.nycfl.certificates;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DebatePFResultParser implements ResultParser {
    @Override
    public List<Result> parseResultsCSV(Map<String, School> schoolsMap,
                                        EliminationRound eliminationRound,
                                        InputStream inputStream) {
        CSVParser parse = getParser(inputStream);
        List<Result> results = new ArrayList<>();
        for (CSVRecord record : getRecords(parse)) {
            Result result = new Result();
            result.code = record.get("Code");
            result.eliminationRound = eliminationRound;
            result.school = schoolsMap.computeIfAbsent(
                    result.code.substring(0, result.code.length() - 3),
                    School::fromName);

            if(eliminationRound != EliminationRound.FINALIST) {
                result.name = record.get("Name");
                result.count = 2;
                result.place = Integer.parseInt(record.get("Place"));
            } else {
                result.name =
                            record.get("Name 1") + " & " + record.get("Name 2");
                result.count = 2;
                result.place =
                        Integer.parseInt(record.get("Ranking").replace("T-", ""));
            }
            results.add(result);
        }
        return results;
    }
}
