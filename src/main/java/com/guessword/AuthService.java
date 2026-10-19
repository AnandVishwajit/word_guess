package com.guessword;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

import org.mindrot.jbcrypt.BCrypt;

public class AuthService {

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[A-Za-z]{5,50}$");

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[$%*]).{5,}$");

    public static void register(String username, String password)
            throws SQLException {

        if (username == null ||
                !USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException(
                    "Username must contain 5–50 letters only.");
        }

        if (password == null ||
                !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException(
                    "Password must be at least 5 characters and contain " +
                    "a letter, a number, and one of $, % or *.");
        }

        String sql = """
                INSERT INTO users (username, password_hash, role)
                VALUES (?, ?, 'PLAYER')
                """;

        String hash = BCrypt.hashpw(password, BCrypt.gensalt());

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, hash);
            ps.executeUpdate();

        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState())) {
                throw new IllegalArgumentException(
                        "That username is already registered.");
            }
            throw e;
        }
    }

    public static User login(String username, String password)
            throws SQLException {

        String sql = """
                SELECT id, username, password_hash, role
                FROM users
                WHERE username = ?
                """;

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                String storedHash = rs.getString("password_hash");

                if (!BCrypt.checkpw(password, storedHash)) {
                    return null;
                }

                return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("role")
                );
            }
        }
    }
}
