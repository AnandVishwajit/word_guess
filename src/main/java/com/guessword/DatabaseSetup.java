package com.guessword;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import org.mindrot.jbcrypt.BCrypt;

public class DatabaseSetup {

    private static final String[] WORDS = {
        "AUDIO", "HOUSE", "MOUSE", "TIGER", "APPLE",
        "GRAPE", "BREAD", "CHAIR", "PLANT", "WATER",
        "STONE", "LIGHT", "MONEY", "TRAIN", "CLOUD",
        "DREAM", "SMILE", "WORLD", "RIVER", "EARTH"
    };

    public static void initialize() throws Exception {
        try (Connection con = DB.getConnection();
             Statement st = con.createStatement()) {

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    username VARCHAR(50) NOT NULL UNIQUE,
                    password_hash VARCHAR(100) NOT NULL,
                    role ENUM('ADMIN','PLAYER') NOT NULL DEFAULT 'PLAYER',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS words (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    word CHAR(5) NOT NULL UNIQUE
                )
                """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS games (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    user_id INT NOT NULL,
                    secret_word CHAR(5) NOT NULL,
                    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    game_date DATE NOT NULL DEFAULT (CURRENT_DATE),
                    won BOOLEAN NOT NULL DEFAULT FALSE,
                    completed BOOLEAN NOT NULL DEFAULT FALSE,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
                """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS guesses (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    game_id INT NOT NULL,
                    guess_number INT NOT NULL,
                    guessed_word CHAR(5) NOT NULL,
                    correct BOOLEAN NOT NULL DEFAULT FALSE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE (game_id, guess_number),
                    FOREIGN KEY (game_id) REFERENCES games(id)
                        ON DELETE CASCADE
                )
                """);

            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT IGNORE INTO words (word) VALUES (?)")) {
                for (String word : WORDS) {
                    ps.setString(1, word);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            try (PreparedStatement ps = con.prepareStatement("""
                    INSERT IGNORE INTO users
                        (username, password_hash, role)
                    VALUES (?, ?, 'ADMIN')
                    """)) {
                ps.setString(1, "admin");
                ps.setString(2, BCrypt.hashpw(
                        "Admin1$", BCrypt.gensalt()));
                ps.executeUpdate();
            }
        }
    }
}
