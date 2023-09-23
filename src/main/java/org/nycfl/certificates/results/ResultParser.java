package org.nycfl.certificates.results;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.nycfl.certificates.CSVUtils;
import org.nycfl.certificates.EliminationRound;
import org.nycfl.certificates.School;

import jakarta.ws.rs.BadRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public interface ResultParser {
    List<Result> parseResultsCSV(Map<String, School> schoolsMap,
                                 EliminationRound eliminationRound,
                                 InputStream inputStream);

    default List<CSVRecord> getRecords(CSVParser parse) {
        try {
            return parse.getRecords();
        } catch (IOException ioException) {
            throw new BadRequestException("Could not get CSV Records");
        }
    }

    default CSVParser getParser(InputStream inputStream) {
        try {
            return CSVUtils.parse(inputStream);
        } catch (IOException ioException) {
            throw new BadRequestException("Cannot parse CSV");
        }
    }

    /**
     * @param csvRecord a CSV Record
     * @param names a varargs list of column names that may or may not exist in this record
     * @return the value of the first valid colum in the list of column names provided
     */
    default String  getOrAlternateColumn(CSVRecord csvRecord,
                                        String... names) {
        for (String name : names) {
            if(csvRecord.isMapped(name)){
                return csvRecord.get(name);
            }
        }
        throw new IllegalArgumentException("Could not find any of [" +
            Arrays.toString(names) + "]");
    }

    default String getOrDefault(CSVRecord csvRecord,
                                String name,
                                String defaultVal) {
        if(csvRecord.isMapped(name)){
            return csvRecord.get(name);
        } else {
            return defaultVal;
        }
    }
}
