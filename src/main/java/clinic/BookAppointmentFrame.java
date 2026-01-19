package clinic;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.format.DateTimeParseException;

// Simple wrapper so JComboBox can show names but keep IDs
class PatientItem {
    int id;
    String name;

    PatientItem(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return name; 
    }
}

class DoctorItem {
    int id;
    String name;

    DoctorItem(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}

public class BookAppointmentFrame extends JFrame {

    private JComboBox<PatientItem> patientBox;
    private JComboBox<DoctorItem> doctorBox;
    private JTextField datetimeField;
    private JTextArea reasonArea;

    // Strict formatter: rejects invalid dates/times (e.g., 2021-02-30, 45:75)
    private static final DateTimeFormatter APPT_FMT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT);

    public BookAppointmentFrame() {
        setTitle("Book Appointment");
        setSize(450, 350);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initComponents();
        loadPatients();
        loadDoctors();
    }

    private void initComponents() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        patientBox = new JComboBox<>();
        doctorBox = new JComboBox<>();
        datetimeField = new JTextField(20); 
        reasonArea = new JTextArea(3, 20);
        JScrollPane reasonScroll = new JScrollPane(reasonArea);

        int row = 0;

        // patient
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Patient*:"), gbc);
        gbc.gridx = 1;
        formPanel.add(patientBox, gbc);
        row++;

        // doctor
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Doctor*:"), gbc);
        gbc.gridx = 1;
        formPanel.add(doctorBox, gbc);
        row++;

        // datetime
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Date/Time* (YYYY-MM-DD HH:MM):"), gbc);
        gbc.gridx = 1;
        formPanel.add(datetimeField, gbc);
        row++;

        // reason
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Reason:"), gbc);
        gbc.gridx = 1;
        formPanel.add(reasonScroll, gbc);
        row++;

        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Book");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        saveButton.addActionListener(e -> onSave());
        cancelButton.addActionListener(e -> dispose());

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(formPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadPatients() {
        String sql = "SELECT id, first_name, last_name FROM patients ORDER BY last_name, first_name";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("first_name") + " " + rs.getString("last_name");
                patientBox.addItem(new PatientItem(id, name));
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading patients: " + ex.getMessage());
        }
    }

    private void loadDoctors() {
        String sql = "SELECT id, name FROM doctors WHERE status = 'ACTIVE' ORDER BY name";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                doctorBox.addItem(new DoctorItem(id, name));
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading doctors: " + ex.getMessage());
        }
    }

    private void onSave() {
        PatientItem selectedPatient = (PatientItem) patientBox.getSelectedItem();
        DoctorItem selectedDoctor = (DoctorItem) doctorBox.getSelectedItem();
        String datetimeText = datetimeField.getText().trim();
        String reason = reasonArea.getText().trim();

        if (selectedPatient == null) {
            JOptionPane.showMessageDialog(this, "Select a patient.");
            return;
        }
        if (selectedDoctor == null) {
            JOptionPane.showMessageDialog(this, "Select a doctor.");
            return;
        }
        if (datetimeText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter appointment date and time.");
            return;
        }

        // Validate + normalize datetime
        LocalDateTime dt;
        try {
            dt = LocalDateTime.parse(datetimeText, APPT_FMT);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Invalid date/time. Use YYYY-MM-DD HH:MM (e.g., 2026-01-10 14:30).");
            return;
        }

        if (dt.isBefore(LocalDateTime.now())) {
            JOptionPane.showMessageDialog(this, "Appointment time cannot be in the past.");
            return;
        }

        // Store in normalized format that matches DB CHECK constraint
        String normalized = dt.format(APPT_FMT);

        String sql = "INSERT INTO appointments (patient_id, doctor_id, appointment_datetime, reason, status) " +
                     "VALUES (?, ?, ?, ?, 'BOOKED')";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, selectedPatient.id);
            ps.setInt(2, selectedDoctor.id);
            ps.setString(3, normalized);
            ps.setString(4, reason.isEmpty() ? null : reason);

            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Appointment booked.");
            dispose();

        } catch (Exception ex) {
            String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();

            // Friendly message for unique index double-booking
            if (msg.contains("uq_doctor_time_booked") || msg.contains("unique") || msg.contains("constraint")) {
                JOptionPane.showMessageDialog(this,
                        "That doctor already has an appointment at this time. Choose a different time.");
                return;
            }

            JOptionPane.showMessageDialog(this, "Error booking appointment: " + ex.getMessage());
        }
    }
}
