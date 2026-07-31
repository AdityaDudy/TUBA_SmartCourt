package in.tubalaw.courtos.modules.reports.renderer;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import in.tubalaw.courtos.modules.reports.dto.ReportTable;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class PdfReportRenderer {

    public byte[] render(ReportTable reportTable) throws DocumentException {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();

        // ── Brand Colors & Fonts ──────────────────────────────
        Color primaryColor = new Color(13, 102, 55); // #0D6637
        Color lightBgColor = new Color(245, 247, 246);
        Color headerTextColor = Color.WHITE;

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, primaryColor);
        Font subFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.GRAY);
        Font thFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, headerTextColor);
        Font tdFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);

        // ── Title & Meta ──────────────────────────────────────
        Paragraph title = new Paragraph("SMARTCOURT - " + (reportTable.getTitle() != null ? reportTable.getTitle().toUpperCase() : "REPORT"), titleFont);
        title.setSpacingAfter(4);
        document.add(title);

        String metaText = "Filters: " + (reportTable.getFilterSummary() != null ? reportTable.getFilterSummary() : "None")
                + "  |  Generated On: " + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        Paragraph meta = new Paragraph(metaText, subFont);
        meta.setSpacingAfter(16);
        document.add(meta);

        // ── Table ─────────────────────────────────────────────
        List<String> headers = reportTable.getHeaders();
        int numCols = headers.size();
        if (numCols > 0) {
            PdfPTable table = new PdfPTable(numCols);
            table.setWidthPercentage(100);

            // Table Header Row
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, thFont));
                cell.setBackgroundColor(primaryColor);
                cell.setPadding(8);
                cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                cell.setBorderColor(primaryColor);
                table.addCell(cell);
            }

            // Table Data Rows
            boolean alternate = false;
            for (List<Object> row : reportTable.getRows()) {
                for (Object item : row) {
                    PdfPCell cell = new PdfPCell(new Phrase(item != null ? item.toString() : "", tdFont));
                    cell.setPadding(6);
                    cell.setBackgroundColor(alternate ? lightBgColor : Color.WHITE);
                    cell.setBorderColor(new Color(230, 230, 230));
                    table.addCell(cell);
                }
                alternate = !alternate;
            }

            document.add(table);
        }

        // ── Footer ────────────────────────────────────────────
        Paragraph footer = new Paragraph("\n\nDigitally Certified by SmartCourt Legal Management Engine.", subFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();
        return out.toByteArray();
    }
}
