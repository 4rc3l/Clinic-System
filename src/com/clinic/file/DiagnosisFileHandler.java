package com.clinic.file;

import com.clinic.model.Patient;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiagnosisFileHandler {
    public static final String FILE_NAME = "diagnoses.csv";
    private static final String HEADER = "patientId,diagnosis,notes";

    public static final class Entry {
        public final String diagnosis;
        public final String notes;
        Entry(String diagnosis, String notes) {
            this.diagnosis = diagnosis;
            this.notes = notes;
        }
    }

    private final Path file;

    public DiagnosisFileHandler() {
        this(CsvUtil.defaultDataDir().resolve(FILE_NAME));
    }

    public DiagnosisFileHandler(Path file) {
        this.file = file;
    }

    public Path getFile() { return file; }

    public void ensureFileExists() throws IOException {
        CsvUtil.createIfMissing(file, HEADER);
    }

    public void save(Collection<Patient> patients) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (Patient p : patients) {
            boolean hasDiagnosis = !p.getDiagnosis().trim().isEmpty();
            boolean hasNotes = !p.getNotes().trim().isEmpty();
            if (!hasDiagnosis && !hasNotes) continue;
            lines.add(CsvUtil.toLine(p.getPatientId(), p.getDiagnosis(), p.getNotes()));
        }
        CsvUtil.writeAtomically(file, lines);
    }

    public Map<String, Entry> load() throws IOException {
        Map<String, Entry> result = new HashMap<>();
        ensureFileExists();
        String text = CsvUtil.readOrNull(file);
        if (text == null) return result;

        boolean first = true;
        for (List<String> row : CsvUtil.parse(text)) {
            if (first) { first = false; if (row.get(0).equals("patientId")) continue; }
            if (row.size() < 3 || row.get(0).trim().isEmpty()) continue;
            result.put(row.get(0).trim().toUpperCase(), new Entry(row.get(1), row.get(2)));
        }
        return result;
    }

    public void loadInto(Collection<Patient> patients) throws IOException {
        Map<String, Entry> saved = load();
        for (Patient p : patients) {
            Entry e = saved.get(p.getPatientId().toUpperCase());
            if (e != null) {
                p.setDiagnosis(e.diagnosis);
                p.setNotes(e.notes);
            }
        }
    }
}