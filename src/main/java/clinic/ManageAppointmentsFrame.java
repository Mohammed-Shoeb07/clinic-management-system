package clinic;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class ManageAppointmentsFrame extends JFrame {

    private JTextField dateField;
    private JTable table;
    private DefaultTableModel tableModel;
    private JComboBox<String> statusBox;

    public ManageAppointmentsFrame() {
        setTitle("View / Manage Appointments");
        setSize(700, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initComponents();
        loadAppointments(null); // load all initially
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // Top filter panel
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Date (YYYY-MM-DD, empty = all):"));
        dateField = new JTextField(10);
        topPanel.add(dateField);
        JButton loadButton = new JButton("Load");
        topPanel.add(loadButton);

        loadButton.addActionListener(e -> {
            String date = dateField.getText().trim();
            if (date.isEmpty()) {
                loadAppointments(null);
                return;
            }

            // Validate date filter (prevents useless queries + user confusion)
            try {
                LocalDate.parse(date); // strict ISO yyyy-MM-dd
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(this, "Invalid date. Use YYYY-MM-DD (e.g., 2026-01-10).");
                return;
            }

            loadAppointments(date);
        });

        add(topPanel, BorderLayout.NORTH);

        // Table
        tableModel = new DefaultTableModel(
                new Object[] { "ID", "Date/Time", "Patient", "Doctor", "Reason", "Status" },
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // hide ID column visually but keep in model
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setWidth(0);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // Bottom panel: status update
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.add(new JLabel("New status:"));
        statusBox = new JComboBox<>(new String[] { "BOOKED", "COMPLETED", "CANCELLED" });
        bottomPanel.add(statusBox);
        JButton updateStatusButton = new JButton("Update Status");
        bottomPanel.add(updateStatusButton);

        updateStatusButton.addActionListener(e -> onUpdateStatus());

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void loadAppointments(String dateFilter) {
        tableModel.setRowCount(0);

        String baseSql =
                "SELECT a.id, a.appointment_datetime, " +
                "       p.first_name, p.last_name, " +
                "       d.name AS doctor_name, " +
                "       a.reason, a.status " +
                "FROM appointments a " +
                "JOIN patients p ON a.patient_id = p.id " +
                "JOIN doctors d ON a.doctor_id = d.id ";

        String orderBy = " ORDER BY a.appointment_datetime";

        boolean hasFilter = dateFilter != null && !dateFilter.isEmpty();
        String sql = hasFilter
                ? baseSql + "WHERE substr(a.appointment_datetime, 1, 10) = ?" + orderBy
                : baseSql + orderBy;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (hasFilter) {
                ps.setString(1, dateFilter);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String datetime = rs.getString("appointment_datetime");
                    String patientName = rs.getString("first_name") + " " + rs.getString("last_name");
                    String doctorName = rs.getString("doctor_name");
                    String reason = rs.getString("reason");
                    String status = rs.getString("status");

                    tableModel.addRow(new Object[] {
                            id,
                            datetime,
                            patientName,
                            doctorName,
                            reason,
                            status
                    });
                }
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading appointments: " + ex.getMessage());
        }
    }

    private void onUpdateStatus() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Select an appointment first.");
            return;
        }

        int modelRow = table.convertRowIndexToModel(viewRow);

        Object idObj = tableModel.getValueAt(modelRow, 0);
        if (!(idObj instanceof Integer)) {
            JOptionPane.showMessageDialog(this, "Invalid appointment selection.");
            return;
        }

        int appointmentId = (Integer) idObj;
        String newStatus = (String) statusBox.getSelectedItem();

        // Avoid pointless updates
        Object currentStatusObj = tableModel.getValueAt(modelRow, 5);
        String currentStatus = currentStatusObj == null ? "" : currentStatusObj.toString();
        if (newStatus != null && newStatus.equals(currentStatus)) {
            JOptionPane.showMessageDialog(this, "This appointment is already " + newStatus + ".");
            return;
        }

        // Simple confirmation when cancelling
        if ("CANCELLED".equals(newStatus)) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Cancel this appointment?",
                    "Confirm",
                    JOptionPane.YES_NO_OPTION
            );
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
        }

        String sql = "UPDATE appointments SET status = ? WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newStatus);
            ps.setInt(2, appointmentId);

            int updated = ps.executeUpdate();
            if (updated == 0) {
                JOptionPane.showMessageDialog(this, "Appointment not found (it may have been deleted).");
                return;
            }

            tableModel.setValueAt(newStatus, modelRow, 5);
            JOptionPane.showMessageDialog(this, "Status updated.");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error updating status: " + ex.getMessage());
        }
    }
}
