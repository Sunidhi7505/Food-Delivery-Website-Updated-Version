package com.foody.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvExportServiceTest {

    @Test
    void cellsAreQuotedAndFormulaInjectionIsNeutralised() {
        assertEquals("\"Pizza, large\"", CsvExportService.cell("Pizza, large"));
        assertEquals("\"say \"\"hi\"\"\"", CsvExportService.cell("say \"hi\""));
        assertEquals("\"'=HYPERLINK(\"\"x\"\")\"", CsvExportService.cell("=HYPERLINK(\"x\")"));
        assertEquals("\"-5.0\"", CsvExportService.cell(-5.0));   // real numbers are left alone
        assertEquals("\"\"", CsvExportService.cell(null));
    }
}
