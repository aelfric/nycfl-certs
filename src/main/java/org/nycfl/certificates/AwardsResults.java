package org.nycfl.certificates;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.CREATE;

public class AwardsResults {

    final List<AwardsResult> results;
    private String schoolName;

    public AwardsResults(Collection<AwardsResult> c, String schoolName) {
        this.schoolName = schoolName;
        results = c
            .stream()
            .flatMap(
                awardsResult -> {
                    if(awardsResult.studentName.contains("&")){
                        return awardsResult.split().stream();
                    } else {
                        return Stream.of(awardsResult);
                    }
                }
        ).toList();
    }

    public File toSpreadsheet(){
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Awards");
        int rowNum = 0;
        Row row = sheet.createRow(rowNum++);

        writeHeaderRow(workbook, row, List.of(
            "ID",
            "Student Name",
            "School Name",
            "Award",
            "Category",
            "Address",
            "City",
            "State",
            "ZIP"
        ));
        int colNum = 0;
        for (AwardsResult datatype : results) {
            row = sheet.createRow(rowNum++);
            colNum = 0;
            Cell cell = row.createCell(colNum++);
            cell.setCellValue(datatype.id);
            cell = row.createCell(colNum++);
            cell.setCellValue(datatype.studentName);
            cell = row.createCell(colNum++);
            cell.setCellValue(datatype.schoolName);
            cell = row.createCell(colNum++);
            cell.setCellValue(datatype.award);
            cell = row.createCell(colNum);
            cell.setCellValue(datatype.eventName);
        }
        for(int i = 0; i <= colNum; i++){
            sheet.autoSizeColumn(i);
        }

        try {
            Path tempSpreadsheet = Files.createTempFile("Awards for " +
                schoolName.replace("/"," ") + " ",
                ".xlsx");
            OutputStream outputStream =
                Files.newOutputStream(tempSpreadsheet, CREATE);

            workbook.write(outputStream);
            workbook.close();
            return tempSpreadsheet.toFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void writeHeaderRow(XSSFWorkbook workbook,
                                Row row, List<String> headers) {
        int colNum = 0;
        CellStyle style = workbook.createCellStyle();
        // Setting Background color
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        for (String header : headers) {
            Cell cell = row.createCell(colNum++);
            cell.setCellValue(header);
            cell.setCellStyle(style);
        }
    }
}
