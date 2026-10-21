package com.clinic.model;

import java.time.LocalDateTime;

public class Appointment {
    private final String appointmentId;
    private final String patientId;
    private final String doctorId; // "" when no doctor is assigned (e.g. emergency walk-ins)
    private LocalDateTime dateTime;
    private String reason;
    private AppointmentStatus status;

    public Appointment(String appointmentId, String patientId, String doctorId, LocalDateTime dateTime, String reason) {
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.doctorId = (doctorId == null) ? "" : doctorId;
        this.dateTime = dateTime;
        this.reason = reason;
        this.status = AppointmentStatus.PENDING; // Default status pagkagagawa
    }

    // Old signature kept so existing code keeps compiling
    public Appointment(String appointmentId, String patientId, LocalDateTime dateTime, String reason) {
        this(appointmentId, patientId, "", dateTime, reason);
    }

    public String getAppointmentId() { return appointmentId; }
    public String getPatientId() { return patientId; }
    public String getDoctorId() { return doctorId; }
    public LocalDateTime getDateTime() { return dateTime; }
    public String getReason() { return reason; }
    public AppointmentStatus getStatus() { return status; }

    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }
    public void setReason(String reason) { this.reason = reason; }
    public void setStatus(AppointmentStatus status) { this.status = status; }
}