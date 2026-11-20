package com.clinic.file;

import com.clinic.model.Doctor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Reads and writes doctors.txt. Same style as the patient / appointment handlers.
 * The doctorId column holds the doctor's license number.
 */
public class DoctorFileHandler {
    public static final String FILE_NAME = "doctors.txt";
    private static final String HEADER = "doctorId|name|age|contactNumber|specialization|available";

    private final Path file;

    public DoctorFileHandler() {
        this(TxtUtil.defaultDataDir().resolve(FILE_NAME));
    }

    public DoctorFileHandler(Path file) {
        this.file = file;
    }

    public Path getFile() { return file; }

    public void ensureFileExists() throws IOException {
        TxtUtil.createIfMissing(file, HEADER);   // header only: a new clinic starts with no doctors
    }

    public void save(Collection<Doctor> doctors) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (Doctor d : doctors) {
            lines.add(TxtUtil.toLine(
                    d.getDoctorId(),
                    d.getName(),
                    String.valueOf(d.getAge()),
                    d.getContactNumber(),
                    d.getSpecialization(),
                    String.valueOf(d.isAvailable())));
        }
        TxtUtil.writeAtomically(file, lines);
    }

    public List<Doctor> load() throws IOException {
        List<Doctor> result = new ArrayList<>();
        ensureFileExists();
        String text = TxtUtil.readOrNull(file);
        if (text == null) return result;

        boolean first = true;
        boolean oldFormat = false;   // files saved before shifts were removed have an extra "shift" column
        for (List<String> row : TxtUtil.parse(text)) {
            if (first) {
                first = false;
                if (row.get(0).equals("doctorId")) {
                    oldFormat = row.size() >= 7 && row.get(5).trim().equalsIgnoreCase("shift");
                    continue;
                }
            }
            if (row.size() < 5 || row.get(0).trim().isEmpty() || row.get(1).trim().isEmpty()) continue;

            int age;
            try { age = Integer.parseInt(row.get(2).trim()); }
            catch (NumberFormatException e) { age = 0; }

            final String id = row.get(0).trim();
            boolean dup = false;
            for (Doctor existing : result) if (existing.getDoctorId().equalsIgnoreCase(id)) { dup = true; break; }
            if (dup) continue; // skip duplicate IDs from a hand-edited file

            Doctor d = new Doctor(id, row.get(1).trim(), age, row.get(3).trim(), row.get(4).trim());
            // rows without the availability column count as available
            int availableCol = oldFormat ? 6 : 5;
            d.setAvailable(row.size() <= availableCol || !row.get(availableCol).trim().equalsIgnoreCase("false"));
            result.add(d);
        }
        return result;
    }
}