package clinic;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

// Wrapper for doctor combo box
class DoctorComboItem {
    int id;
    String name;

    DoctorComboItem(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return name; // what shows in dropdown
    }
}

public class UpdateDoctorFrame extends JFrame {

    private JComboBox<DoctorComboItem> doctorBox;
    private JTextField nameField;
    private JTextField specializationField;
    private JTextField phoneField;
    private JTextField emailField;
    private JComboBox<String> statusBox;

    public UpdateDoctorFrame() {
        setTitle("Update Doctor Details");
        setSize(450, 350);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initComponents();
        loadDoctors();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Top: doctor selector
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        doctorBox = new JComboBox<>();
        topPanel.add(new JLabel("Select doctor:"));
        topPanel.add(doctorBox);

        doctorBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                DoctorComboItem item = (DoctorComboItem) doctorBox.getSelectedItem();
                if (item != null) {
                    loadDoctorDetails(item.id);
                }
            }
        });

        // Center: form
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

        // specialization (required by DB)
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Specialization*:"), gbc);
        gbc.gridx = 1;
        formPanel.add(specializationField, gbc);
        row++;

        // phone (required by DB)
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

    private void loadDoctors() {
        doctorBox.removeAllItems();

        String sql = "SELECT id, name FROM doctors ORDER BY name";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                doctorBox.addItem(new DoctorComboItem(id, name));
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading doctors: " + ex.getMessage());
        }

        if (doctorBox.getItemCount() > 0) {
            doctorBox.setSelectedIndex(0);
            DoctorComboItem item = (DoctorComboItem) doctorBox.getSelectedItem();
            if (item != null) loadDoctorDetails(item.id);
        }
    }

    private void loadDoctorDetails(int doctorId) {
        String sql = "SELECT name, specialization, phone, email, status FROM doctors WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, doctorId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    nameField.setText(rs.getString("name"));
                    specializationField.setText(rs.getString("specialization"));
                    phoneField.setText(rs.getString("phone"));
                    emailField.setText(rs.getString("email"));
                    String status = rs.getString("status");
                    statusBox.setSelectedItem(status != null ? status : "ACTIVE");
                }
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading doctor details: " + ex.getMessage());
        }
    }

    private void onSave() {
        DoctorComboItem item = (DoctorComboItem) doctorBox.getSelectedItem();
        if (item == null) {
            JOptionPane.showMessageDialog(this, "No doctor selected.");
            return;
        }

        String name = nameField.getText().trim();
        String specialization = specializationField.getText().trim();

        // normalize phone to digits only
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

        String sql = "UPDATE doctors SET name = ?, specialization = ?, phone = ?, email = ?, status = ? WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, specialization);
            ps.setString(3, phoneDigits);
            ps.setString(4, email.isEmpty() ? null : email);
            ps.setString(5, status);
            ps.setInt(6, item.id);

            int updated = ps.executeUpdate();
            if (updated == 0) {
                JOptionPane.showMessageDialog(this, "Doctor not found (it may have been deleted).");
                return;
            }

            // keep combo display in sync if name changed
            item.name = name;
            doctorBox.repaint();

            JOptionPane.showMessageDialog(this, "Doctor details updated.");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error updating doctor: " + ex.getMessage());
        }
    }
}
