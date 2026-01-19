package clinic;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class AddDoctorFrame extends JFrame {

    private JTextField nameField;
    private JTextField specializationField;
    private JTextField phoneField;
    private JTextField emailField;
    private JComboBox<String> statusBox;

    public AddDoctorFrame() {
        setTitle("Add New Doctor");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initComponents();
    }

    private void initComponents() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        nameField = new JTextField(20);
        specializationField = new JTextField(20);
        phoneField = new JTextField(15);
        emailField = new JTextField(20);
        statusBox = new JComboBox<>(new String[] { "ACTIVE", "INACTIVE" });

        int row = 0;

        // name
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Name*:"), gbc);
        gbc.gridx = 1;
        formPanel.add(nameField, gbc);
        row++;

        // specialization
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Specialization*:"), gbc);
        gbc.gridx = 1;
        formPanel.add(specializationField, gbc);
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

        // status
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Status:"), gbc);
        gbc.gridx = 1;
        formPanel.add(statusBox, gbc);
        row++;

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
        String name = nameField.getText().trim();
        String specialization = specializationField.getText().trim();

        // normalize phone: keep digits only
        String phoneDigits = phoneField.getText().replaceAll("\\D", "").trim();

        String email = emailField.getText().trim();
        String status = (String) statusBox.getSelectedItem();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name is required.");
            return;
        }

        if (specialization.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Specialization is required.");
            return;
        }

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

        String sql = "INSERT INTO doctors (name, specialization, phone, email, status) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, specialization);
            ps.setString(3, phoneDigits);
            ps.setString(4, email.isEmpty() ? null : email);
            ps.setString(5, status);

            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Doctor saved.");
            dispose();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error saving doctor: " + ex.getMessage());
        }
    }
}
