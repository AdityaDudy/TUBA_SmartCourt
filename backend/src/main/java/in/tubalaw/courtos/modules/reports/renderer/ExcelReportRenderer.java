package in.tubalaw.courtos.modules.reports.renderer;

import in.tubalaw.courtos.modules.reports.dto.ReportTable;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ExcelReportRenderer {

    public byte[] render(ReportTable reportTable) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Report Data");

            // ── Styles ──────────────────────────────────────────
            // Title style
            Font titleFont = workbook.createFont();
            titleFont.setFontName("Calibri");
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setBold(true);
            titleFont.setColor(IndexedColors.DARK_GREEN.getIndex());

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);

            // Subtitle style
            Font subFont = workbook.createFont();
            subFont.setFontName("Calibri");
            subFont.setFontHeightInPoints((short) 10);
            subFont.setItalic(true);
            subFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());

            CellStyle subStyle = workbook.createCellStyle();
            subStyle.setFont(subFont);

            // Header Style
            Font headerFont = workbook.createFont();
            headerFont.setFontName("Calibri");
            headerFont.setFontHeightInPoints((short) 11);
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.LEFT);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.MEDIUM);

            // Data Cell Styles
            Font dataFont = workbook.createFont();
            dataFont.setFontName("Calibri");
            dataFont.setFontHeightInPoints((short) 11);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setFont(dataFont);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());

            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.setFont(dataFont);
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("₹#,##0.00"));
            currencyStyle.setBorderBottom(BorderStyle.THIN);
            currencyStyle.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());

            int rowIndex = 0;

            // Row 0: Title
            Row titleRow = sheet.createRow(rowIndex++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("SMARTCOURT - " + (reportTable.getTitle() != null ? reportTable.getTitle().toUpperCase() : "REPORT"));
            titleCell.setCellStyle(titleStyle);

            // Row 1: Filters & Date
            Row subRow = sheet.createRow(rowIndex++);
            Cell subCell = subRow.createCell(0);
            String filterText = "Filters: " + (reportTable.getFilterSummary() != null ? reportTable.getFilterSummary() : "None") 
                    + " | Generated: " + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            subCell.setCellValue(filterText);
            subCell.setCellStyle(subStyle);

            rowIndex++; // Empty spacing row

            // Row 3: Header Row
            Row headerRow = sheet.createRow(rowIndex++);
            headerRow.setHeightInPoints(24);
            List<String> headers = reportTable.getHeaders();
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            // Data Rows
            for (List<Object> rowData : reportTable.getRows()) {
                Row row = sheet.createRow(rowIndex++);
                for (int colIndex = 0; colIndex < rowData.size(); colIndex++) {
                    Cell cell = row.createCell(colIndex);
                    Object val = rowData.get(colIndex);
                    if (val instanceof Number num) {
                        cell.setCellValue(num.doubleValue());
                        if (headers.get(colIndex).toLowerCase().contains("revenue") || headers.get(colIndex).toLowerCase().contains("amount") || headers.get(colIndex).toLowerCase().contains("paid") || headers.get(colIndex).toLowerCase().contains("balance")) {
                            cell.setCellStyle(currencyStyle);
                        } else {
                            cell.setCellStyle(dataStyle);
                        }
                    } else if (val != null) {
                        cell.setCellValue(val.toString());
                        cell.setCellStyle(dataStyle);
                    } else {
                        cell.setCellValue("");
                        cell.setCellStyle(dataStyle);
                    }
                }
            }

            // Freeze header pane
            sheet.createFreezePane(0, 4);

            // Auto-size columns
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i) + 1000, 3500));
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
