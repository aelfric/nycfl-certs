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
        List<String> headerNames = parse.getHeaderNames();
        for (CSVRecord record : getRecords(parse)) {
            Result result = new Result();
            result.code = record.get("Code");
            result.eliminationRound = eliminationRound;
            result.school = schoolsMap.computeIfAbsent(
                    result.code.substring(0, result.code.length() - 3),
                    School::fromCode);
            try {
                result.numWins = Integer.valueOf(getOrAlternateColumn(
                    record,
                    "WinPm",
                    "WinPr",
                    "Win"));
            } catch (IllegalArgumentException e){
                result.numWins = 0;
                e.printStackTrace();
            };
            if (headerNames.contains("Name 2")) {
                result.name =
                    record.get("Name 1") + " & " + record.get("Name 2");
            } else {
                result.name = getOrAlternateColumn(
                    record,
                    "Name 1",
                    "Name");
            }
            result.count = 2;
            result.place = Integer.parseInt(getOrAlternateColumn(
                record,
                "Place",
                "Ranking"));
            results.add(result);
        }
        return results;
    }
}
