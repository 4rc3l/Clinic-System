package com.clinic.file;

import com.clinic.model.Appointment;
import com.clinic.model.AppointmentStatus;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AppointmentFileHandler {
    public static final String FILE_NAME = "appointments.csv";
    private static final String HEADER = "appointmentId,patientId,doctorId,dateTime,reason,status";

    private final Path file;

    public AppointmentFileHandler() {
        this(CsvUtil.defaultDataDir().resolve(FILE_NAME));
    }

    public AppointmentFileHandler(Path file) {
        this.file = file;
    }

    public Path getFile() { return file; }

    public void ensureFileExists() throws IOException {
        CsvUtil.createIfMissing(file, HEADER);
    }

    public void save(Collection<Appointment> appointments) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (Appointment a : appointments) {
            lines.add(CsvUtil.toLine(
                    a.getAppointmentId(),
                    a.getPatientId(),
                    a.getDoctorId(),
                    a.getDateTime().toString(),
                    a.getReason(),
                    a.getStatus().name()));
        }
        CsvUtil.writeAtomically(file, lines);
    }

    public List<Appointment> load() throws IOException {
        List<Appointment> result = new ArrayList<>();
        ensureFileExists();
        String text = CsvUtil.readOrNull(file);
        if (text == null) return result;

        boolean first = true;
        for (List<String> row : CsvUtil.parse(text)) {
            if (first) { first = false; if (row.get(0).equals("appointmentId")) continue; }
            if (row.size() < 6 || row.get(0).trim().isEmpty() || row.get(1).trim().isEmpty()) continue;

            LocalDateTime when;
            try { when = LocalDateTime.parse(row.get(3).trim()); }
            catch (DateTimeParseException e) { continue; }

            AppointmentStatus status;
            try { status = AppointmentStatus.valueOf(row.get(5).trim().toUpperCase()); }
            catch (IllegalArgumentException e) { status = AppointmentStatus.PENDING; }

            Appointment a = new Appointment(row.get(0).trim(), row.get(1).trim(), row.get(2).trim(), when, row.get(4));
            a.setStatus(status);
            result.add(a);
        }
        return result;
    }
}