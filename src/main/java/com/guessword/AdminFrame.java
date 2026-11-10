package com.guessword;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

public class AdminFrame extends JFrame {

    private final User user;

    private final JLabel totalUsersValue = new JLabel("0", SwingConstants.CENTER);
    private final JLabel totalGamesValue = new JLabel("0", SwingConstants.CENTER);
    private final JLabel completedGamesValue = new JLabel("0", SwingConstants.CENTER);
    private final JLabel winsValue = new JLabel("0", SwingConstants.CENTER);
    private final JLabel winRateValue = new JLabel("0%", SwingConstants.CENTER);

    private final DefaultTableModel usersModel = readOnlyModel(
            "ID", "Username", "Role", "Created At");
    private final DefaultTableModel gamesModel = readOnlyModel(
            "Game ID", "Username", "Started At", "Word", "Won", "Completed");
    private final DefaultTableModel wordsModel = readOnlyModel("ID", "Word");

    private final JTable usersTable = new JTable(usersModel);
    private final JTable gamesTable = new JTable(gamesModel);
    private final JTable wordsTable = new JTable(wordsModel);

    public AdminFrame(User user) {
        this.user = user;

        if (user == null || !user.isAdmin()) {
            JOptionPane.showMessageDialog(
                    null, "Administrator access is required.",
                    "Access denied", JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }

        setTitle("Word Guess - Admin Dashboard");
        setSize(900, 600);
        setMinimumSize(new java.awt.Dimension(700, 450));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JLabel heading = new JLabel(
                "Admin Dashboard — Welcome, " + user.username(),
                SwingConstants.CENTER);
        heading.setBorder(BorderFactory.createEmptyBorder(15, 5, 15, 5));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Overview", createOverviewPanel());
        tabs.addTab("Users", createUsersPanel());
        tabs.addTab("Game History", createGamesPanel());
        tabs.addTab("Word Management", createWordsPanel());

        JButton refreshButton = new JButton("Refresh All");
        refreshButton.addActionListener(e -> refreshAll());

        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(refreshButton);
        bottom.add(logoutButton);

        setLayout(new BorderLayout());
        add(heading, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        refreshAll();
    }

    private JPanel createOverviewPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 3, 12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        panel.add(statCard("Registered Users", totalUsersValue));
        panel.add(statCard("Total Games", totalGamesValue));
        panel.add(statCard("Completed Games", completedGamesValue));
        panel.add(statCard("Games Won", winsValue));
        panel.add(statCard("Win Rate", winRateValue));

        JLabel note = new JLabel(
                "Statistics are calculated from the local H2 database.",
                SwingConstants.CENTER);
        panel.add(note);
        return panel;
    }

    private JPanel statCard(String title, JLabel value) {
        JPanel card = new JPanel(new BorderLayout(5, 10));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(15, 10, 15, 10)));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(titleLabel.getFont().deriveFont(
                java.awt.Font.BOLD, 14f));
        value.setFont(value.getFont().deriveFont(
                java.awt.Font.BOLD, 25f));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JPanel createUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JScrollPane(usersTable), BorderLayout.CENTER);

        JButton refresh = new JButton("Refresh Users");
        refresh.addActionListener(e -> loadUsers());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(refresh);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createGamesPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JScrollPane(gamesTable), BorderLayout.CENTER);

        JButton refresh = new JButton("Refresh History");
        refresh.addActionListener(e -> loadGames());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(refresh);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createWordsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JScrollPane(wordsTable), BorderLayout.CENTER);

        JTextField wordField = new JTextField(10);
        wordField.setDocument(new javax.swing.text.PlainDocument() {
            @Override
            public void insertString(int offset, String text,
                    javax.swing.text.AttributeSet attributes)
                    throws javax.swing.text.BadLocationException {
                if (text != null && getLength() + text.length() <= 5) {
                    super.insertString(offset, text.toUpperCase(), attributes);
                }
            }
        });

        JButton addButton = new JButton("Add Word");
        addButton.addActionListener(e -> addWord(wordField));

        JButton deleteButton = new JButton("Delete Selected");
        deleteButton.addActionListener(e -> deleteSelectedWord());

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadWords());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(new JLabel("New 5-letter word:"));
        actions.add(wordField);
        actions.add(addButton);
        actions.add(deleteButton);
        actions.add(refreshButton);

        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private void refreshAll() {
        loadStatistics();
        loadUsers();
        loadGames();
        loadWords();
    }

    private void loadStatistics() {
        String sql = """
                SELECT
                    (SELECT COUNT(*) FROM users) AS total_users,
                    (SELECT COUNT(*) FROM games) AS total_games,
                    (SELECT COUNT(*) FROM games WHERE completed = TRUE)
                        AS completed_games,
                    (SELECT COUNT(*) FROM games
                        WHERE completed = TRUE AND won = TRUE) AS wins
                """;

        try (Connection con = DB.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                int totalGames = rs.getInt("total_games");
                int completed = rs.getInt("completed_games");
                int wins = rs.getInt("wins");

                totalUsersValue.setText(String.valueOf(rs.getInt("total_users")));
                totalGamesValue.setText(String.valueOf(totalGames));
                completedGamesValue.setText(String.valueOf(completed));
                winsValue.setText(String.valueOf(wins));

                double rate = completed == 0 ? 0.0 : wins * 100.0 / completed;
                winRateValue.setText(String.format("%.1f%%", rate));
            }
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private void loadUsers() {
        usersModel.setRowCount(0);
        String sql = """
                SELECT id, username, role, created_at
                FROM users
                ORDER BY id
                """;

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                usersModel.addRow(new Object[] {
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("role"),
                    rs.getTimestamp("created_at")
                });
            }
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private void loadGames() {
        gamesModel.setRowCount(0);
        String sql = """
                SELECT g.id, u.username, g.started_at, g.secret_word,
                       g.won, g.completed
                FROM games g
                JOIN users u ON u.id = g.user_id
                ORDER BY g.started_at DESC
                """;

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                gamesModel.addRow(new Object[] {
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getTimestamp("started_at"),
                    rs.getString("secret_word").trim(),
                    rs.getBoolean("won"),
                    rs.getBoolean("completed")
                });
            }
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private void loadWords() {
        wordsModel.setRowCount(0);
        String sql = "SELECT id, word FROM words ORDER BY word";

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                wordsModel.addRow(new Object[] {
                    rs.getInt("id"),
                    rs.getString("word").trim()
                });
            }
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private void addWord(JTextField wordField) {
        String word = wordField.getText().trim().toUpperCase();

        if (!word.matches("[A-Z]{5}")) {
            JOptionPane.showMessageDialog(
                    this, "Enter exactly five letters.",
                    "Invalid word", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "INSERT INTO words (word) VALUES (?)";

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, word);
            ps.executeUpdate();
            wordField.setText("");
            loadWords();

            JOptionPane.showMessageDialog(this, "Word added successfully.");

        } catch (SQLException ex) {
            if (ex.getSQLState() != null && ex.getSQLState().startsWith("23")) {
                JOptionPane.showMessageDialog(
                        this, "That word already exists.",
                        "Duplicate word", JOptionPane.WARNING_MESSAGE);
            } else {
                showDatabaseError(ex);
            }
        }
    }

    private void deleteSelectedWord() {
        int selectedRow = wordsTable.getSelectedRow();

        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(
                    this, "Select a word to delete.",
                    "No selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int modelRow = wordsTable.convertRowIndexToModel(selectedRow);
        int wordId = (int) wordsModel.getValueAt(modelRow, 0);
        String word = String.valueOf(wordsModel.getValueAt(modelRow, 1));

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete the word \"" + word + "\" from the dictionary?",
                "Confirm deletion",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM words WHERE id = ?")) {

            ps.setInt(1, wordId);
            ps.executeUpdate();
            loadWords();

        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private void showDatabaseError(SQLException ex) {
        JOptionPane.showMessageDialog(
                this,
                "Database operation failed:\n" + ex.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
    }

    private static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }
}
