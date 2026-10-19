package com.guessword;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class AdminFrame extends JFrame {

    private final User user;

    public AdminFrame(User user) {
        this.user = user;

        setTitle("Guess the Word - Admin");
        setSize(500, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JLabel heading = new JLabel(
                "Admin Dashboard",
                SwingConstants.CENTER
        );
        heading.setFont(new Font("Arial", Font.BOLD, 22));

        JLabel welcome = new JLabel(
                "Welcome, " + user.username(),
                SwingConstants.CENTER
        );

        JLabel message = new JLabel(
                "Reports will be implemented next.",
                SwingConstants.CENTER
        );

        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });

        JPanel center = new JPanel(new GridLayout(2, 1, 5, 5));
        center.add(welcome);
        center.add(message);

        JPanel panel = new JPanel(new BorderLayout(10, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(25, 20, 25, 20));
        panel.add(heading, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);
        panel.add(logoutButton, BorderLayout.SOUTH);

        setContentPane(panel);
    }
}
