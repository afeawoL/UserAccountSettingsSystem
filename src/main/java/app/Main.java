package app;

/*
 * User Account & Settings Management System
 *
 * Java Swing desktop application using SQLite.
 *
 * Features:
 * - User registration
 * - User login
 * - Password hashing
 * - User dashboard
 * - Profile management
 * - Account settings management
 * - Admin user management
 * - Admin add user function
 * - Admin delete user function
 */

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.security.MessageDigest;
import java.sql.*;

public class Main extends JFrame {

    private static final String DB_URL = "jdbc:sqlite:database/user_settings.db";

    private int currentUserId = -1;
    private String currentUsername = "";
    private String currentRole = "";

    public Main() {
        setTitle("User Account & Settings Management System");
        setSize(900, 580);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        showLoginPage();
    }

    public static void main(String[] args) {
        createTables();
        seedAdminAccount();

        SwingUtilities.invokeLater(() -> {
            Main app = new Main();
            app.setVisible(true);
        });
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private static void createTables() {
        String usersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    user_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    first_name TEXT NOT NULL,
                    last_name TEXT NOT NULL,
                    email TEXT UNIQUE NOT NULL,
                    username TEXT UNIQUE NOT NULL,
                    password TEXT NOT NULL,
                    role TEXT DEFAULT 'user',
                    status TEXT DEFAULT 'active'
                );
                """;

        String settingsTable = """
                CREATE TABLE IF NOT EXISTS settings (
                    setting_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    theme TEXT DEFAULT 'light',
                    notifications_enabled INTEGER DEFAULT 1,
                    language TEXT DEFAULT 'English',
                    privacy_mode INTEGER DEFAULT 0,
                    FOREIGN KEY (user_id) REFERENCES users(user_id)
                );
                """;

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {

            stmt.execute(usersTable);
            stmt.execute(settingsTable);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database setup error: " + e.getMessage());
        }
    }

    private static void seedAdminAccount() {
        String checkSql = "SELECT COUNT(*) FROM users WHERE username = 'admin'";

        String insertUserSql = """
                INSERT INTO users
                (first_name, last_name, email, username, password, role, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        String insertSettingsSql = """
                INSERT INTO settings
                (user_id, theme, notifications_enabled, language, privacy_mode)
                VALUES (?, 'light', 1, 'English', 0)
                """;

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {

            if (rs.next() && rs.getInt(1) == 0) {
                try (PreparedStatement ps = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, "System");
                    ps.setString(2, "Administrator");
                    ps.setString(3, "admin@example.com");
                    ps.setString(4, "admin");
                    ps.setString(5, hashPassword("admin123"));
                    ps.setString(6, "admin");
                    ps.setString(7, "active");

                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            int adminId = keys.getInt(1);

                            try (PreparedStatement settingsPs = conn.prepareStatement(insertSettingsSql)) {
                                settingsPs.setInt(1, adminId);
                                settingsPs.executeUpdate();
                            }
                        }
                    }
                }
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Admin seed error: " + e.getMessage());
        }
    }

    private void setPage(JPanel panel) {
        setContentPane(panel);
        revalidate();
        repaint();
    }

    private JPanel pageWrapper(String title) {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel heading = new JLabel(title, SwingConstants.CENTER);
        heading.setFont(new Font("Arial", Font.BOLD, 24));
        heading.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        panel.add(heading, BorderLayout.NORTH);

        return panel;
    }

    private void showLoginPage() {
        JPanel panel = pageWrapper("Login");

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField usernameField = new JTextField(20);
        JPasswordField passwordField = new JPasswordField(20);

        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Create Account");

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        form.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        form.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        form.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        form.add(loginButton, gbc);

        gbc.gridx = 1;
        form.add(registerButton, gbc);

        panel.add(form, BorderLayout.CENTER);

        loginButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                message("Please enter username and password.");
                return;
            }

            login(username, password);
        });

        registerButton.addActionListener(e -> showRegisterPage());

        setPage(panel);
    }

    private void login(String username, String password) {
        String sql = """
                SELECT user_id, username, role, status
                FROM users
                WHERE username = ? AND password = ?
                """;

        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, hashPassword(password));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString("status");

                    if (!status.equalsIgnoreCase("active")) {
                        message("This account is inactive. Contact an administrator.");
                        return;
                    }

                    currentUserId = rs.getInt("user_id");
                    currentUsername = rs.getString("username");
                    currentRole = rs.getString("role");

                    showDashboardPage();
                } else {
                    message("Invalid username or password.");
                }
            }

        } catch (SQLException e) {
            message("Login error: " + e.getMessage());
        }
    }

    private void showRegisterPage() {
        JPanel panel = pageWrapper("Create Account");

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = new Insets(7, 7, 7, 7);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField firstNameField = new JTextField(20);
        JTextField lastNameField = new JTextField(20);
        JTextField emailField = new JTextField(20);
        JTextField usernameField = new JTextField(20);
        JPasswordField passwordField = new JPasswordField(20);

        addFormRow(form, gbc, 0, "First Name:", firstNameField);
        addFormRow(form, gbc, 1, "Last Name:", lastNameField);
        addFormRow(form, gbc, 2, "Email:", emailField);
        addFormRow(form, gbc, 3, "Username:", usernameField);
        addFormRow(form, gbc, 4, "Password:", passwordField);

        JButton registerButton = new JButton("Register");
        JButton backButton = new JButton("Back to Login");

        gbc.gridx = 0;
        gbc.gridy = 5;
        form.add(registerButton, gbc);

        gbc.gridx = 1;
        form.add(backButton, gbc);

        panel.add(form, BorderLayout.CENTER);

        registerButton.addActionListener(e -> {
            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            String email = emailField.getText().trim();
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()
                    || username.isEmpty() || password.isEmpty()) {
                message("All fields are required.");
                return;
            }

            registerUser(firstName, lastName, email, username, password);
        });

        backButton.addActionListener(e -> showLoginPage());

        setPage(panel);
    }

    private void registerUser(String firstName, String lastName, String email, String username, String password) {
        String insertUserSql = """
                INSERT INTO users
                (first_name, last_name, email, username, password, role, status)
                VALUES (?, ?, ?, ?, ?, 'user', 'active')
                """;

        String insertSettingsSql = """
                INSERT INTO settings
                (user_id, theme, notifications_enabled, language, privacy_mode)
                VALUES (?, 'light', 1, 'English', 0)
                """;

        try (Connection conn = connect()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, firstName);
                ps.setString(2, lastName);
                ps.setString(3, email);
                ps.setString(4, username);
                ps.setString(5, hashPassword(password));

                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int newUserId = keys.getInt(1);

                        try (PreparedStatement settingsPs = conn.prepareStatement(insertSettingsSql)) {
                            settingsPs.setInt(1, newUserId);
                            settingsPs.executeUpdate();
                        }
                    }
                }

                conn.commit();

                message("Account created successfully.");
                showLoginPage();

            } catch (SQLException e) {
                conn.rollback();
                message("Registration failed. Username or email may already exist.");
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            message("Registration error: " + e.getMessage());
        }
    }

    private void showDashboardPage() {
        JPanel panel = pageWrapper("Dashboard");

        JPanel center = new JPanel(new GridLayout(5, 1, 12, 12));
        center.setBorder(BorderFactory.createEmptyBorder(40, 250, 40, 250));

        JLabel welcome = new JLabel(
                "Logged in as: " + currentUsername + " (" + currentRole + ")",
                SwingConstants.CENTER
        );

        JButton profileButton = new JButton("Profile Management");
        JButton settingsButton = new JButton("Account Settings");
        JButton adminButton = new JButton("Admin User Management");
        JButton logoutButton = new JButton("Logout");

        center.add(welcome);
        center.add(profileButton);
        center.add(settingsButton);

        if (currentRole.equalsIgnoreCase("admin")) {
            center.add(adminButton);
        }

        center.add(logoutButton);

        profileButton.addActionListener(e -> showProfilePage());
        settingsButton.addActionListener(e -> showSettingsPage());
        adminButton.addActionListener(e -> showAdminPage());

        logoutButton.addActionListener(e -> {
            currentUserId = -1;
            currentUsername = "";
            currentRole = "";
            showLoginPage();
        });

        panel.add(center, BorderLayout.CENTER);
        setPage(panel);
    }

    private void showProfilePage() {
        JPanel panel = pageWrapper("Profile Management");

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = new Insets(7, 7, 7, 7);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField firstNameField = new JTextField(20);
        JTextField lastNameField = new JTextField(20);
        JTextField emailField = new JTextField(20);
        JTextField usernameField = new JTextField(20);
        JPasswordField passwordField = new JPasswordField(20);

        loadProfile(firstNameField, lastNameField, emailField, usernameField);

        addFormRow(form, gbc, 0, "First Name:", firstNameField);
        addFormRow(form, gbc, 1, "Last Name:", lastNameField);
        addFormRow(form, gbc, 2, "Email:", emailField);
        addFormRow(form, gbc, 3, "Username:", usernameField);
        addFormRow(form, gbc, 4, "New Password:", passwordField);

        JButton updateButton = new JButton("Update Profile");
        JButton backButton = new JButton("Back");

        gbc.gridx = 0;
        gbc.gridy = 5;
        form.add(updateButton, gbc);

        gbc.gridx = 1;
        form.add(backButton, gbc);

        panel.add(form, BorderLayout.CENTER);

        updateButton.addActionListener(e -> {
            updateProfile(
                    firstNameField.getText().trim(),
                    lastNameField.getText().trim(),
                    emailField.getText().trim(),
                    usernameField.getText().trim(),
                    new String(passwordField.getPassword())
            );
        });

        backButton.addActionListener(e -> showDashboardPage());

        setPage(panel);
    }

    private void loadProfile(JTextField firstNameField, JTextField lastNameField,
                             JTextField emailField, JTextField usernameField) {
        String sql = """
                SELECT first_name, last_name, email, username
                FROM users
                WHERE user_id = ?
                """;

        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, currentUserId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    firstNameField.setText(rs.getString("first_name"));
                    lastNameField.setText(rs.getString("last_name"));
                    emailField.setText(rs.getString("email"));
                    usernameField.setText(rs.getString("username"));
                }
            }

        } catch (SQLException e) {
            message("Profile load error: " + e.getMessage());
        }
    }

    private void updateProfile(String firstName, String lastName, String email, String username, String newPassword) {
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || username.isEmpty()) {
            message("First name, last name, email, and username are required.");
            return;
        }

        String sqlWithoutPassword = """
                UPDATE users
                SET first_name = ?, last_name = ?, email = ?, username = ?
                WHERE user_id = ?
                """;

        String sqlWithPassword = """
                UPDATE users
                SET first_name = ?, last_name = ?, email = ?, username = ?, password = ?
                WHERE user_id = ?
                """;

        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(
                     newPassword.isEmpty() ? sqlWithoutPassword : sqlWithPassword
             )) {

            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, email);
            ps.setString(4, username);

            if (newPassword.isEmpty()) {
                ps.setInt(5, currentUserId);
            } else {
                ps.setString(5, hashPassword(newPassword));
                ps.setInt(6, currentUserId);
            }

            ps.executeUpdate();

            currentUsername = username;

            message("Profile updated successfully.");
            showDashboardPage();

        } catch (SQLException e) {
            message("Profile update failed. Username or email may already exist.");
        }
    }

    private void showSettingsPage() {
        JPanel panel = pageWrapper("Account Settings");

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = new Insets(7, 7, 7, 7);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<String> themeBox = new JComboBox<>(new String[]{"light", "dark"});
        JComboBox<String> languageBox = new JComboBox<>(new String[]{"English", "Spanish", "French"});
        JCheckBox notificationsBox = new JCheckBox("Enable Notifications");
        JCheckBox privacyBox = new JCheckBox("Enable Privacy Mode");

        loadSettings(themeBox, languageBox, notificationsBox, privacyBox);

        addFormRow(form, gbc, 0, "Theme:", themeBox);
        addFormRow(form, gbc, 1, "Language:", languageBox);

        gbc.gridx = 1;
        gbc.gridy = 2;
        form.add(notificationsBox, gbc);

        gbc.gridy = 3;
        form.add(privacyBox, gbc);

        JButton saveButton = new JButton("Save Settings");
        JButton backButton = new JButton("Back");

        gbc.gridx = 0;
        gbc.gridy = 4;
        form.add(saveButton, gbc);

        gbc.gridx = 1;
        form.add(backButton, gbc);

        panel.add(form, BorderLayout.CENTER);

        saveButton.addActionListener(e -> {
            updateSettings(
                    themeBox.getSelectedItem().toString(),
                    languageBox.getSelectedItem().toString(),
                    notificationsBox.isSelected(),
                    privacyBox.isSelected()
            );
        });

        backButton.addActionListener(e -> showDashboardPage());

        setPage(panel);
    }

    private void loadSettings(JComboBox<String> themeBox, JComboBox<String> languageBox,
                              JCheckBox notificationsBox, JCheckBox privacyBox) {
        String sql = """
                SELECT theme, notifications_enabled, language, privacy_mode
                FROM settings
                WHERE user_id = ?
                """;

        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, currentUserId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    themeBox.setSelectedItem(rs.getString("theme"));
                    languageBox.setSelectedItem(rs.getString("language"));
                    notificationsBox.setSelected(rs.getInt("notifications_enabled") == 1);
                    privacyBox.setSelected(rs.getInt("privacy_mode") == 1);
                }
            }

        } catch (SQLException e) {
            message("Settings load error: " + e.getMessage());
        }
    }

    private void updateSettings(String theme, String language, boolean notifications, boolean privacy) {
        String sql = """
                UPDATE settings
                SET theme = ?, notifications_enabled = ?, language = ?, privacy_mode = ?
                WHERE user_id = ?
                """;

        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, theme);
            ps.setInt(2, notifications ? 1 : 0);
            ps.setString(3, language);
            ps.setInt(4, privacy ? 1 : 0);
            ps.setInt(5, currentUserId);

            ps.executeUpdate();

            message("Settings updated successfully.");
            showDashboardPage();

        } catch (SQLException e) {
            message("Settings update error: " + e.getMessage());
        }
    }

    private void showAdminPage() {
        JPanel panel = pageWrapper("Admin User Management");

        DefaultTableModel model = new DefaultTableModel(
                new String[]{"ID", "First Name", "Last Name", "Email", "Username", "Role", "Status"},
                0
        );

        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);

        JPanel buttons = new JPanel(new FlowLayout());

        JButton refreshButton = new JButton("Refresh");
        JButton addUserButton = new JButton("Add User");
        JButton deactivateButton = new JButton("Deactivate");
        JButton reactivateButton = new JButton("Reactivate");
        JButton makeAdminButton = new JButton("Make Admin");
        JButton makeUserButton = new JButton("Make User");
        JButton deleteButton = new JButton("Delete User");
        JButton backButton = new JButton("Back");

        buttons.add(refreshButton);
        buttons.add(addUserButton);
        buttons.add(deactivateButton);
        buttons.add(reactivateButton);
        buttons.add(makeAdminButton);
        buttons.add(makeUserButton);
        buttons.add(deleteButton);
        buttons.add(backButton);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);

        Runnable refresh = () -> loadUsersIntoTable(model);
        refresh.run();

        refreshButton.addActionListener(e -> refresh.run());

        addUserButton.addActionListener(e -> showAdminAddUserPage());

        deactivateButton.addActionListener(e -> {
            int userId = getSelectedUserId(table);

            if (userId != -1) {
                if (userId == currentUserId) {
                    message("You cannot deactivate your own account.");
                    return;
                }

                updateUserStatus(userId, "inactive");
                refresh.run();
            }
        });

        reactivateButton.addActionListener(e -> {
            int userId = getSelectedUserId(table);

            if (userId != -1) {
                updateUserStatus(userId, "active");
                refresh.run();
            }
        });

        makeAdminButton.addActionListener(e -> {
            int userId = getSelectedUserId(table);

            if (userId != -1) {
                updateUserRole(userId, "admin");
                refresh.run();
            }
        });

        makeUserButton.addActionListener(e -> {
            int userId = getSelectedUserId(table);

            if (userId != -1) {
                if (userId == currentUserId) {
                    message("You cannot remove your own admin role.");
                    return;
                }

                updateUserRole(userId, "user");
                refresh.run();
            }
        });

        deleteButton.addActionListener(e -> {
            int userId = getSelectedUserId(table);

            if (userId != -1) {
                if (userId == currentUserId) {
                    message("You cannot delete your own account.");
                    return;
                }

                int confirm = JOptionPane.showConfirmDialog(
                        this,
                        "Are you sure you want to delete this user?",
                        "Confirm Delete",
                        JOptionPane.YES_NO_OPTION
                );

                if (confirm == JOptionPane.YES_OPTION) {
                    deleteUser(userId);
                    refresh.run();
                }
            }
        });

        backButton.addActionListener(e -> showDashboardPage());

        setPage(panel);
    }

    private void showAdminAddUserPage() {
        JPanel panel = pageWrapper("Admin Add User");

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = new Insets(7, 7, 7, 7);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField firstNameField = new JTextField(20);
        JTextField lastNameField = new JTextField(20);
        JTextField emailField = new JTextField(20);
        JTextField usernameField = new JTextField(20);
        JPasswordField passwordField = new JPasswordField(20);

        JComboBox<String> roleBox = new JComboBox<>(new String[]{"user", "admin"});
        JComboBox<String> statusBox = new JComboBox<>(new String[]{"active", "inactive"});

        addFormRow(form, gbc, 0, "First Name:", firstNameField);
        addFormRow(form, gbc, 1, "Last Name:", lastNameField);
        addFormRow(form, gbc, 2, "Email:", emailField);
        addFormRow(form, gbc, 3, "Username:", usernameField);
        addFormRow(form, gbc, 4, "Password:", passwordField);
        addFormRow(form, gbc, 5, "Role:", roleBox);
        addFormRow(form, gbc, 6, "Status:", statusBox);

        JButton createButton = new JButton("Create User");
        JButton backButton = new JButton("Back");

        gbc.gridx = 0;
        gbc.gridy = 7;
        form.add(createButton, gbc);

        gbc.gridx = 1;
        form.add(backButton, gbc);

        panel.add(form, BorderLayout.CENTER);

        createButton.addActionListener(e -> {
            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            String email = emailField.getText().trim();
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            String role = roleBox.getSelectedItem().toString();
            String status = statusBox.getSelectedItem().toString();

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()
                    || username.isEmpty() || password.isEmpty()) {
                message("All fields are required.");
                return;
            }

            adminCreateUser(firstName, lastName, email, username, password, role, status);
        });

        backButton.addActionListener(e -> showAdminPage());

        setPage(panel);
    }

    private void adminCreateUser(String firstName, String lastName, String email,
                                 String username, String password, String role, String status) {
        String insertUserSql = """
                INSERT INTO users
                (first_name, last_name, email, username, password, role, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        String insertSettingsSql = """
                INSERT INTO settings
                (user_id, theme, notifications_enabled, language, privacy_mode)
                VALUES (?, 'light', 1, 'English', 0)
                """;

        try (Connection conn = connect()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, firstName);
                ps.setString(2, lastName);
                ps.setString(3, email);
                ps.setString(4, username);
                ps.setString(5, hashPassword(password));
                ps.setString(6, role);
                ps.setString(7, status);

                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int newUserId = keys.getInt(1);

                        try (PreparedStatement settingsPs = conn.prepareStatement(insertSettingsSql)) {
                            settingsPs.setInt(1, newUserId);
                            settingsPs.executeUpdate();
                        }
                    }
                }

                conn.commit();

                message("User created successfully.");
                showAdminPage();

            } catch (SQLException e) {
                conn.rollback();
                message("Admin user creation failed. Username or email may already exist.");
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            message("Admin user creation error: " + e.getMessage());
        }
    }

    private void loadUsersIntoTable(DefaultTableModel model) {
        model.setRowCount(0);

        String sql = """
                SELECT user_id, first_name, last_name, email, username, role, status
                FROM users
                ORDER BY user_id
                """;

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("user_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getString("status")
                });
            }

        } catch (SQLException e) {
            message("User table load error: " + e.getMessage());
        }
    }

    private int getSelectedUserId(JTable table) {
        int row = table.getSelectedRow();

        if (row == -1) {
            message("Please select a user from the table.");
            return -1;
        }

        return Integer.parseInt(table.getValueAt(row, 0).toString());
    }

    private void updateUserStatus(int userId, String status) {
        String sql = "UPDATE users SET status = ? WHERE user_id = ?";

        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, userId);
            ps.executeUpdate();

            message("User status updated.");

        } catch (SQLException e) {
            message("Status update error: " + e.getMessage());
        }
    }

    private void updateUserRole(int userId, String role) {
        String sql = "UPDATE users SET role = ? WHERE user_id = ?";

        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, role);
            ps.setInt(2, userId);
            ps.executeUpdate();

            message("User role updated.");

        } catch (SQLException e) {
            message("Role update error: " + e.getMessage());
        }
    }

    private void deleteUser(int userId) {
        String deleteSettingsSql = "DELETE FROM settings WHERE user_id = ?";
        String deleteUserSql = "DELETE FROM users WHERE user_id = ?";

        try (Connection conn = connect()) {
            conn.setAutoCommit(false);

            try (PreparedStatement settingsPs = conn.prepareStatement(deleteSettingsSql);
                 PreparedStatement userPs = conn.prepareStatement(deleteUserSql)) {

                settingsPs.setInt(1, userId);
                settingsPs.executeUpdate();

                userPs.setInt(1, userId);
                userPs.executeUpdate();

                conn.commit();

                message("User deleted successfully.");

            } catch (SQLException e) {
                conn.rollback();
                message("Delete failed: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            message("Delete error: " + e.getMessage());
        }
    }

    private void addFormRow(JPanel form, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        form.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        form.add(field, gbc);
    }

    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(password.getBytes());

            StringBuilder sb = new StringBuilder();

            for (byte b : encoded) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();

        } catch (Exception e) {
            throw new RuntimeException("Password hashing failed.");
        }
    }

    private void message(String text) {
        JOptionPane.showMessageDialog(this, text);
    }
}
