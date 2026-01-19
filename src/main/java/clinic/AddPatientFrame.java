package clinic;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class AddPatientFrame extends JFrame {

    private JTextField firstNameField;
    private JTextField lastNameField;
    private JTextField dobField;
    private JComboBox<String> genderBox;
    private JTextField phoneField;
    private JTextField emailField;
    private JTextArea addressArea;

    public AddPatientFrame() {
        setTitle("Add New Patient");
        setSize(420, 420);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initComponents();
    }

    private void initComponents() {
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

        // dob
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("DOB (YYYY-MM-DD)*:"), gbc);
        gbc.gridx = 1;
        formPanel.add(dobField, gbc);
        row++;

        // gender
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Gender*:"), gbc);
        gbc.gridx = 1;
        formPanel.add(genderBox, gbc);
        row++;

        // phone
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

        // buttons
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        saveButton.addActionListener(e -> onSave());
        cancelButton.addActionListener(e -> dispose());

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(formPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }

    private void onSave() {
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

        LocalDate dob;
        try {
            dob = LocalDate.parse(dobText); // expects YYYY-MM-DD
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

        // email optional, but validate if provided
        if (!email.isEmpty()) {
            int at = email.indexOf('@');
            int dot = email.lastIndexOf('.');
            if (at <= 0 || dot <= at + 1 || dot == email.length() - 1) {
                JOptionPane.showMessageDialog(this, "Enter a valid email or leave it blank.");
                return;
            }
        }

        String sql = "INSERT INTO patients (first_name, last_name, gender, dob, phone, email, address) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, gender);
            ps.setString(4, dob.toString());       // normalized YYYY-MM-DD
            ps.setString(5, phoneDigits);          // digits only
            ps.setString(6, email.isEmpty() ? null : email);
            ps.setString(7, address.isEmpty() ? null : address);

            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Patient saved.");
            dispose();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error saving patient: " + ex.getMessage());
        }
    }
}
