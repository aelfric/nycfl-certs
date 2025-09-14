package org.nycfl.certificates.results;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.nycfl.certificates.EliminationRound;
import org.nycfl.certificates.School;

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
        for (CSVRecord csvRecord : getRecords(parse)) {
            Result result = new Result();
            result.code = csvRecord.get("Code");
            result.eliminationRound = eliminationRound;
            result.school = schoolsMap.computeIfAbsent(
                    result.code.substring(0, result.code.length() - 3),
                    School::fromCode);
            try {
                result.numWins = Integer.valueOf(getOrAlternateColumn(
                    csvRecord,
                    "WinPm",
                    "WinPr",
                    "Win"));
            } catch (IllegalArgumentException _){
                result.numWins = 0;
            }
            if (headerNames.contains("Name 2")) {
                result.name =
                    csvRecord.get("Name 1") + " & " + csvRecord.get("Name 2");
            } else {
                result.name = getOrAlternateColumn(
                    csvRecord,
                    "Name 1",
                    "Name");
            }
            result.count = 2;
            result.place = Integer.parseInt(getOrAlternateColumn(
                csvRecord,
                "Place",
                "Ranking").replace("T-",""));
            results.add(result);
        }
        return results;
    }
}
