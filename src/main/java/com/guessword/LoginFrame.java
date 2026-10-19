package com.guessword;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

public class LoginFrame extends JFrame {

    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);

    public LoginFrame() {
        setTitle("Guess the Word - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 300);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 6, 8, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel heading = new JLabel("GUESS THE WORD", SwingConstants.CENTER);
        heading.setFont(new Font("Arial", Font.BOLD, 22));

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        panel.add(heading, c);

        c.gridwidth = 1;
        c.gridy++;
        c.gridx = 0;
        panel.add(new JLabel("Username:"), c);
        c.gridx = 1;
        panel.add(usernameField, c);

        c.gridy++;
        c.gridx = 0;
        panel.add(new JLabel("Password:"), c);
        c.gridx = 1;
        panel.add(passwordField, c);

        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");

        c.gridy++;
        c.gridx = 0;
        panel.add(loginButton, c);
        c.gridx = 1;
        panel.add(registerButton, c);

        loginButton.addActionListener(e -> login());
        registerButton.addActionListener(e -> register());

        setContentPane(panel);
    }

    private void login() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showError("Enter both username and password.");
            return;
        }

        try {
            User user = AuthService.login(username, password);

            if (user == null) {
                showError("Invalid username or password.");
                return;
            }

            JOptionPane.showMessageDialog(this, "Welcome, " + user.username() + "!");

            dispose();

            if (user.isAdmin()) {
                new AdminFrame(user).setVisible(true);
            } else {
                new GameFrame(user).setVisible(true);
            }

        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        } catch (RuntimeException ex) {
            showError("Unable to open the next screen: " + ex.getMessage());
        }
    }

    private void register() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        try {
            AuthService.register(username, password);
            JOptionPane.showMessageDialog(
                    this, "Registration successful. You can now log in.");
            passwordField.setText("");

        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
