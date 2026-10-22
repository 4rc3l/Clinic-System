package com.clinic.ui;

import com.clinic.model.Appointment;
import com.clinic.model.AppointmentStatus;
import com.clinic.model.Doctor;
import com.clinic.model.Patient;
import com.clinic.service.ClinicManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * "Records" page. Shows one patient's record (details, appointments, diagnosis and notes)
 * or one doctor's record (details, duty shift, assigned patients).
 *
 * Open one with showPatient(...) / showDoctor(...), usually from the Search page.
 */
public class RecordsPanel extends JPanel {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private final ClinicManager clinic;
    private final Runnable onBack;

    private Patient currentPatient;
    private Doctor currentDoctor;
    private Doctor returnDoctor; // set when a patient was opened from a doctor's record

    private final JLabel titleLabel = new JLabel("Records");
    private final JButton backButton = new JButton("Back to search", Icons.of(Icons.Type.ARROW_LEFT, 18, Theme.PRIMARY));
    private final VScrollPanel holder = new VScrollPanel(new BorderLayout());
    private final JScrollPane scroll;

    public RecordsPanel(ClinicManager clinic, Runnable onBack) {
        this.clinic = clinic;
        this.onBack = onBack;

        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.CONTENT_BG);
        setBorder(new EmptyBorder(22, 24, 22, 24));

        titleLabel.setFont(Theme.font(Font.BOLD, 22));
        titleLabel.setForeground(Theme.TEXT_DARK);

        backButton.setFont(Theme.font(Font.BOLD, 15));
        backButton.setForeground(Theme.PRIMARY);
        backButton.setIconTextGap(8);
        backButton.setContentAreaFilled(false);
        backButton.setBorderPainted(false);
        backButton.setFocusPainted(false);
        backButton.setCursor(Theme.hand());
        backButton.addActionListener(e -> goBack());

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(titleLabel, BorderLayout.WEST);
        header.add(backButton, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        holder.setOpaque(false);
        scroll = new JScrollPane(holder);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        add(scroll, BorderLayout.CENTER);

        render();
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    public void showPatient(Patient patient) {
        currentPatient = patient;
        currentDoctor = null;
        returnDoctor = null;
        render();
    }

    public void showDoctor(Doctor doctor) {
        currentDoctor = doctor;
        currentPatient = null;
        returnDoctor = null;
        render();
    }

    /** Re-reads the records (call when the page becomes visible). */
    public void refresh() {
        if (currentPatient != null && !clinic.getPatients().contains(currentPatient)) currentPatient = null; // removed
        render();
    }

    private void openPatientFromDoctor(Patient patient, Doctor from) {
        currentPatient = patient;
        currentDoctor = null;
        returnDoctor = from;
        render();
    }

    private void goBack() {
        if (returnDoctor != null) {
            showDoctor(returnDoctor);
        } else {
            onBack.run();
        }
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    private void render() {
        holder.removeAll();
        if (currentPatient != null) {
            titleLabel.setText("Patient Record");
            backButton.setText(returnDoctor != null ? "Back to doctor" : "Back to search");
            backButton.setVisible(true);
            holder.add(buildPatientView(currentPatient), BorderLayout.NORTH);
        } else if (currentDoctor != null) {
            titleLabel.setText("Doctor Record");
            backButton.setText("Back to search");
            backButton.setVisible(true);
            holder.add(buildDoctorView(currentDoctor), BorderLayout.NORTH);
        } else {
            titleLabel.setText("Records");
            backButton.setVisible(false);
            holder.add(buildEmptyView(), BorderLayout.CENTER);
        }
        holder.revalidate();
        holder.repaint();
        SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
    }

    private JComponent buildEmptyView() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(100, 360));

        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        JLabel msg = new JLabel("Select a patient or doctor from Search to see their record.");
        msg.setFont(Theme.font(Font.PLAIN, 18));
        msg.setForeground(Theme.TEXT_MUTED);
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);

        PillButton go = PillButton.primary("Go to Search", Icons.of(Icons.Type.SEARCH, 22, Color.WHITE));
        go.setPreferredSize(new Dimension(220, 50));
        go.setMaximumSize(new Dimension(220, 50));
        go.setAlignmentX(Component.CENTER_ALIGNMENT);
        go.addActionListener(e -> onBack.run());

        box.add(msg);
        box.add(Box.createVerticalStrut(16));
        box.add(go);
        p.add(box);
        return p;
    }

    // ------------------------------------------------------------------
    // Patient record
    // ------------------------------------------------------------------

    private JComponent buildPatientView(Patient p) {
        List<Appointment> appts = clinic.getAppointmentsForPatient(p.getPatientId());
        Doctor assigned = clinic.getAssignedDoctor(p.getPatientId());
        Appointment next = clinic.getNextAppointment(p.getPatientId());

        int pending = 0, completed = 0, cancelled = 0;
        for (Appointment a : appts) {
            if (a.getStatus() == AppointmentStatus.PENDING) pending++;
            else if (a.getStatus() == AppointmentStatus.COMPLETED) completed++;
            else if (a.getStatus() == AppointmentStatus.CANCELLED) cancelled++;
        }

        // profile
        UiKit.Pill visitPill = new UiKit.Pill();
        if (next != null) visitPill.style("Upcoming visit", Theme.PENDING_BG, Theme.PENDING_FG);
        else visitPill.style("No upcoming visit", new Color(0xE3E6E8), Theme.TEXT_MUTED);

        JComponent profile = profileCard(
                new UiKit.Avatar(Icons.Type.USER, 84, UiKit.AVATAR_BLUE, Color.WHITE),
                p.getName(),
                "Patient ID: " + p.getPatientId(),
                visitPill,
                new String[][]{
                        {"Age", p.getAge() == 0 ? "N/A" : String.valueOf(p.getAge())},
                        {"Contact", p.getContactNumber().isEmpty() ? "N/A" : p.getContactNumber()},
                        {"Assigned Doctor", assigned == null ? "Not assigned"
                                : assigned.getName() + " (" + assigned.getSpecialization() + ")"},
                        {"Next Visit", next == null ? "None scheduled" : UiKit.formatDate(next.getDateTime().toLocalDate())}
                });

        JComponent stats = statsRow(
                new Object[]{appts.size(), "Total Appointments", Theme.TILE_BLUE},
                new Object[]{pending, "Pending", Theme.TILE_YELLOW},
                new Object[]{completed, "Completed", Theme.TILE_GREEN},
                new Object[]{cancelled, "Cancelled", Theme.CANCELLED_BG});

        // appointment history
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"ID", "Date", "Time", "Doctor", "Reason", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Appointment a : appts) {
            Doctor d = clinic.getDoctorById(a.getDoctorId());
            model.addRow(new Object[]{
                    a.getAppointmentId(),
                    UiKit.formatDate(a.getDateTime().toLocalDate()),
                    a.getDateTime().format(TIME_FMT),
                    d == null ? "-" : d.getName(),
                    a.getReason(),
                    a.getStatus().name()});
        }
        JTable table = new JTable(model);
        UiKit.styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(70);
        table.getColumnModel().getColumn(1).setPreferredWidth(110);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(130);
        table.getColumnModel().getColumn(4).setPreferredWidth(110);
        table.getColumnModel().getColumn(5).setPreferredWidth(110);
        table.getColumnModel().getColumn(5).setCellRenderer(new UiKit.StatusRenderer());

        JComponent historyCard = sectionCard("Appointment History", null,
                appts.isEmpty() ? emptyNote("No appointments yet.") : tableScroll(table));

        JComponent notesCard = buildNotesCard(p);

        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setPreferredSize(new Dimension(100, 400));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1;
        gc.gridx = 0;
        gc.weightx = 0.62;
        gc.insets = new Insets(0, 0, 0, 9);
        row.add(historyCard, gc);
        gc.gridx = 1;
        gc.weightx = 0.38;
        gc.insets = new Insets(0, 9, 0, 0);
        row.add(notesCard, gc);

        return stack(profile, stats, row);
    }

    private JComponent buildNotesCard(Patient p) {
        JTextArea diagnosis = textArea(p.getDiagnosis());
        JTextArea notes = textArea(p.getNotes());

        JLabel status = new JLabel(" ");
        status.setFont(Theme.font(Font.PLAIN, 13));

        PillButton save = PillButton.primary("Save", null);
        save.setFont(Theme.font(Font.BOLD, 16));
        save.setPreferredSize(new Dimension(120, 42));
        save.setMaximumSize(new Dimension(120, 42));
        save.addActionListener(e -> {
            try {
                clinic.updateMedicalNotes(p.getPatientId(), diagnosis.getText(), notes.getText());
                status.setForeground(Theme.DONE_FG);
                status.setText("Saved.");
            } catch (Exception ex) {
                status.setForeground(Theme.ERROR);
                status.setText(ex.getMessage());
            }
        });

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(fieldLabel("Diagnosis / Results"));
        body.add(Box.createVerticalStrut(6));
        body.add(wrapArea(diagnosis, 96));
        body.add(Box.createVerticalStrut(14));
        body.add(fieldLabel("Notes"));
        body.add(Box.createVerticalStrut(6));
        body.add(wrapArea(notes, 120));
        body.add(Box.createVerticalGlue());
        body.add(Box.createVerticalStrut(10));

        JPanel foot = new JPanel(new BorderLayout(12, 0));
        foot.setOpaque(false);
        foot.setAlignmentX(Component.LEFT_ALIGNMENT);
        foot.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        foot.add(save, BorderLayout.WEST);
        foot.add(status, BorderLayout.CENTER);
        body.add(foot);

        return sectionCard("Diagnosis & Notes", null, body);
    }

    private static JTextArea textArea(String text) {
        JTextArea a = new JTextArea(text == null ? "" : text);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setFont(Theme.font(Font.PLAIN, 14));
        a.setForeground(Theme.TEXT_DARK);
        a.setBorder(new EmptyBorder(8, 10, 8, 10));
        return a;
    }

    private static JComponent wrapArea(JTextArea area, int height) {
        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(new LineBorder(Theme.BORDER, 1, true));
        sp.setAlignmentX(Component.LEFT_ALIGNMENT);
        sp.setPreferredSize(new Dimension(100, height));
        sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return sp;
    }

    private static JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.font(Font.BOLD, 14));
        l.setForeground(Theme.PRIMARY);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    // ------------------------------------------------------------------
    // Doctor record
    // ------------------------------------------------------------------

    private JComponent buildDoctorView(Doctor d) {
        List<Appointment> appts = clinic.getAppointmentsForDoctor(d.getDoctorId());
        List<Patient> patients = clinic.getPatientsOfDoctor(d.getDoctorId());
        LocalDate today = LocalDate.now();

        int pending = 0, completed = 0;
        Map<String, Integer> visits = new HashMap<>();
        Map<String, LocalDate> nextVisit = new HashMap<>();
        for (Appointment a : appts) {
            if (a.getStatus() == AppointmentStatus.PENDING) pending++;
            if (a.getStatus() == AppointmentStatus.COMPLETED) completed++;
            String key = a.getPatientId().toUpperCase();
            visits.merge(key, 1, Integer::sum);
            LocalDate date = a.getDateTime().toLocalDate();
            if (a.getStatus() == AppointmentStatus.PENDING && !date.isBefore(today)) {
                nextVisit.merge(key, date, (x, y) -> x.isBefore(y) ? x : y);
            }
        }

        UiKit.Pill availability = new UiKit.Pill();
        availability.setStatus(d.isAvailable() ? "Available" : "On leave");

        JComponent profile = profileCard(
                new UiKit.Avatar(Icons.Type.DOCTOR, 84, UiKit.AVATAR_BLUE, Color.WHITE),
                d.getName(),
                d.getSpecialization() + "  |  Doctor ID: " + d.getDoctorId(),
                availability,
                new String[][]{
                        {"Specialization", d.getSpecialization()},
                        {"Duty Shift", d.getShift()},
                        {"Age", d.getAge() == 0 ? "N/A" : String.valueOf(d.getAge())},
                        {"Contact", d.getContactNumber().isEmpty() ? "N/A" : d.getContactNumber()}
                });

        JComponent stats = statsRow(
                new Object[]{clinic.countAppointmentsToday(d.getDoctorId()), "Appointments Today", Theme.TILE_BLUE},
                new Object[]{pending, "Pending", Theme.TILE_YELLOW},
                new Object[]{completed, "Completed", Theme.TILE_GREEN},
                new Object[]{patients.size(), "Patients Assigned", new Color(0xD9CFF5)});

        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Patient Name", "Patient ID", "Contact", "Visits", "Next Visit"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        List<Patient> rowPatients = new ArrayList<>();
        for (Patient p : patients) {
            String key = p.getPatientId().toUpperCase();
            LocalDate next = nextVisit.get(key);
            model.addRow(new Object[]{
                    p.getName(),
                    p.getPatientId(),
                    p.getContactNumber().isEmpty() ? "N/A" : p.getContactNumber(),
                    visits.getOrDefault(key, 0),
                    next == null ? "-" : UiKit.formatDate(next)});
            rowPatients.add(p);
        }
        JTable table = new JTable(model);
        UiKit.styleTable(table);
        table.setCursor(Theme.hand());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0 && row < rowPatients.size()) openPatientFromDoctor(rowPatients.get(row), d);
            }
        });

        JComponent listCard = sectionCard("Assigned Patients", "Click a patient to open their record",
                patients.isEmpty() ? emptyNote("No patients assigned to this doctor yet.") : tableScroll(table));
        listCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        listCard.setPreferredSize(new Dimension(100, 380));
        listCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 380));

        return stack(profile, stats, listCard);
    }

    // ------------------------------------------------------------------
    // Shared building blocks
    // ------------------------------------------------------------------

    /** Vertical stack of full-width sections with a gap between them. */
    private static JComponent stack(JComponent... parts) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        for (int i = 0; i < parts.length; i++) {
            parts[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(parts[i]);
            if (i < parts.length - 1) p.add(Box.createVerticalStrut(18));
        }
        return p;
    }

    private JComponent profileCard(JComponent avatar, String name, String subtitle, JComponent badge, String[][] info) {
        UiKit.Card card = new UiKit.Card(18, Color.WHITE, Theme.CARD_STROKE);
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(Theme.font(Font.BOLD, 26));
        nameLabel.setForeground(Theme.TEXT_DARK);
        JLabel sub = new JLabel(subtitle);
        sub.setFont(Theme.font(Font.PLAIN, 15));
        sub.setForeground(Theme.PRIMARY);

        JPanel names = new JPanel();
        names.setOpaque(false);
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        names.add(nameLabel);
        names.add(Box.createVerticalStrut(4));
        names.add(sub);

        JPanel namesWrap = new JPanel(new GridBagLayout());
        namesWrap.setOpaque(false);
        GridBagConstraints left = new GridBagConstraints();
        left.anchor = GridBagConstraints.WEST;
        left.weightx = 1;
        namesWrap.add(names, left);

        JPanel badgeWrap = new JPanel(new GridBagLayout());
        badgeWrap.setOpaque(false);
        badgeWrap.add(badge);

        JPanel avatarWrap = new JPanel(new GridBagLayout());
        avatarWrap.setOpaque(false);
        avatarWrap.add(avatar);

        JPanel top = new JPanel(new BorderLayout(18, 0));
        top.setOpaque(false);
        top.add(avatarWrap, BorderLayout.WEST);
        top.add(namesWrap, BorderLayout.CENTER);
        top.add(badgeWrap, BorderLayout.EAST);

        JPanel grid = new JPanel(new GridLayout(1, info.length, 18, 0));
        grid.setOpaque(false);
        grid.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 0, 0, 0, Theme.BORDER), new EmptyBorder(14, 0, 0, 0)));
        for (String[] item : info) {
            JPanel cell = new JPanel();
            cell.setOpaque(false);
            cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
            JLabel k = new JLabel(item[0]);
            k.setFont(Theme.font(Font.PLAIN, 13));
            k.setForeground(Theme.TEXT_MUTED);
            JLabel v = new JLabel("<html>" + UiKit.esc(item[1]) + "</html>");
            v.setFont(Theme.font(Font.BOLD, 15));
            v.setForeground(Theme.TEXT_DARK);
            cell.add(k);
            cell.add(Box.createVerticalStrut(3));
            cell.add(v);
            grid.add(cell);
        }

        card.add(top, BorderLayout.NORTH);
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    /** Four small number tiles. Each item is {Integer count, String caption, Color accent}. */
    private JComponent statsRow(Object[]... items) {
        JPanel row = new JPanel(new GridLayout(1, items.length, 18, 0));
        row.setOpaque(false);
        for (Object[] it : items) {
            final Color accent = (Color) it[2];
            UiKit.Card tile = new UiKit.Card(16, Color.WHITE, Theme.CARD_STROKE);
            tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
            tile.setBorder(new EmptyBorder(14, 18, 14, 18));

            JComponent bar = new JComponent() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    Theme.antialias(g2);
                    g2.setColor(accent);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                    g2.dispose();
                }
            };
            bar.setPreferredSize(new Dimension(44, 7));
            bar.setMaximumSize(new Dimension(44, 7));
            bar.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel number = new JLabel(String.valueOf(it[0]));
            number.setFont(Theme.font(Font.BOLD, 30));
            number.setForeground(Theme.TEXT_DARK);
            number.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel caption = new JLabel((String) it[1]);
            caption.setFont(Theme.font(Font.PLAIN, 14));
            caption.setForeground(Theme.TEXT_MUTED);
            caption.setAlignmentX(Component.LEFT_ALIGNMENT);

            tile.add(bar);
            tile.add(Box.createVerticalStrut(8));
            tile.add(number);
            tile.add(caption);
            row.add(tile);
        }
        row.setPreferredSize(new Dimension(100, 104));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 104));
        return row;
    }

    /** White card with a bold title, optional hint, and content. */
    private JComponent sectionCard(String title, String hint, JComponent content) {
        UiKit.Card card = new UiKit.Card(18, Color.WHITE, Theme.CARD_STROKE);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(16, 20, 18, 20));

        JLabel t = new JLabel(title);
        t.setFont(Theme.font(Font.BOLD, 19));
        t.setForeground(Theme.TEXT_DARK);

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(t, BorderLayout.WEST);
        if (hint != null) {
            JLabel h = new JLabel(hint);
            h.setFont(Theme.font(Font.PLAIN, 13));
            h.setForeground(Theme.TEXT_MUTED);
            head.add(h, BorderLayout.EAST);
        }

        card.add(head, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private static JComponent tableScroll(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(new LineBorder(new Color(0xD3D3D3), 1, true));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    private static JComponent emptyNote(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(Theme.font(Font.PLAIN, 15));
        l.setForeground(Theme.TEXT_MUTED);
        return l;
    }
}