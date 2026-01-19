package clinic;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

// Reuse a small wrapper for patients in combo box
class PatientComboItem {
    int id;
    String name;

    PatientComboItem(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return name; // what shows in dropdown
    }
}

public class UpdatePatientFrame extends JFrame {

    private JComboBox<PatientComboItem> patientBox;
    private JTextField firstNameField;
    private JTextField lastNameField;
    private JTextField dobField;
    private JComboBox<String> genderBox;
    private JTextField phoneField;
    private JTextField emailField;
    private JTextArea addressArea;

    public UpdatePatientFrame() {
        setTitle("Update Patient Details");
        setSize(450, 450);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initComponents();
        loadPatients();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Top: patient selector
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        patientBox = new JComboBox<>();
        topPanel.add(new JLabel("Select patient:"));
        topPanel.add(patientBox);

        patientBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                PatientComboItem item = (PatientComboItem) patientBox.getSelectedItem();
                if (item != null) loadPatientDetails(item.id);
            }
        });

        // Center: form
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        firstNameField = new JTextField(20);
        lastNameField = new JTextField(20);
        dobField = new JTextField(10); // YYYY-MM-DD
        genderBox = new JComboBox<>(new String[] { "M", "F", "O", "N/A" });
        phoneField = new JTextField(15);
        emailField = new JTextField(20);
        addressArea = new JTextArea(3, 20);
        JScrollPane addressScroll = new JScrollPane(addressArea);

        int row = 0;

        // first name
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("First Name*:"), gbc);
        gbc.gridx = 1;
        formPanel.add(firstNameField, gbc);
        row++;

        // last name
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Last Name*:"), gbc);
        gbc.gridx = 1;
        formPanel.add(lastNameField, gbc);
        row++;

        // dob (required)
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("DOB (YYYY-MM-DD)*:"), gbc);
        gbc.gridx = 1;
        formPanel.add(dobField, gbc);
        row++;

        // gender (required)
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Gender*:"), gbc);
        gbc.gridx = 1;
        formPanel.add(genderBox, gbc);
        row++;

        // phone (required)
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Phone (10 digits)*:"), gbc);
        gbc.gridx = 1;
        formPanel.add(phoneField, gbc);
        row++;

        // email
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        formPanel.add(emailField, gbc);
        row++;

        // address
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Address:"), gbc);
        gbc.gridx = 1;
        formPanel.add(addressScroll, gbc);
        row++;

        // Bottom: buttons
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Save Changes");
        JButton closeButton = new JButton("Close");
        buttonPanel.add(saveButton);
        buttonPanel.add(closeButton);

        saveButton.addActionListener(e -> onSave());
        closeButton.addActionListener(e -> dispose());

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private void loadPatients() {
        patientBox.removeAllItems();

        String sql = "SELECT id, first_name, last_name FROM patients ORDER BY last_name, first_name";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("first_name") + " " + rs.getString("last_name");
                patientBox.addItem(new PatientComboItem(id, name));
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading patients: " + ex.getMessage());
        }

        if (patientBox.getItemCount() > 0) {
            patientBox.setSelectedIndex(0);
            PatientComboItem item = (PatientComboItem) patientBox.getSelectedItem();
            if (item != null) loadPatientDetails(item.id);
        }
    }

    private void loadPatientDetails(int patientId) {
        String sql = "SELECT first_name, last_name, dob, gender, phone, email, address FROM patients WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, patientId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    firstNameField.setText(rs.getString("first_name"));
                    lastNameField.setText(rs.getString("last_name"));
                    dobField.setText(rs.getString("dob"));

                    String g = rs.getString("gender");
                    if (g == null || g.isBlank()) g = "N/A";
                    genderBox.setSelectedItem(g);

                    phoneField.setText(rs.getString("phone"));
                    emailField.setText(rs.getString("email"));
                    addressArea.setText(rs.getString("address"));
                }
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading patient details: " + ex.getMessage());
        }
    }

    private void onSave() {
        PatientComboItem item = (PatientComboItem) patientBox.getSelectedItem();
        if (item == null) {
            JOptionPane.showMessageDialog(this, "No patient selected.");
            return;
        }

        String firstName = firstNameField.getText().trim();
        String lastName  = lastNameField.getText().trim();
        String dobText   = dobField.getText().trim();
        String gender    = (String) genderBox.getSelectedItem();

        // normalize phone to digits only
        String phoneDigits = phoneField.getText().replaceAll("\\D", "").trim();

        String email   = emailField.getText().trim();
        String address = addressArea.getText().trim();

        if (firstName.isEmpty() || lastName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "First name and last name are required.");
            return;
        }

        // DOB required + valid + not future + not insane old
        LocalDate dob;
        try {
            dob = LocalDate.parse(dobText); // YYYY-MM-DD
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "DOB must be in YYYY-MM-DD format (e.g., 2000-01-31).");
            return;
        }
        if (dob.isAfter(LocalDate.now())) {
            JOptionPane.showMessageDialog(this, "DOB cannot be in the future.");
            return;
        }
        if (dob.isBefore(LocalDate.of(1900, 1, 1))) {
            JOptionPane.showMessageDialog(this, "DOB must be 1900-01-01 or later.");
            return;
        }

        // phone required + exactly 10 digits
        if (phoneDigits.length() != 10) {
            JOptionPane.showMessageDialog(this, "Phone must be exactly 10 digits.");
            return;
        }

        // email optional, validate if provided
        if (!email.isEmpty()) {
            int at = email.indexOf('@');
            int dot = email.lastIndexOf('.');
            if (at <= 0 || dot <= at + 1 || dot == email.length() - 1) {
                JOptionPane.showMessageDialog(this, "Enter a valid email or leave it blank.");
                return;
            }
        }

        String sql = "UPDATE patients SET first_name = ?, last_name = ?, dob = ?, gender = ?, " +
                     "phone = ?, email = ?, address = ? WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, dob.toString());
            ps.setString(4, gender);
            ps.setString(5, phoneDigits);
            ps.setString(6, email.isEmpty() ? null : email);
            ps.setString(7, address.isEmpty() ? null : address);
            ps.setInt(8, item.id);

            int updated = ps.executeUpdate();
            if (updated == 0) {
                JOptionPane.showMessageDialog(this, "Patient not found (it may have been deleted).");
                return;
            }

            // keep combo display in sync if name changed
            item.name = firstName + " " + lastName;
            patientBox.repaint();

            JOptionPane.showMessageDialog(this, "Patient details updated.");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error updating patient: " + ex.getMessage());
        }
    }
}
