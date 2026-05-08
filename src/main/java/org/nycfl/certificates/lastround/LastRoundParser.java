package org.nycfl.certificates.lastround;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class LastRoundParser {
    static void main() throws IOException {
        CsvMapper mapper = new CsvMapper();
        MappingIterator<StagedResult> it = mapper
            .readerFor(StagedResult.class)
            .with(CsvSchema.emptySchema().withHeader())
            .readValues(Files.newBufferedReader(Paths.get("/home/fricc/Downloads/LastRoundActive-NYSFLStateChampionship (5).csv")));
        while (it.hasNextValue()) {
            StagedResult value = it.nextValue();
            System.out.println(value);
        }
    }
}
