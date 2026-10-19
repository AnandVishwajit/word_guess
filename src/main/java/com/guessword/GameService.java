package com.guessword;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class GameService {

    private final User user;
    private int gameId;
    private String secretWord;
    private int guessesUsed;
    private boolean finished;
    private boolean won;

    public GameService(User user) {
        this.user = user;
    }

    public int getGuessesUsed() {
        return guessesUsed;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isWon() {
        return won;
    }

    public String getSecretWord() {
        return secretWord;
    }

    public int startGame() throws SQLException {
        try (Connection con = DB.getConnection()) {
            con.setAutoCommit(false);

            try {
                String countSql = """
                    SELECT COUNT(*) FROM games
                    WHERE user_id = ? AND game_date = CURRENT_DATE
                    """;

                try (PreparedStatement ps = con.prepareStatement(countSql)) {
                    ps.setInt(1, user.id());

                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();

                        if (rs.getInt(1) >= 3) {
                            throw new IllegalStateException(
                                    "You have already started 3 games today.");
                        }
                    }
                }

                try (Statement st = con.createStatement();
                     ResultSet rs = st.executeQuery(
                             "SELECT word FROM words ORDER BY RAND() LIMIT 1")) {
                    if (!rs.next()) {
                        throw new SQLException("No words are available.");
                    }
                    secretWord = rs.getString(1);
                }

                String insertSql = """
                    INSERT INTO games (user_id, secret_word)
                    VALUES (?, ?)
                    """;

                try (PreparedStatement ps = con.prepareStatement(
                        insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, user.id());
                    ps.setString(2, secretWord);
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Could not create game.");
                        }
                        gameId = keys.getInt(1);
                    }
                }

                con.commit();

                guessesUsed = 0;
                finished = false;
                won = false;

                return gameId;

            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    public WordEvaluator.Result[] submitGuess(String guess)
            throws SQLException {

        if (finished) {
            throw new IllegalStateException("This game has finished.");
        }

        if (guess == null || !guess.matches("[A-Za-z]{5}")) {
            throw new IllegalArgumentException(
                    "Enter exactly 5 letters.");
        }

        guess = guess.toUpperCase();

        // Accept only words from the game's dictionary.
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT 1 FROM words WHERE word = ?")) {
            ps.setString(1, guess);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException(
                            "That word is not in the game dictionary.");
                }
            }
        }

        WordEvaluator.Result[] result =
                WordEvaluator.evaluate(secretWord, guess);

        guessesUsed++;
        won = guess.equals(secretWord);
        finished = won || guessesUsed >= 5;

        try (Connection con = DB.getConnection()) {
            con.setAutoCommit(false);

            try {
                String insertSql = """
                    INSERT INTO guesses
                        (game_id, guess_number, guessed_word, correct)
                    VALUES (?, ?, ?, ?)
                    """;

                try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                    ps.setInt(1, gameId);
                    ps.setInt(2, guessesUsed);
                    ps.setString(3, guess);
                    ps.setBoolean(4, won);
                    ps.executeUpdate();
                }

                if (finished) {
                    try (PreparedStatement ps = con.prepareStatement("""
                            UPDATE games
                            SET won = ?, completed = TRUE
                            WHERE id = ?
                            """)) {
                        ps.setBoolean(1, won);
                        ps.setInt(2, gameId);
                        ps.executeUpdate();
                    }
                }

                con.commit();

            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }

        return result;
    }
}
