package org.nycfl.certificates;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import javax.ws.rs.BadRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public interface ResultParser {
    List<Result> parseResultsCSV(Map<String, School> schoolsMap,
                                 EliminationRound eliminationRound,
                                 InputStream inputStream);

    default List<CSVRecord> getRecords(CSVParser parse) {
        try {
            return parse.getRecords();
        } catch (IOException ioException){
            throw new BadRequestException("Could not get CSV Records");
        }
    }

    default CSVParser getParser(InputStream inputStream) {
        try {
            return CSVParser.parse(inputStream,
                StandardCharsets.UTF_8,
                CSVFormat.DEFAULT.withFirstRecordAsHeader()
                    .withAllowMissingColumnNames(true));
        } catch (IOException ioException){
            throw new BadRequestException("Cannot parse CSV");
        }
    }

    default String getOrAlternateColumn(CSVRecord record,
                                        String primaryName,
                                        String secondaryName) {
        try {
            return record.get(primaryName);
        } catch (IllegalArgumentException e){
            return record.get(secondaryName);
        }
    }
}
