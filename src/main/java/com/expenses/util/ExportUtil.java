package com.expenses.util;

import com.expenses.model.Expense;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExportUtil {
    private static final Logger logger = LoggerFactory.getLogger(ExportUtil.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final com.itextpdf.text.Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final com.itextpdf.text.Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
    private static final com.itextpdf.text.Font NORMAL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 12);

    public static void exportExpenses(
        List<Expense> expenses,
        File file,
        boolean isPDF,
        boolean includeCharts,
        boolean includeSummary,
        boolean includeDetails
    ) throws Exception {
        if (isPDF) {
            exportToPDF(file, expenses, includeCharts, includeSummary, includeDetails);
        } else {
            exportToExcel(file, expenses, includeCharts, includeSummary, includeDetails);
        }
    }

    private static void exportToPDF(
        File file,
        List<Expense> expenses,
        boolean includeCharts,
        boolean includeSummary,
        boolean includeDetails
    ) throws Exception {
        Document document = new Document(PageSize.A4);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        // Título
        Paragraph title = new Paragraph("Relatório de Despesas", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // Período
        if (!expenses.isEmpty()) {
            LocalDate startDate = expenses.get(0).getDate();
            LocalDate endDate = expenses.get(expenses.size() - 1).getDate();
            Paragraph period = new Paragraph(
                "Período: " + startDate.format(DATE_FORMATTER) + " a " + endDate.format(DATE_FORMATTER),
                SUBTITLE_FONT
            );
            period.setAlignment(Element.ALIGN_CENTER);
            period.setSpacingAfter(30);
            document.add(period);
        }

        // Resumo
        if (includeSummary) {
            addSummaryToPDF(document, expenses);
        }

        // Gráficos
        if (includeCharts) {
            addChartsToPDF(document, expenses);
        }

        // Detalhes
        if (includeDetails) {
            addDetailsToPDF(document, expenses);
        }

        document.close();
    }

    private static void addSummaryToPDF(Document document, List<Expense> expenses) throws Exception {
        Paragraph summaryTitle = new Paragraph("Resumo", SUBTITLE_FONT);
        summaryTitle.setSpacingBefore(20);
        summaryTitle.setSpacingAfter(10);
        document.add(summaryTitle);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(20);

        // Total de despesas
        double total = expenses.stream()
            .map(Expense::getAmount)
            .mapToDouble(BigDecimal::doubleValue)
            .sum();

        addTableRow(table, "Total de despesas:", String.format("R$ %.2f", total));
        addTableRow(table, "Quantidade de registros:", String.valueOf(expenses.size()));

        document.add(table);
    }

    private static void addTableRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, NORMAL_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, NORMAL_FONT));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    private static void addChartsToPDF(Document document, List<Expense> expenses) throws Exception {
        // TODO: Implementar geração de gráficos usando JFreeChart
        Paragraph chartsTitle = new Paragraph("Gráficos", SUBTITLE_FONT);
        chartsTitle.setSpacingBefore(20);
        chartsTitle.setSpacingAfter(10);
        document.add(chartsTitle);

        // Placeholder para gráficos
        Paragraph placeholder = new Paragraph(
            "Esta seção será implementada em uma versão futura.",
            NORMAL_FONT
        );
        placeholder.setSpacingAfter(20);
        document.add(placeholder);
    }

    private static void addDetailsToPDF(Document document, List<Expense> expenses) throws Exception {
        Paragraph detailsTitle = new Paragraph("Detalhes", SUBTITLE_FONT);
        detailsTitle.setSpacingBefore(20);
        detailsTitle.setSpacingAfter(10);
        document.add(detailsTitle);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        float[] columnWidths = {2, 4, 2, 2};
        table.setWidths(columnWidths);

        // Cabeçalho
        String[] headers = {"Data", "Descrição", "Categoria", "Valor"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, SUBTITLE_FONT));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell.setPadding(5);
            table.addCell(cell);
        }

        // Dados
        for (Expense expense : expenses) {
            table.addCell(new Phrase(expense.getDate().format(DATE_FORMATTER), NORMAL_FONT));
            table.addCell(new Phrase(expense.getDescription(), NORMAL_FONT));
            table.addCell(new Phrase(expense.getCategoryId().toString(), NORMAL_FONT)); // TODO: Converter para nome da categoria
            table.addCell(new Phrase(String.format("R$ %.2f", expense.getAmount()), NORMAL_FONT));
        }

        document.add(table);
    }

    private static void exportToExcel(
        File file,
        List<Expense> expenses,
        boolean includeCharts,
        boolean includeSummary,
        boolean includeDetails
    ) throws Exception {
        Workbook workbook = new XSSFWorkbook();

        if (includeSummary) {
            addSummaryToExcel(workbook, expenses);
        }

        if (includeDetails) {
            addDetailsToExcel(workbook, expenses);
        }

        if (includeCharts) {
            addChartsToExcel(workbook, expenses);
        }

        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            workbook.write(outputStream);
        }
    }

    private static void addSummaryToExcel(Workbook workbook, List<Expense> expenses) {
        Sheet sheet = workbook.createSheet("Resumo");
        sheet.setColumnWidth(0, 6000);
        sheet.setColumnWidth(1, 4000);

        CellStyle headerStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Resumo de Despesas");
        titleCell.setCellStyle(headerStyle);

        // Total de despesas
        double total = expenses.stream()
            .map(Expense::getAmount)
            .mapToDouble(BigDecimal::doubleValue)
            .sum();

        Row totalRow = sheet.createRow(2);
        totalRow.createCell(0).setCellValue("Total de despesas:");
        totalRow.createCell(1).setCellValue(String.format("R$ %.2f", total));

        Row countRow = sheet.createRow(3);
        countRow.createCell(0).setCellValue("Quantidade de registros:");
        countRow.createCell(1).setCellValue(expenses.size());
    }

    private static void addDetailsToExcel(Workbook workbook, List<Expense> expenses) {
        Sheet sheet = workbook.createSheet("Detalhes");
        sheet.setColumnWidth(0, 3000);
        sheet.setColumnWidth(1, 8000);
        sheet.setColumnWidth(2, 4000);
        sheet.setColumnWidth(3, 3000);

        CellStyle headerStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Cabeçalho
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Data", "Descrição", "Categoria", "Valor"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Dados
        int rowNum = 1;
        for (Expense expense : expenses) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(expense.getDate().format(DATE_FORMATTER));
            row.createCell(1).setCellValue(expense.getDescription());
            row.createCell(2).setCellValue(expense.getCategoryId().toString()); // TODO: Converter para nome da categoria
            row.createCell(3).setCellValue(String.format("R$ %.2f", expense.getAmount()));
        }
    }

    private static void addChartsToExcel(Workbook workbook, List<Expense> expenses) {
        // TODO: Implementar geração de gráficos no Excel
        Sheet sheet = workbook.createSheet("Gráficos");
        Row row = sheet.createRow(0);
        row.createCell(0).setCellValue("Esta seção será implementada em uma versão futura.");
    }
}
