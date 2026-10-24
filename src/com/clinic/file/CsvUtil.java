package com.clinic.file;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

final class CsvUtil {
    private CsvUtil() {}

    static Path defaultDataDir() {
        Path found = locate(Paths.get("").toAbsolutePath());
        if (found == null) {
            try {
                Path classes = Paths.get(CsvUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                found = locate(classes.toAbsolutePath());
            } catch (Exception ignored) {
            }
        }
        return found != null ? found : Paths.get("data").toAbsolutePath();
    }

    private static Path locate(Path start) {
        for (Path p = start; p != null; p = p.getParent()) {
            Path src = p.resolve("src");
            if (Files.isDirectory(src)) {
                try (Stream<Path> files = Files.walk(src, 8)) {
                    Optional<Path> hit = files
                            .filter(f -> f.getFileName().toString().equals("CsvUtil.java"))
                            .findFirst();
                    if (hit.isPresent()) return hit.get().getParent();
                } catch (IOException ignored) {
                }
            }
        }
        return null;
    }

    static String toLine(String... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(escape(fields[i]));
        }
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        boolean needsQuotes = s.indexOf(',') >= 0 || s.indexOf('"') >= 0
                || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0
                || !s.equals(s.trim());
        return needsQuotes ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }

    static List<List<String>> parse(String text) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean fieldStarted = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') { field.append('"'); i++; }
                    else inQuotes = false;
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
                fieldStarted = true;
            } else if (c == ',') {
                row.add(field.toString());
                field.setLength(0);
                fieldStarted = true;
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                if (fieldStarted || field.length() > 0 || !row.isEmpty()) {
                    row.add(field.toString());
                    rows.add(row);
                }
                row = new ArrayList<>();
                field.setLength(0);
                fieldStarted = false;
            } else {
                field.append(c);
                fieldStarted = true;
            }
        }
        if (fieldStarted || field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString());
            rows.add(row);
        }
        return rows;
    }

    static void createIfMissing(Path file, String header) throws IOException {
        if (!Files.exists(file)) {
            writeAtomically(file, java.util.Collections.singletonList(header));
        }
    }

    static String readOrNull(Path file) throws IOException {
        if (!Files.exists(file)) return null;
        String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        return text.startsWith("\uFEFF") ? text.substring(1) : text;
    }

    static void writeAtomically(Path file, List<String> lines) throws IOException {
        Path dir = file.toAbsolutePath().getParent();
        Files.createDirectories(dir);
        Path tmp = Files.createTempFile(dir, file.getFileName().toString(), ".tmp");
        try {
            Files.write(tmp, lines, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                try {
                    Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e2) {
                    Files.write(file, lines, StandardCharsets.UTF_8);
                }
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}