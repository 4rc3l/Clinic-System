package com.clinic.ui;

import com.clinic.model.Appointment;
import com.clinic.model.Doctor;
import com.clinic.service.ClinicManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;

/**
 * "Reschedule Appointment" pop-up, opened from a patient's record.
 * Moves a PENDING appointment to a new date/time. The appointment keeps its ID,
 * doctor and bill - only the schedule changes - so the patient's history stays in one place.
 */
public class RescheduleDialog extends JDialog {

    private static final Color DARK = Theme.SIDEBAR_BG;

    private static final DateTimeFormatter DATE_PARSER = DateTimeFormatter
            .ofPattern("MM/dd/uuuu", Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter NICE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a", Locale.ENGLISH);

    private final ClinicManager clinic;
    private final Appointment appointment;
    private final Runnable onDone;

    private final JTextField dateField = FormField.textField("MM/DD/YYYY");
    private final TimePicker timePicker = new TimePicker();
    private final IconButton calendarButton = new IconButton(
            Icons.of(Icons.Type.CALENDAR, 20, DARK), "Pick a date", new Color(0, 0, 0, 20));
    private final JLabel errorLabel = new JLabel(" ");
    private PillButton saveButton;

    public static void open(JFrame owner, ClinicManager clinic, Appointment appointment, Runnable onDone) {
        Component oldGlass = owner.getGlassPane();
        JComponent dim = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(0, 0, 0, 120));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        dim.addMouseListener(new MouseAdapter() { });
        owner.setGlassPane(dim);
        dim.setVisible(true);

        RescheduleDialog dialog = new RescheduleDialog(owner, clinic, appointment, onDone);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);

        dim.setVisible(false);
        owner.setGlassPane(oldGlass);
    }

    private RescheduleDialog(JFrame owner, ClinicManager clinic, Appointment appointment, Runnable onDone) {
        super(owner, "Reschedule Appointment", true);
        this.clinic = clinic;
        this.appointment = appointment;
        this.onDone = onDone;

        setUndecorated(true);

        Surface surface = new Surface();
        surface.setLayout(new BorderLayout());
        surface.add(buildHeader(), BorderLayout.NORTH);
        surface.add(buildForm(), BorderLayout.CENTER);
        setContentPane(surface);

        // Start from the current slot, or tomorrow if the current one is today/past.
        LocalDate current = appointment.getDateTime().toLocalDate();
        LocalDate start = current.isAfter(LocalDate.now()) ? current : LocalDate.now().plusDays(1);
        dateField.setText(start.format(DATE_FMT));
        timePicker.setTime(appointment.getDateTime().toLocalTime());

        limitDateField();
        getRootPane().setDefaultButton(saveButton);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e) { dateField.requestFocusInWindow(); }
        });
        pack();
        setShape(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 34, 34));
    }

    private JComponent buildHeader() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(22, 24, 16, 24));

        JLabel title = new JLabel("Reschedule Appointment");
        title.setFont(Theme.font(Font.BOLD, 30));
        title.setForeground(DARK);
        row.add(new JLabel(Icons.of(Icons.Type.CALENDAR_CLOCK, 42, DARK)));
        row.add(title);

        JPanel divider = new JPanel();
        divider.setBackground(new Color(0xD9D9D9));
        divider.setPreferredSize(new Dimension(1, 2));

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(row, BorderLayout.CENTER);
        wrap.add(divider, BorderLayout.SOUTH);
        return wrap;
    }

    private JComponent buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(22, 30, 28, 30));

        Doctor d = clinic.getDoctorById(appointment.getDoctorId());
        JLabel current = new JLabel("<html>Current schedule: <b>" + appointment.getDateTime().format(NICE_FMT)
                + "</b><br>Doctor: <b>" + (d == null ? "-" : d.getName()) + "</b>"
                + "<br>Reason: <b>" + appointment.getReason() + "</b></html>");
        current.setFont(Theme.font(Font.PLAIN, 16));
        current.setForeground(DARK);
        GridBagConstraints cc = new GridBagConstraints();
        cc.gridx = 0; cc.gridy = 0; cc.gridwidth = 2;
        cc.fill = GridBagConstraints.HORIZONTAL;
        cc.insets = new Insets(0, 4, 20, 0);
        form.add(current, cc);

        FormField dateRow = new FormField(Icons.of(Icons.Type.CALENDAR, 20, DARK), dateField, calendarButton);
        calendarButton.addActionListener(e -> new CalendarPopup(
                parseDate(dateField.getText()), LocalDate.now().plusDays(1),
                day -> dateField.setText(day.format(DATE_FMT)))
                .show(dateRow, 0, dateRow.getHeight() + 4));
        place(form, cell("New Date", dateRow), 0, 1);
        place(form, cell("New Time", new FormField(null, timePicker, null)), 1, 1);

        errorLabel.setFont(Theme.font(Font.PLAIN, 14));
        errorLabel.setForeground(Theme.ERROR);
        GridBagConstraints ec = new GridBagConstraints();
        ec.gridx = 0; ec.gridy = 2; ec.gridwidth = 2;
        ec.fill = GridBagConstraints.HORIZONTAL;
        ec.insets = new Insets(0, 4, 10, 0);
        form.add(errorLabel, ec);

        saveButton = PillButton.primary("Reschedule", Icons.of(Icons.Type.CALENDAR_CLOCK, 24, Color.WHITE));
        PillButton cancelButton = PillButton.secondary("Cancel");
        saveButton.addActionListener(e -> submit());
        cancelButton.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new GridLayout(1, 2, 40, 0));
        buttons.setOpaque(false);
        buttons.add(saveButton);
        buttons.add(cancelButton);
        GridBagConstraints bc = new GridBagConstraints();
        bc.gridx = 0; bc.gridy = 3; bc.gridwidth = 2;
        bc.fill = GridBagConstraints.HORIZONTAL;
        form.add(buttons, bc);
        return form;
    }

    private static JPanel cell(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        JLabel l = new JLabel("<html>" + label + "<font color='#E53935'>*</font></html>");
        l.setFont(Theme.font(Font.BOLD, 18));
        l.setForeground(DARK);
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private static void place(JPanel form, JComponent comp, int col, int row) {
        Dimension d = comp.getPreferredSize();
        comp.setPreferredSize(new Dimension(col == 0 ? 330 : 300, Math.max(d.height, 46)));
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = col;
        gc.gridy = row;
        gc.weightx = col == 0 ? 0.52 : 0.48;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTH;
        gc.insets = new Insets(0, col == 0 ? 0 : 14, 18, col == 0 ? 14 : 0);
        form.add(comp, gc);
    }

    private void limitDateField() {
        ((AbstractDocument) dateField.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (text == null) { super.replace(fb, offset, length, text, attrs); return; }
                StringBuilder ok = new StringBuilder();
                for (char c : text.toCharArray()) if ((c >= '0' && c <= '9') || c == '/') ok.append(c);
                int room = 10 - (fb.getDocument().getLength() - length);
                if (room <= 0 && ok.length() > 0) return;
                if (ok.length() > room) ok.setLength(room);
                super.replace(fb, offset, length, ok.toString(), attrs);
            }
        });
    }

    private static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text.trim(), DATE_PARSER);
        } catch (Exception e) {
            return null;
        }
    }

    private void submit() {
        errorLabel.setText(" ");

        LocalDate date = parseDate(dateField.getText());
        if (date == null) {
            errorLabel.setText("Please pick a new date (MM/DD/YYYY).");
            dateField.requestFocusInWindow();
            return;
        }
        if (!date.isAfter(LocalDate.now())) {
            errorLabel.setText("Appointments must be booked for a following day, not today.");
            dateField.requestFocusInWindow();
            return;
        }

        LocalDateTime when = LocalDateTime.of(date, timePicker.getTime());
        try {
            clinic.rescheduleAppointment(appointment.getAppointmentId(), when);
            JOptionPane.showMessageDialog(this,
                    "Appointment rescheduled.\n\nNew schedule: " + when.format(NICE_FMT),
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
            if (onDone != null) onDone.run();
        } catch (Exception ex) {
            errorLabel.setText(ex.getMessage());
        }
    }

    private static class Surface extends JPanel {
        Surface() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 34, 34);
            g2.dispose();
        }
    }
}