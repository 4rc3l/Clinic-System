package com.clinic.model;

public class Doctor extends Persons {
    private final String doctorId;
    private String specialization;
    private boolean isAvailable;

    public Doctor(String doctorId, String name, int age, String contactNumber, String specialization) {
        super(name, age, contactNumber);
        this.doctorId = doctorId;
        this.specialization = specialization;
        this.isAvailable = true;
    }

    public String getDoctorId() { return doctorId; }
    public String getSpecialization() { return specialization; }
    public boolean isAvailable() { return isAvailable; }

    public void setSpecialization(String specialization) { this.specialization = specialization; }
    public void setAvailable(boolean available) { isAvailable = available; }

    @Override
    public String displayInfo() {
        String status = isAvailable ? "Available" : "On Leave / Busy";
        String shown = name.startsWith("Dr.") ? name : "Dr. " + name;
        return shown + " (" + specialization + ") | ID: " + doctorId +
                " | Status: " + status;
    }
}