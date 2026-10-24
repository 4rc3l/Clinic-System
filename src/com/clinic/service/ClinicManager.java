package com.clinic.service;

import com.clinic.exception.*;
import com.clinic.model.*;
import com.clinic.file.AppointmentFileHandler;
import com.clinic.file.DiagnosisFileHandler;
import com.clinic.file.PatientFileHandler;
import com.clinic.util.InputValidator;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class ClinicManager {
    private final ArrayList<Patient> patients = new ArrayList<>();
    private final ArrayList<Doctor> doctors = new ArrayList<>();
    private final ArrayList<Appointment> appointments = new ArrayList<>();
    private final Random random = new Random();
    private int nextAppointmentNo = 1001; // counter, so IDs never repeat after removals

    private final PatientFileHandler patientFile;
    private final DiagnosisFileHandler diagnosisFile;
    private final AppointmentFileHandler appointmentFile;

    public ClinicManager() {
        this(new PatientFileHandler(), new DiagnosisFileHandler(), new AppointmentFileHandler());
    }

    public ClinicManager(Path dataDir) {
        this(new PatientFileHandler(dataDir.resolve(PatientFileHandler.FILE_NAME)),
                new DiagnosisFileHandler(dataDir.resolve(DiagnosisFileHandler.FILE_NAME)),
                new AppointmentFileHandler(dataDir.resolve(AppointmentFileHandler.FILE_NAME)));
    }

    private ClinicManager(PatientFileHandler patientFile, DiagnosisFileHandler diagnosisFile,
                          AppointmentFileHandler appointmentFile) {
        this.patientFile = patientFile;
        this.diagnosisFile = diagnosisFile;
        this.appointmentFile = appointmentFile;

        // Sample seed data
        doctors.add(new Doctor("D001", "Dr. Juan Cruz", 45, "09171234567", "General Physician", "Morning Shift"));
        doctors.add(new Doctor("D002", "Dr. Maria Santos", 38, "09189876543", "Pediatrics", "Night Shift"));

        loadFromFiles();
    }

    private void loadFromFiles() {
        System.out.println("Patient data is saved in: " + patientFile.getFile().getParent());
        try {
            patients.addAll(patientFile.load());
        } catch (Exception e) {
            System.err.println("Could not load " + patientFile.getFile() + ": " + e.getMessage());
        }
        try {
            diagnosisFile.loadInto(patients);
        } catch (Exception e) {
            System.err.println("Could not load " + diagnosisFile.getFile() + ": " + e.getMessage());
        }
        try {
            for (Appointment a : appointmentFile.load()) {
                if (isPatientIdExists(a.getPatientId())) {
                    appointments.add(a);
                    nextAppointmentNo = Math.max(nextAppointmentNo, appointmentNumber(a.getAppointmentId()) + 1);
                }
            }
        } catch (Exception e) {
            System.err.println("Could not load " + appointmentFile.getFile() + ": " + e.getMessage());
        }
        System.out.println("Loaded " + patients.size() + " patients, " + appointments.size() + " appointments");
    }

    private int appointmentNumber(String appointmentId) {
        try {
            return Integer.parseInt(appointmentId.substring(1));
        } catch (Exception e) {
            return 0;
        }
    }

    private void saveAppointments() {
        try {
            appointmentFile.save(appointments);
        } catch (Exception e) {
            System.err.println("Could not save " + appointmentFile.getFile() + ": " + e.getMessage());
        }
    }

    private void savePatients() {
        try {
            patientFile.save(patients);
        } catch (Exception e) {
            System.err.println("Could not save " + patientFile.getFile() + ": " + e.getMessage());
        }
    }

    private void saveDiagnoses() {
        try {
            diagnosisFile.save(patients);
        } catch (Exception e) {
            System.err.println("Could not save " + diagnosisFile.getFile() + ": " + e.getMessage());
        }
    }

    // --- Private Helpers ---
    private Patient locatePatient(String id) throws PatientNotFoundException {
        for (Patient p : patients) {
            if (p.getPatientId().equalsIgnoreCase(id)) return p;
        }
        throw new PatientNotFoundException("Patient with ID " + id + " not found!");
    }

    private Doctor locateDoctor(String id) throws InvalidInputException {
        for (Doctor d : doctors) {
            if (d.getDoctorId().equalsIgnoreCase(id)) return d;
        }
        throw new InvalidInputException("Doctor with ID " + id + " not found!");
    }

    /** The doctor must exist, be available, and have no pending appointment at the same time. */
    private void ensureDoctorFree(String doctorId, LocalDateTime when) throws InvalidInputException {
        Doctor d = locateDoctor(doctorId);
        if (!d.isAvailable()) {
            throw new InvalidInputException(d.getName() + " is not available!");
        }
        for (Appointment a : appointments) {
            if (a.getStatus() == AppointmentStatus.PENDING
                    && doctorId.equalsIgnoreCase(a.getDoctorId())
                    && a.getDateTime().equals(when)) {
                throw new InvalidInputException(d.getName() + " already has an appointment at that time!");
            }
        }
    }

    private String generatePatientId() {
        String id;
        do {
            int num = 10000 + random.nextInt(90000); // 5-digit random
            id = "P" + num;
        } while (isPatientIdExists(id));
        return id;
    }

    private String generateAppointmentId() {
        return "A" + (nextAppointmentNo++);
    }

    private boolean isPatientIdExists(String id) {
        for (Patient p : patients) {
            if (p.getPatientId().equalsIgnoreCase(id)) return true;
        }
        return false;
    }

    private boolean isDuplicate(String name, String contact) {
        for (Patient p : patients) {
            if (p.getName().equalsIgnoreCase(name) && p.getContactNumber().equals(contact)) {
                return true;
            }
        }
        return false;
    }

    // --- Feature 1: Patient Registration ---

    // Old signature kept so existing callers keep working (no doctor assigned)
    public Patient registerPatient(String name, int age, String contact, LocalDateTime schedule, String reason)
            throws InvalidInputException, DuplicatePatientException, PatientNotFoundException {
        return registerPatient(name, age, contact, null, schedule, reason);
    }

    public Patient registerPatient(String name, int age, String contact, String doctorId,
                                   LocalDateTime schedule, String reason)
            throws InvalidInputException, DuplicatePatientException, PatientNotFoundException {
        name = InputValidator.normalizeSpaces(name);

        // Validate everything first, so nothing is stored when an input is bad
        if (!InputValidator.isValidName(name)) throw new InvalidInputException("Name must contain letters only (2-50 characters)!");
        if (!InputValidator.isValidAge(age)) throw new InvalidInputException("Age must be between " + InputValidator.MIN_AGE + " and " + InputValidator.MAX_AGE + "!");
        if (!InputValidator.isValidContact(contact)) throw new InvalidInputException("Contact must start with 09 and be 11 digits!");
        if (isDuplicate(name, contact)) throw new DuplicatePatientException("Patient with same name and contact already exists!");

        if (schedule != null) {
            String timeError = InputValidator.appointmentTimeError(schedule);
            if (timeError != null) throw new InvalidInputException(timeError);
            if (reason != null && !InputValidator.isWithinLength(reason, InputValidator.MAX_REASON_LENGTH)) {
                throw new InvalidInputException("Reason is too long (max " + InputValidator.MAX_REASON_LENGTH + " characters)!");
            }
            if (doctorId != null && !doctorId.isEmpty()) ensureDoctorFree(doctorId, schedule);
        }

        Patient p = new Patient(generatePatientId(), name, age, contact);
        patients.add(p);
        savePatients();

        if (schedule != null) {
            scheduleAppointment(p.getPatientId(), doctorId, schedule, reason);
        }
        return p;
    }

    // Extra: Emergency Registration (Name only)
    public Patient registerEmergencyPatient(String name) throws InvalidInputException, PatientNotFoundException {
        if (!InputValidator.isNotBlank(name)) throw new InvalidInputException("Name cannot be blank!");

        String pId = generatePatientId();
        Patient p = new Patient(pId, InputValidator.normalizeSpaces(name));
        patients.add(p);
        savePatients();

        // Auto-create appointment for today
        scheduleAppointment(pId, LocalDateTime.now(), "Emergency Walk-in");
        return p;
    }

    // --- Feature 2: Record Edits ---
    public void updateRecord(String patientId, int age, String diagnosis, String notes, String contact)
            throws PatientNotFoundException, InvalidInputException {
        Patient p = locatePatient(patientId);

        if (contact != null && !contact.isEmpty() && !InputValidator.isValidContact(contact)) {
            throw new InvalidInputException("Invalid contact number format!");
        }
        if (age != 0 && !InputValidator.isValidAge(age)) {
            throw new InvalidInputException("Age must be between " + InputValidator.MIN_AGE + " and " + InputValidator.MAX_AGE + "!");
        }
        if (!InputValidator.isValidNotes(diagnosis) || !InputValidator.isValidNotes(notes)) {
            throw new InvalidInputException("Diagnosis and notes must be at most " + InputValidator.MAX_NOTES_LENGTH + " characters!");
        }

        if (age != 0) p.setAge(age);
        if (InputValidator.isNotBlank(contact)) p.setContactNumber(contact);
        if (InputValidator.isNotBlank(diagnosis)) p.setDiagnosis(diagnosis);
        if (InputValidator.isNotBlank(notes)) p.setNotes(notes);

        savePatients();
        saveDiagnoses();
    }

    // --- Feature 3: Cancellation & Removal ---
    public void scheduleAppointment(String patientId, LocalDateTime dateTime, String reason) throws PatientNotFoundException {
        scheduleAppointment(patientId, "", dateTime, reason);
    }

    public void scheduleAppointment(String patientId, String doctorId, LocalDateTime dateTime, String reason)
            throws PatientNotFoundException {
        locatePatient(patientId); // Ensure patient exists
        String aId = generateAppointmentId();
        appointments.add(new Appointment(aId, patientId, doctorId, dateTime, reason));
        saveAppointments();
    }

    public void cancelAppointment(String appointmentId) throws AppointmentNotFoundException {
        for (Appointment a : appointments) {
            if (a.getAppointmentId().equalsIgnoreCase(appointmentId)) {
                a.setStatus(AppointmentStatus.CANCELLED);
                saveAppointments();
                return;
            }
        }
        throw new AppointmentNotFoundException("Appointment " + appointmentId + " not found!");
    }

    public void removePatient(String patientId) throws PatientNotFoundException {
        Patient p = locatePatient(patientId);
        patients.remove(p);
        appointments.removeIf(a -> a.getPatientId().equalsIgnoreCase(patientId)); // Purge appointments
        savePatients();
        saveDiagnoses();
        saveAppointments();
    }

    // --- Feature 4: Patient Search ---
    public Patient searchById(String patientId) throws PatientNotFoundException {
        return locatePatient(patientId);
    }

    public List<Patient> searchByName(String partialName) {
        List<Patient> result = new ArrayList<>();
        for (Patient p : patients) {
            if (p.getName().toLowerCase().contains(partialName.toLowerCase())) {
                result.add(p);
            }
        }
        return result;
    }

    // --- Feature 5: Pending Appointments ---
    public List<Appointment> getPendingAppointments() {
        List<Appointment> pending = new ArrayList<>();
        for (Appointment a : appointments) {
            if (a.getStatus() == AppointmentStatus.PENDING) {
                pending.add(a);
            }
        }
        pending.sort(Comparator.comparing(Appointment::getDateTime));
        return pending;
    }

    // --- Feature 6: Report Generation (Returns String) ---
    public String generateMasterPatientList() {
        StringBuilder sb = new StringBuilder("=== MASTER PATIENT LIST ===\n");
        if (patients.isEmpty()) return sb.append("No patient records.\n").toString();

        for (Patient p : patients) {
            sb.append(p.displayInfo()).append("\n");
        }
        return sb.toString();
    }

    public String generateDailyAppointments(LocalDate date) {
        StringBuilder sb = new StringBuilder("=== DAILY APPOINTMENTS (" + date + ") ===\n");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("hh:mm a");

        List<Appointment> today = new ArrayList<>();
        for (Appointment a : appointments) {
            if (a.getDateTime().toLocalDate().equals(date)) today.add(a);
        }
        today.sort(Comparator.comparing(Appointment::getDateTime));

        for (Appointment a : today) {
            sb.append("[").append(a.getDateTime().format(timeFmt)).append("] ")
                    .append("App ID: ").append(a.getAppointmentId())
                    .append(" | Patient ID: ").append(a.getPatientId())
                    .append(" | Status: ").append(a.getStatus())
                    .append(" | Reason: ").append(a.getReason()).append("\n");
        }
        if (today.isEmpty()) sb.append("No appointments scheduled for this date.\n");
        return sb.toString();
    }

    // --- Lookups used by the Search and Records pages ---

    /** Returns the doctor with this ID, or null when there is none. */
    public Doctor getDoctorById(String id) {
        if (id == null) return null;
        for (Doctor d : doctors) {
            if (d.getDoctorId().equalsIgnoreCase(id)) return d;
        }
        return null;
    }

    /** All appointments of one patient, oldest first. */
    public List<Appointment> getAppointmentsForPatient(String patientId) {
        List<Appointment> list = new ArrayList<>();
        for (Appointment a : appointments) {
            if (a.getPatientId().equalsIgnoreCase(patientId)) list.add(a);
        }
        list.sort(Comparator.comparing(Appointment::getDateTime));
        return list;
    }

    /** All appointments of one doctor, oldest first. */
    public List<Appointment> getAppointmentsForDoctor(String doctorId) {
        List<Appointment> list = new ArrayList<>();
        for (Appointment a : appointments) {
            if (a.getDoctorId().equalsIgnoreCase(doctorId)) list.add(a);
        }
        list.sort(Comparator.comparing(Appointment::getDateTime));
        return list;
    }

    /** Soonest pending appointment from today onwards, or null. */
    public Appointment getNextAppointment(String patientId) {
        LocalDate today = LocalDate.now();
        for (Appointment a : getAppointmentsForPatient(patientId)) {
            if (a.getStatus() == AppointmentStatus.PENDING && !a.getDateTime().toLocalDate().isBefore(today)) {
                return a;
            }
        }
        return null;
    }

    /** Doctor of the next appointment, otherwise of the latest appointment that had one; null if none. */
    public Doctor getAssignedDoctor(String patientId) {
        Appointment next = getNextAppointment(patientId);
        if (next != null) {
            Doctor d = getDoctorById(next.getDoctorId());
            if (d != null) return d;
        }
        List<Appointment> all = getAppointmentsForPatient(patientId);
        for (int i = all.size() - 1; i >= 0; i--) {
            Doctor d = getDoctorById(all.get(i).getDoctorId());
            if (d != null) return d;
        }
        return null;
    }

    /** Today's appointments for a doctor, not counting cancelled ones. */
    public int countAppointmentsToday(String doctorId) {
        LocalDate today = LocalDate.now();
        int count = 0;
        for (Appointment a : appointments) {
            if (a.getDoctorId().equalsIgnoreCase(doctorId)
                    && a.getStatus() != AppointmentStatus.CANCELLED
                    && a.getDateTime().toLocalDate().equals(today)) {
                count++;
            }
        }
        return count;
    }

    /** Distinct patients who have (or had) an appointment with this doctor. */
    public List<Patient> getPatientsOfDoctor(String doctorId) {
        java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
        for (Appointment a : getAppointmentsForDoctor(doctorId)) ids.add(a.getPatientId().toUpperCase());
        List<Patient> result = new ArrayList<>();
        for (String id : ids) {
            for (Patient p : patients) {
                if (p.getPatientId().equalsIgnoreCase(id)) {
                    result.add(p);
                    break;
                }
            }
        }
        return result;
    }

    /** Saves diagnosis and notes. Unlike updateRecord, blank text clears the field. */
    public void updateMedicalNotes(String patientId, String diagnosis, String notes)
            throws PatientNotFoundException, InvalidInputException {
        Patient p = locatePatient(patientId);
        String d = diagnosis == null ? "" : diagnosis.trim();
        String n = notes == null ? "" : notes.trim();
        if (!InputValidator.isValidNotes(d) || !InputValidator.isValidNotes(n)) {
            throw new InvalidInputException("Diagnosis and notes must be at most "
                    + InputValidator.MAX_NOTES_LENGTH + " characters each!");
        }
        p.setDiagnosis(d);
        p.setNotes(n);
        saveDiagnoses();
    }

    // Read-only views, so the UI can't change the internal lists by accident
    public List<Patient> getPatients() { return Collections.unmodifiableList(patients); }
    public List<Doctor> getDoctors() { return Collections.unmodifiableList(doctors); }
    public List<Appointment> getAppointments() { return Collections.unmodifiableList(appointments); }
}