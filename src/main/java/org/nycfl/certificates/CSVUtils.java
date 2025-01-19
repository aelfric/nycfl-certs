package org.nycfl.certificates;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class CSVUtils {
  private CSVUtils(){

  }

  public static final CSVFormat CSV_FORMAT = CSVFormat.Builder
      .create(CSVFormat.DEFAULT)
      .setHeader()
      .setSkipHeaderRecord(true)
      .setAllowMissingColumnNames(true)
      .get();

  public static CSVParser parse(InputStream inputStream) throws IOException {
    return CSVParser.parse(inputStream, StandardCharsets.UTF_8, CSV_FORMAT);
  }
}
