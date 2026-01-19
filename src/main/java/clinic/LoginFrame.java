package clinic;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;

public class LoginFrame extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;

    // color palette (soft, sky blue)
    private static final Color PRIMARY = new Color(135, 206, 235);      // sky blue
    private static final Color BG_LIGHT = new Color(250, 252, 253);     // almost white

    public LoginFrame() {
        setTitle("Clinic Management - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 260);
        setLocationRelativeTo(null); // center on screen

        initComponents();
    }

    private void initComponents() {
        // Root panel
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_LIGHT);

        // Header
        JPanel header = new JPanel();
        header.setBackground(PRIMARY);
        JLabel title = new JLabel("Clinic Management System", SwingConstants.CENTER);
        title.setForeground(Color.BLACK);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        title.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        header.setLayout(new BorderLayout());
        header.add(title, BorderLayout.CENTER);

        // Center form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(BG_LIGHT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel userLabel = new JLabel("Username:");
        JLabel passLabel = new JLabel("Password:");
        usernameField = new JTextField(15);
        passwordField = new JPasswordField(15);

        JButton loginButton = new JButton("Login");
        loginButton.setBackground(PRIMARY);
        loginButton.setForeground(Color.BLACK);
        loginButton.setFocusPainted(false);
        loginButton.setBorderPainted(false);
        loginButton.setOpaque(true);


        // Row 0 - username
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0;
        formPanel.add(userLabel, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(usernameField, gbc);

        // Row 1 - password
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.weightx = 0;
        formPanel.add(passLabel, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(passwordField, gbc);

        // Row 2 - button
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(loginButton, gbc);

        loginButton.addActionListener(this::onLogin);

        root.add(header, BorderLayout.NORTH);
        root.add(formPanel, BorderLayout.CENTER);

        setContentPane(root);
    }

    private void onLogin(ActionEvent e) {
        String username = usernameField.getText().trim();

        char[] pwChars = passwordField.getPassword();
        String password = new String(pwChars); // do NOT trim passwords
        Arrays.fill(pwChars, '\0');

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter username and password.");
            return;
        }

        if (checkCredentials(username, password)) {
            SwingUtilities.invokeLater(() -> new DashboardFrame(username).setVisible(true));
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Invalid username or password.");
        }
    }

    private boolean checkCredentials(String username, String password) {
        String sql = "SELECT password FROM users WHERE username = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;

                String storedHash = rs.getString("password");
                String inputHash = PasswordUtil.hashPassword(password);

                return storedHash != null && storedHash.equals(inputHash);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
            return false;
        }
    }
}
