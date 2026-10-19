package com.guessword;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

public class GameFrame extends JFrame {

    private final User user;
    private final GameService service;

    private final JLabel[][] tiles = new JLabel[5][5];
    private final JTextField guessField = new JTextField(8);
    private final JLabel statusLabel = new JLabel("Start a new game.");
    private final JLabel attemptsLabel = new JLabel("Guesses: 0 / 5");

    private int row = 0;
    private boolean gameStarted = false;

    public GameFrame(User user) {
        this.user = user;
        this.service = new GameService(user);

        setTitle("Guess the Word");
        setSize(470, 590);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(10, 12));
        root.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel heading = new JLabel("GUESS THE WORD", SwingConstants.CENTER);
        heading.setFont(new Font("Arial", Font.BOLD, 25));
        root.add(heading, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(10, 10));

        JPanel grid = new JPanel(new GridLayout(5, 5, 6, 6));
        grid.setPreferredSize(new Dimension(300, 300));

        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                JLabel tile = new JLabel("", SwingConstants.CENTER);
                tile.setOpaque(true);
                tile.setBackground(Color.WHITE);
                tile.setForeground(Color.BLACK);
                tile.setFont(new Font("Arial", Font.BOLD, 24));
                tile.setBorder(new LineBorder(new Color(190, 190, 190), 2));
                tiles[r][c] = tile;
                grid.add(tile);
            }
        }

        center.add(grid, BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout());
        guessField.setFont(new Font("Arial", Font.BOLD, 18));
        guessField.setDocument(new UppercaseDocument(5));

        JButton submitButton = new JButton("Submit Guess");
        JButton newGameButton = new JButton("New Game");

        controls.add(guessField);
        controls.add(submitButton);
        controls.add(newGameButton);

        submitButton.addActionListener(e -> submitGuess());
        guessField.addActionListener(e -> submitGuess());
        newGameButton.addActionListener(e -> startNewGame());

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.add(controls, BorderLayout.NORTH);
        bottom.add(statusLabel, BorderLayout.CENTER);
        bottom.add(attemptsLabel, BorderLayout.SOUTH);

        center.add(bottom, BorderLayout.SOUTH);

        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });

        JPanel south = new JPanel(new BorderLayout());
        south.add(logoutButton, BorderLayout.EAST);

        root.add(center, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);
        setContentPane(root);

        startNewGame();
    }

    private void startNewGame() {
        try {
            service.startGame();
            row = 0;
            gameStarted = true;

            for (int r = 0; r < 5; r++) {
                for (int c = 0; c < 5; c++) {
                    tiles[r][c].setText("");
                    tiles[r][c].setBackground(Color.WHITE);
                    tiles[r][c].setForeground(Color.BLACK);
                }
            }

            guessField.setText("");
            guessField.setEnabled(true);
            statusLabel.setText("Enter a valid five-letter word.");
            attemptsLabel.setText("Guesses: 0 / 5");
            guessField.requestFocusInWindow();

        } catch (IllegalStateException e) {
            gameStarted = false;
            guessField.setEnabled(false);
            statusLabel.setText("Daily limit reached.");
            JOptionPane.showMessageDialog(
                    this, e.getMessage(), "Daily Limit",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            gameStarted = false;
            guessField.setEnabled(false);
            showError("Could not start game: " + e.getMessage());
        }
    }

    private void submitGuess() {
        if (!gameStarted) {
            return;
        }

        String guess = guessField.getText().trim().toUpperCase();

        try {
            WordEvaluator.Result[] result = service.submitGuess(guess);

            for (int c = 0; c < 5; c++) {
                tiles[row][c].setText(
                        String.valueOf(guess.charAt(c)));
                tiles[row][c].setBackground(
                        WordEvaluator.getColor(result[c]));
                tiles[row][c].setForeground(Color.WHITE);
            }

            row = service.getGuessesUsed();
            attemptsLabel.setText("Guesses: " + row + " / 5");
            guessField.setText("");

            if (service.isFinished()) {
                gameStarted = false;
                guessField.setEnabled(false);

                if (service.isWon()) {
                    statusLabel.setText("Congratulations! You guessed it!");
                    JOptionPane.showMessageDialog(
                            this, "Congratulations! You guessed the word!",
                            "You Win", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    statusLabel.setText(
                            "Better luck next time! Word: "
                                    + service.getSecretWord());
                    JOptionPane.showMessageDialog(
                            this, "Better luck next time! The word was "
                                    + service.getSecretWord(),
                            "Game Over", JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                statusLabel.setText("Try again!");
                guessField.requestFocusInWindow();
            }

        } catch (IllegalArgumentException | IllegalStateException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            showError("Could not save guess: " + e.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static class UppercaseDocument
            extends javax.swing.text.PlainDocument {

        private final int maxLength;

        UppercaseDocument(int maxLength) {
            this.maxLength = maxLength;
        }

        @Override
        public void insertString(
                int offset, String text,
                javax.swing.text.AttributeSet attributes)
                throws javax.swing.text.BadLocationException {

            if (text == null) {
                return;
            }

            String value = text.toUpperCase()
                    .replaceAll("[^A-Z]", "");

            if (getLength() + value.length() <= maxLength) {
                super.insertString(offset, value, attributes);
            }
        }
    }
}
