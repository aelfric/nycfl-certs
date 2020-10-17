package org.nycfl.certificates;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DebateSpeakerResultParser implements ResultParser {
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
            School school = schoolsMap.computeIfAbsent(
                record.get("School"),
                School::fromName);
            result.school = school;
            if(school.getDebateCode()==null){
                school.setDebateCode(getSchoolFromCode(result.code));
            }
            result.name = record.get("Name");
            result.count = 1;
            result.place = Integer.parseInt(record.get("Order"));
            results.add(result);
        }
        return results;
    }

    private String getSchoolFromCode(String code) {
        return code.substring(0, code.length() - 3);
    }
}
