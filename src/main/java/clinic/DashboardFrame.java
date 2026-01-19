package clinic;

import javax.swing.*;
import java.awt.*;

public class DashboardFrame extends JFrame {

    // soft sky-blue theme
    private static final Color PRIMARY = new Color(135, 206, 235);      // sky blue
    private static final Color PRIMARY_DARK = new Color(100, 170, 210); // darker hover
    private static final Color BG_LIGHT = new Color(250, 252, 253);     // almost white

    public DashboardFrame(String username) {
        setTitle("Clinic Management - Dashboard (" + username + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(750, 430);
        setLocationRelativeTo(null);

        initComponents();
    }

    private JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(PRIMARY);
        btn.setForeground(Color.BLACK);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);

        // hover effect
        btn.getModel().addChangeListener(e -> {
            ButtonModel m = btn.getModel();
            if (m.isRollover()) {
                btn.setBackground(PRIMARY_DARK);
            } else {
                btn.setBackground(PRIMARY);
            }
        });

        return btn;
    }

    private void initComponents() {

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_LIGHT);

        // ---------------- HEADER ----------------
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PRIMARY);

        JLabel title = new JLabel("Clinic Dashboard", SwingConstants.CENTER);
        title.setForeground(Color.BLACK);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        header.setLayout(new BorderLayout());
        header.add(title, BorderLayout.CENTER);
        root.add(header, BorderLayout.NORTH);

        // ---------------- CENTER GRID ----------------
        JPanel center = new JPanel(new GridLayout(3, 3, 12, 12));
        center.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        center.setBackground(BG_LIGHT);

        JButton addDoctorBtn = createPrimaryButton("Add Doctor");
        JButton updateDoctorBtn = createPrimaryButton("Update Doctor");

        JButton addPatientBtn = createPrimaryButton("Add Patient");
        JButton updatePatientBtn = createPrimaryButton("Update Patient");

        JButton bookAppointmentBtn = createPrimaryButton("Book Appointment");
        JButton manageAppointmentsBtn = createPrimaryButton("Manage Appointments");

        JButton exitBtn = createPrimaryButton("Exit");

        // add buttons to grid (leave empty slots for spacing balance)
        center.add(addDoctorBtn);
        center.add(addPatientBtn);
        center.add(updatePatientBtn);
        center.add(bookAppointmentBtn);
        center.add(manageAppointmentsBtn);
        center.add(updateDoctorBtn);
        center.add(exitBtn);

        // ---------------- ACTIONS ----------------
        addDoctorBtn.addActionListener(e -> new AddDoctorFrame().setVisible(true));
        updateDoctorBtn.addActionListener(e -> new UpdateDoctorFrame().setVisible(true));

        addPatientBtn.addActionListener(e -> new AddPatientFrame().setVisible(true));
        updatePatientBtn.addActionListener(e -> new UpdatePatientFrame().setVisible(true));

        bookAppointmentBtn.addActionListener(e -> new BookAppointmentFrame().setVisible(true));
        manageAppointmentsBtn.addActionListener(e -> new ManageAppointmentsFrame().setVisible(true));

        exitBtn.addActionListener(e -> System.exit(0));

        root.add(center, BorderLayout.CENTER);

        setContentPane(root);
    }
}
