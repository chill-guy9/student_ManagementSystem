package com.sms.nexus.common.util;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
public class CsvExporter {

    private static final byte[] BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    public static void export(HttpServletResponse response, String fileName,
                               String[] headers, List<String[]> data) throws IOException {
        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        // Write BOM for Excel compatibility
        response.getOutputStream().write(BOM);

        try (Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            // Write headers
            writer.write(String.join(",", headers));
            writer.write("\n");

            // Write data rows
            for (String[] row : data) {
                for (int i = 0; i < row.length; i++) {
                    if (i > 0) writer.write(",");
                    writer.write(escapeCsvField(row[i] != null ? row[i] : ""));
                }
                writer.write("\n");
            }
            writer.flush();
        }
    }

    private static String escapeCsvField(String field) {
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}
