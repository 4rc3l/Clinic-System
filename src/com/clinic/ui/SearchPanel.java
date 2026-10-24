package com.clinic.ui;

import com.clinic.model.Appointment;
import com.clinic.model.Doctor;
import com.clinic.model.Patient;
import com.clinic.service.ClinicManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * "Search Patient / Doctor" page.
 *
 * - Type a name, ID or specialization; results update as you type.
 * - The Patients / Doctors chips choose which group is listed first.
 *   (Set STRICT_FILTER to true to show ONLY the chosen group instead.)
 * - Clicking a result opens it in the Records page.
 */
public class SearchPanel extends JPanel {

    private static final boolean STRICT_FILTER = false;

    private enum Scope { PATIENTS, DOCTORS }

    private static final Color CARD_FILL = new Color(0xE4E4E4);
    private static final Color CARD_HOVER = new Color(0xF0F0F0);

    private final ClinicManager clinic;
    private final Consumer<Patient> onPatient;
    private final Consumer<Doctor> onDoctor;

    private Scope scope = Scope.PATIENTS;

    private final JTextField searchField = FormField.textField("Search by name, ID, or specialization...");
    private final UiKit.ToggleChip patientsChip = new UiKit.ToggleChip("Patients");
    private final UiKit.ToggleChip doctorsChip = new UiKit.ToggleChip("Doctors");
    private final JPanel tryRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final JLabel countLabel = new JLabel(" ");
    private final JPanel listPanel = new JPanel();
    private final JScrollPane scroll;

    public SearchPanel(ClinicManager clinic, Consumer<Patient> onPatient, Consumer<Doctor> onDoctor) {
        this.clinic = clinic;
        this.onPatient = onPatient;
        this.onDoctor = onDoctor;

        setLayout(new BorderLayout(0, 18));
        setBackground(Theme.CONTENT_BG);
        setBorder(new EmptyBorder(22, 24, 22, 24));

        // ---- top: title, search bar, chips, suggestions ----
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Search Patient / Doctor");
        title.setFont(Theme.font(Font.BOLD, 22));
        title.setForeground(Theme.TEXT_DARK);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JComponent bar = buildSearchBar();

        JPanel chips = new JPanel();
        chips.setOpaque(false);
        chips.setLayout(new BoxLayout(chips, BoxLayout.X_AXIS));
        chips.setAlignmentX(Component.LEFT_ALIGNMENT);
        chips.add(patientsChip);
        chips.add(Box.createHorizontalStrut(14));
        chips.add(doctorsChip);
        chips.add(Box.createHorizontalGlue());
        patientsChip.addActionListener(e -> setScope(Scope.PATIENTS));
        doctorsChip.addActionListener(e -> setScope(Scope.DOCTORS));

        tryRow.setOpaque(false);
        tryRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        tryRow.setBorder(new EmptyBorder(0, 6, 0, 0));

        top.add(title);
        top.add(Box.createVerticalStrut(14));
        top.add(bar);
        top.add(Box.createVerticalStrut(16));
        top.add(chips);
        top.add(Box.createVerticalStrut(10));
        top.add(tryRow);

        // ---- results card ----
        UiKit.Card results = new UiKit.Card(20, Color.WHITE, null);
        results.setLayout(new BorderLayout(0, 12));
        results.setBorder(new EmptyBorder(18, 22, 20, 22));

        JLabel resultsTitle = new JLabel("Search Results");
        resultsTitle.setFont(Theme.font(Font.BOLD, 22));
        resultsTitle.setForeground(Theme.TEXT_DARK);
        countLabel.setFont(Theme.font(Font.PLAIN, 14));
        countLabel.setForeground(Theme.PRIMARY);

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(resultsTitle, BorderLayout.WEST);
        head.add(countLabel, BorderLayout.EAST);

        UiKit.Card inner = new UiKit.Card(18, new Color(0xDADADA), null);
        inner.setLayout(new BorderLayout());
        inner.setBorder(new EmptyBorder(12, 12, 12, 12));

        listPanel.setOpaque(false);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        VScrollPanel holder = new VScrollPanel(new BorderLayout());
        holder.add(listPanel, BorderLayout.NORTH);

        scroll = new JScrollPane(holder);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        inner.add(scroll, BorderLayout.CENTER);

        results.add(head, BorderLayout.NORTH);
        results.add(inner, BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(results, BorderLayout.CENTER);

        updateChips();
        refresh();
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /** Call when the page is shown so suggestions and results reflect the latest records. */
    public void refresh() {
        buildSuggestions();
        runSearch();
    }

    public void setQuery(String text) {
        searchField.setText(text);
        searchField.requestFocusInWindow();
    }

    // ------------------------------------------------------------------
    // Top section
    // ------------------------------------------------------------------

    private JComponent buildSearchBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.setColor(Theme.PRIMARY);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 16, 16);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(2, 2, 2, 2));
        bar.setPreferredSize(new Dimension(100, 58));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);

        searchField.setFont(Theme.font(Font.PLAIN, 17));
        searchField.setBorder(new EmptyBorder(0, 16, 0, 10));
        searchField.addActionListener(e -> runSearch());
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { runSearch(); }
            @Override public void removeUpdate(DocumentEvent e)  { runSearch(); }
            @Override public void changedUpdate(DocumentEvent e) { runSearch(); }
        });

        JButton go = new JButton(Icons.of(Icons.Type.SEARCH, 26, Color.WHITE)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                ButtonModel m = getModel();
                g2.setColor(m.isPressed() ? Theme.PRIMARY_PRESSED : m.isRollover() ? Theme.PRIMARY_HOVER : Theme.PRIMARY);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 14, 14));
                g2.fillRect(0, 0, 14, getHeight()); // square left corners
                g2.dispose();
                super.paintComponent(g);
            }
        };
        go.setPreferredSize(new Dimension(64, 0));
        go.setContentAreaFilled(false);
        go.setBorderPainted(false);
        go.setFocusPainted(false);
        go.setRolloverEnabled(true);
        go.setCursor(Theme.hand());
        go.setToolTipText("Search");
        go.addActionListener(e -> runSearch());

        bar.add(searchField, BorderLayout.CENTER);
        bar.add(go, BorderLayout.EAST);
        return bar;
    }

    private void setScope(Scope newScope) {
        scope = newScope;
        updateChips();
        runSearch();
    }

    private void updateChips() {
        patientsChip.setActive(scope == Scope.PATIENTS);
        doctorsChip.setActive(scope == Scope.DOCTORS);
    }

    /** "Try: "Baterbonia" or "Dr. Dy"" built from the real records. */
    private void buildSuggestions() {
        tryRow.removeAll();
        tryRow.add(plainText("Try: "));

        String patientTip = null;
        String doctorTip = null;
        if (!clinic.getPatients().isEmpty()) {
            String[] words = clinic.getPatients().get(0).getName().trim().split("\\s+");
            patientTip = words[words.length - 1];
        }
        if (!clinic.getDoctors().isEmpty()) {
            doctorTip = clinic.getDoctors().get(0).getName();
        }

        if (patientTip != null) tryRow.add(link(patientTip));
        if (patientTip != null && doctorTip != null) tryRow.add(plainText(" or "));
        if (doctorTip != null) tryRow.add(link(doctorTip));
        if (patientTip == null && doctorTip == null) tryRow.add(plainText("a patient name, ID, or specialization"));

        tryRow.revalidate();
        tryRow.repaint();
    }

    private JLabel plainText(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.font(Font.PLAIN, 15));
        l.setForeground(Theme.PRIMARY);
        return l;
    }

    /** A clickable suggestion that fills the search box. */
    private JLabel link(String text) {
        JLabel l = plainText("\u201C" + text + "\u201D");
        l.setCursor(Theme.hand());
        Font normal = l.getFont();
        Map<TextAttribute, Object> attrs = new HashMap<>();
        attrs.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        Font underlined = normal.deriveFont(attrs);
        l.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { l.setFont(underlined); }
            @Override public void mouseExited(MouseEvent e)  { l.setFont(normal); }
            @Override public void mouseClicked(MouseEvent e) { setQuery(text); }
        });
        return l;
    }

    // ------------------------------------------------------------------
    // Searching
    // ------------------------------------------------------------------

    private void runSearch() {
        String raw = searchField.getText().trim().replaceAll("\\s+", " ");
        String q = raw.toLowerCase();

        List<Patient> patientHits = new ArrayList<>();
        List<Doctor> doctorHits = new ArrayList<>();
        if (!q.isEmpty()) {
            for (Patient p : clinic.getPatients()) if (matches(p, q)) patientHits.add(p);
            for (Doctor d : clinic.getDoctors()) if (matches(d, q)) doctorHits.add(d);
            patientHits.sort(Comparator.comparing(p -> p.getName().toLowerCase()));
            doctorHits.sort(Comparator.comparing(d -> d.getName().toLowerCase()));
        }
        showResults(raw, patientHits, doctorHits);
    }

    private boolean matches(Patient p, String q) {
        if (p.getName().toLowerCase().contains(q)
                || p.getPatientId().toLowerCase().contains(q)
                || p.getContactNumber().contains(q)) {
            return true;
        }
        Doctor assigned = clinic.getAssignedDoctor(p.getPatientId());
        return assigned != null && assigned.getSpecialization().toLowerCase().contains(q);
    }

    private boolean matches(Doctor d, String q) {
        return d.getName().toLowerCase().contains(q)
                || d.getDoctorId().toLowerCase().contains(q)
                || d.getSpecialization().toLowerCase().contains(q);
    }

    // ------------------------------------------------------------------
    // Rendering results
    // ------------------------------------------------------------------

    private void showResults(String query, List<Patient> patients, List<Doctor> doctors) {
        listPanel.removeAll();

        if (query.isEmpty()) {
            countLabel.setText(" ");
            listPanel.add(message("Type a name, ID, or specialization to search patients and doctors."));
        } else {
            countLabel.setText("Found " + patients.size() + (patients.size() == 1 ? " patient" : " patients")
                    + " and " + doctors.size() + (doctors.size() == 1 ? " doctor" : " doctors"));

            boolean showPatients = !STRICT_FILTER || scope == Scope.PATIENTS;
            boolean showDoctors = !STRICT_FILTER || scope == Scope.DOCTORS;
            boolean any = (showPatients && !patients.isEmpty()) || (showDoctors && !doctors.isEmpty());

            if (!any) {
                listPanel.add(message("No results for \u201C" + query + "\u201D."));
            } else if (scope == Scope.PATIENTS) {
                if (showPatients) addPatientGroup(patients);
                if (showDoctors) addDoctorGroup(doctors);
            } else {
                if (showDoctors) addDoctorGroup(doctors);
                if (showPatients) addPatientGroup(patients);
            }
        }

        listPanel.revalidate();
        listPanel.repaint();
        SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
    }

    private JComponent message(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.font(Font.PLAIN, 16));
        l.setForeground(Theme.TEXT_MUTED);
        l.setBorder(new EmptyBorder(14, 8, 14, 8));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private void addGroupTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.font(Font.BOLD, 17));
        l.setForeground(Theme.TEXT_DARK);
        l.setBorder(new EmptyBorder(6, 2, 8, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        listPanel.add(l);
    }

    private void addPatientGroup(List<Patient> patients) {
        if (patients.isEmpty()) return;
        addGroupTitle("Patients");
        for (Patient p : patients) {
            listPanel.add(patientCard(p));
            listPanel.add(Box.createVerticalStrut(10));
        }
    }

    private void addDoctorGroup(List<Doctor> doctors) {
        if (doctors.isEmpty()) return;
        addGroupTitle(doctors.size() == 1 ? "Doctor" : "Doctors");
        for (Doctor d : doctors) {
            listPanel.add(doctorCard(d));
            listPanel.add(Box.createVerticalStrut(10));
        }
    }

    private JComponent patientCard(Patient p) {
        return UiKit.patientResultCard(clinic, p, () -> onPatient.accept(p));
    }

    private JComponent doctorCard(Doctor d) {
        return UiKit.doctorResultCard(clinic, d, () -> onDoctor.accept(d));
    }
}