package com.clinic.ui;
import com.clinic.model.Appointment;
import com.clinic.model.Doctor;
import java.util.HashMap;
import java.util.Map;
import com.clinic.model.Patient;
import com.clinic.service.ClinicManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;

public class RegistrationPanel extends JPanel {

    private final ClinicManager clinic;
    private JTable patientsTable;
    private DefaultTableModel tableModel;

    public RegistrationPanel(ClinicManager clinic) {
        this.clinic = clinic;
        
        setLayout(new BorderLayout(20, 20));
        setBackground(new Color(230, 233, 236));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        JLabel title = new JLabel("Patients");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(new Color(30, 40, 50));

        JLabel dateLabel = new JLabel("October 9, 2026  📅 ");
        dateLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        dateLabel.setForeground(new Color(100, 110, 120));

        headerPanel.add(title, BorderLayout.WEST);
        headerPanel.add(dateLabel, BorderLayout.EAST);

        // Center Area
        JPanel centerArea = new JPanel(new BorderLayout(0, 20));
        centerArea.setOpaque(false);

        // Top Action Bar with Add Patient Button
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        actionPanel.setOpaque(false);

        JButton btnAddPatient = new JButton("ADD PATIENT");
        btnAddPatient.setFont(new Font("SansSerif", Font.BOLD, 15));
        btnAddPatient.setBackground(new Color(15, 60, 80));
        btnAddPatient.setForeground(Color.WHITE);
        btnAddPatient.setOpaque(true);
        btnAddPatient.setBorderPainted(false);
        btnAddPatient.setFocusPainted(false);
        btnAddPatient.setPreferredSize(new Dimension(320, 50));
        btnAddPatient.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        btnAddPatient.addActionListener(e -> {
            Window owner = SwingUtilities.getWindowAncestor(this);
            if (owner instanceof JFrame) {
                AddPatientDialog.open((JFrame) owner, clinic, this::refreshData);
            }
        });
        
        actionPanel.add(btnAddPatient);

        // Table Container
        JPanel tableContainer = new JPanel(new BorderLayout(0, 10));
        tableContainer.setBackground(Color.WHITE);
        tableContainer.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        String[] columns = {"Patient Name", "Age", "Contact No.", "Assigned Doctor", "Actions"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 4; // Allow interacting with the action buttons
            }
        };

        patientsTable = new JTable(tableModel);
        patientsTable.setRowHeight(45);
        patientsTable.setShowVerticalLines(false);
        patientsTable.setGridColor(new Color(230, 230, 230));
        patientsTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
        patientsTable.getTableHeader().setBackground(new Color(220, 225, 230));
        patientsTable.getTableHeader().setReorderingAllowed(false);

        // Attach custom renderer and editor for the Actions column (index 4)
        patientsTable.getColumnModel().getColumn(4).setCellRenderer(new ActionCellRenderer());
        patientsTable.getColumnModel().getColumn(4).setCellEditor(new ActionCellEditor(
            row -> System.out.println("Edit/Notes clicked for row: " + row),
            row -> System.out.println("Cancel clicked for row: " + row),
            row -> System.out.println("Confirm clicked for row: " + row)
        ));

        JScrollPane scrollPane = new JScrollPane(patientsTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        tableContainer.add(scrollPane, BorderLayout.CENTER);

        centerArea.add(actionPanel, BorderLayout.NORTH);
        centerArea.add(tableContainer, BorderLayout.CENTER);

        add(headerPanel, BorderLayout.NORTH);
        add(centerArea, BorderLayout.CENTER);

        refreshData();
    }

public void refreshData() {
    tableModel.setRowCount(0);

    Map<String, String> doctorNames = new HashMap<>();
    Map<String, String> patientDoctors = new HashMap<>();

    // Store doctor IDs and names
    for (Doctor d : clinic.getDoctors()) {
        doctorNames.put(d.getDoctorId(), d.getName());
    }

    // Match each patient to their assigned doctor
    for (Appointment a : clinic.getAppointments()) {
        String doctorName = doctorNames.get(a.getDoctorId());

        if (doctorName != null && !doctorName.isEmpty()) {
            patientDoctors.put(a.getPatientId(), doctorName);
        }
    }

    // Display patients and their actual assigned doctors
    List<Patient> patients = clinic.getPatients();

    for (Patient p : patients) {
        String doctor = patientDoctors.getOrDefault(
            p.getPatientId(), "Not Assigned"
        );

        tableModel.addRow(new Object[]{
            p.getName(),
            p.getAge(),
            p.getContactNumber(),
            doctor,
            ""
        });
    }

    revalidate();
    repaint();
}

    // --- Custom Action Buttons Renderer & Editor Classes ---

    private static class ActionPanel extends JPanel {
        public final JButton btnEdit = new JButton("📝");
        public final JButton btnCancel = new JButton("❌");
        public final JButton btnConfirm = new JButton("✅");

        public ActionPanel() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 6, 0));
            setOpaque(true);
            for (JButton b : new JButton[]{btnEdit, btnCancel, btnConfirm}) {
                b.setFont(new Font("SansSerif", Font.PLAIN, 13));
                b.setFocusPainted(false);
                b.setBorderPainted(false);
                b.setContentAreaFilled(false);
                b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                add(b);
            }
        }
    }

    private static class ActionCellRenderer extends ActionPanel implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
            return this;
        }
    }

    private static class ActionCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final ActionPanel panel = new ActionPanel();
        private int currentRow;

        public ActionCellEditor(java.util.function.Consumer<Integer> onEdit, 
                                java.util.function.Consumer<Integer> onCancel, 
                                java.util.function.Consumer<Integer> onConfirm) {
            panel.btnEdit.addActionListener(e -> {
                fireEditingStopped();
                onEdit.accept(currentRow);
            });
            panel.btnCancel.addActionListener(e -> {
                fireEditingStopped();
                onCancel.accept(currentRow);
            });
            panel.btnConfirm.addActionListener(e -> {
                fireEditingStopped();
                onConfirm.accept(currentRow);
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            currentRow = row;
            panel.setBackground(table.getSelectionBackground());
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }
    }
}