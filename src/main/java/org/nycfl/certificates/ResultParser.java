package org.nycfl.certificates;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import javax.ws.rs.BadRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
            return CSVParser.parse(inputStream,
                StandardCharsets.UTF_8,
                CSVFormat.DEFAULT.withFirstRecordAsHeader()
                    .withAllowMissingColumnNames(true));
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
            try {
                return csvRecord.get(name);
            } catch (IllegalArgumentException ignored) {
            }
        }
        throw new IllegalArgumentException("Could not find any of [" +
            Arrays.toString(names) + "]");
    }

    default String getOrDefault(CSVRecord csvRecord,
                                String name,
                                String defaultVal) {
        try {
            return csvRecord.get(name);
        } catch (IllegalArgumentException e) {
            return defaultVal;
        }
    }
}
