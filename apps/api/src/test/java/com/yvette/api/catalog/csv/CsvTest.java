package com.yvette.api.catalog.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class CsvTest {

    @Test
    void readsQuotesCommasAndLineBreaksInsideCells() {
        List<Csv.Row> rows = Csv.parse("﻿a,b\r\n\"x, y\",\"say \"\"hi\"\"\nthere\"\r\n\r\n3,4");
        assertEquals(3, rows.size());
        assertEquals(List.of("x, y", "say \"hi\"\nthere"), rows.get(1).cells());
        assertEquals(5, rows.get(2).line()); // the quoted line break and the skipped blank line both count
    }

    @Test
    void writesSafelyAndReadsBack() {
        String line = Csv.line(List.of("=HYPERLINK(\"x\")", "-5", "a,b", "plain"));
        assertEquals("\"'=HYPERLINK(\"\"x\"\")\",-5,\"a,b\",plain\r\n", line);
        assertEquals("=HYPERLINK(\"x\")", Csv.unguard(Csv.parse(line).getFirst().cells().getFirst()));
    }

    @Test
    void reportsAnUnclosedQuote() {
        assertThrows(IllegalArgumentException.class, () -> Csv.parse("a\n\"open"));
    }
}
