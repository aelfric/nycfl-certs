package org.nycfl.certificates.results;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.nycfl.certificates.EliminationRound;
import org.nycfl.certificates.School;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DebateLDResultParser implements ResultParser {
    @Override
    public List<Result> parseResultsCSV(Map<String, School> schoolsMap,
                                        EliminationRound eliminationRound,
                                        InputStream inputStream) {
        CSVParser parse = getParser(inputStream);
        List<Result> results = new ArrayList<>();
        for (CSVRecord csvRecord : getRecords(parse)) {
            Result result = new Result();
            result.name = getOrAlternateColumn(csvRecord, "Name","Name 1");
            result.count = 1;
            result.code = csvRecord.get("Code");
            result.place = Integer.parseInt(getOrAlternateColumn(csvRecord,
                "Place", "Ranking").replace("T-"
                ,""));
            try {
                result.numWins = Integer.valueOf(getOrAlternateColumn(csvRecord,
                    "WinPm",
                    "WinPr", "Win"));
            } catch (IllegalArgumentException _){
                result.numWins = 0;
            }
            result.eliminationRound = eliminationRound;
            result.school = schoolsMap.computeIfAbsent(
                    result.code.substring(0, result.code.length()-3),
                    School::fromCode);
            results.add(result);
        }
        return results;
    }
}
