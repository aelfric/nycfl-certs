package org.nycfl.certificates;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CongressPOListParser implements ResultParser {
    @Override
    public List<Result> parseResultsCSV(Map<String, School> schoolsMap,
                                        EliminationRound eliminationRound,
                                        InputStream inputStream) {
        CSVParser parse = getParser(inputStream);
        List<Result> results = new ArrayList<>();
        for (CSVRecord csvRecord : getRecords(parse)) {
            Result result = new Result();
            result.name = csvRecord.get("Entry");
            result.code = "po_" + csvRecord.get("Entry");
            result.place = 1;
            result.eliminationRound = fromString(csvRecord.get("Session"));
            result.school = schoolsMap.computeIfAbsent(
                    csvRecord.get("School"),
                    School::fromName);
            result.count = 1;
            results.add(result);
        }
        return results;
    }

    private EliminationRound fromString(String session){
        return switch (session) {
            case "Qtr" -> EliminationRound.QUARTER_FINALIST;
            case "Semi" -> EliminationRound.SEMIFINALIST;
            case "Final" -> EliminationRound.FINALIST;
            default -> EliminationRound.PRELIM;
        };

    }
}
