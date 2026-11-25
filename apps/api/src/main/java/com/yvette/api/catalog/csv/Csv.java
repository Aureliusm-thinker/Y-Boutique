package com.yvette.api.catalog.csv;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal RFC 4180 CSV: comma-separated, fields with commas, quotes or line breaks wrapped in double quotes,
 * quotes doubled inside. Reads files saved by Excel or Google Sheets (optional UTF-8 BOM, CRLF or LF).
 */
public final class Csv {

    private Csv() {}

    /** A parsed row and the line number it started on (1-based, as a spreadsheet shows it). */
    public record Row(int line, List<String> cells) {}

    public static List<Row> parse(String text) {
        List<Row> rows = new ArrayList<>();
        if (text.startsWith("﻿")) text = text.substring(1);
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        int line = 1;
        int rowStart = 1;
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    if (c == '\n') line++;
                    cell.append(c);
                }
            } else if (c == '"' && cell.isEmpty()) {
                quoted = true;
            } else if (c == ',') {
                cells.add(cell.toString());
                cell.setLength(0);
            } else if (c == '\r' || c == '\n') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                cells.add(cell.toString());
                cell.setLength(0);
                addIfNotBlank(rows, rowStart, cells);
                cells = new ArrayList<>();
                line++;
                rowStart = line;
            } else {
                cell.append(c);
            }
            i++;
        }
        if (quoted) throw new IllegalArgumentException("A quoted value starting on line " + rowStart + " is never closed");
        if (!cell.isEmpty() || !cells.isEmpty()) {
            cells.add(cell.toString());
            addIfNotBlank(rows, rowStart, cells);
        }
        return rows;
    }

    private static void addIfNotBlank(List<Row> rows, int line, List<String> cells) {
        if (cells.stream().anyMatch(s -> !s.isBlank())) rows.add(new Row(line, cells));
    }

    public static String line(List<String> cells) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) out.append(',');
            out.append(escape(cells.get(i)));
        }
        return out.append("\r\n").toString();
    }

    static String escape(String value) {
        if (value == null) return "";
        // Stop spreadsheet apps treating shop text as a formula (CSV injection).
        if (!value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0 && !looksLikeNumber(value)) {
            value = "'" + value;
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }

    /** Undoes {@link #escape}'s formula guard when a file comes back in. */
    static String unguard(String value) {
        if (value.length() > 1 && value.charAt(0) == '\'' && "=+-@\t\r".indexOf(value.charAt(1)) >= 0) {
            return value.substring(1);
        }
        return value;
    }

    private static boolean looksLikeNumber(String value) {
        return value.matches("-?\\d+(\\.\\d+)?");
    }
}
