package com.guessword;

import java.awt.Color;

public class WordEvaluator {

    public static final Color GREEN = new Color(83, 141, 78);
    public static final Color ORANGE = new Color(230, 160, 50);
    public static final Color GREY = new Color(120, 124, 126);

    public enum Result {
        CORRECT,
        PRESENT,
        ABSENT
    }

    public static Result[] evaluate(String secret, String guess) {
        if (secret == null || guess == null
                || secret.length() != 5 || guess.length() != 5) {
            throw new IllegalArgumentException(
                    "Secret and guess must contain exactly 5 letters.");
        }

        secret = secret.toUpperCase();
        guess = guess.toUpperCase();

        Result[] result = new Result[5];
        int[] remaining = new int[26];

        // exact position
        for (int i = 0; i < 5; i++) {
            if (guess.charAt(i) == secret.charAt(i)) {
                result[i] = Result.CORRECT;
            } else {
                remaining[secret.charAt(i) - 'A']++;
            }
        }

        // wrong position
        for (int i = 0; i < 5; i++) {
            if (result[i] == Result.CORRECT) {
                continue;
            }

            char letter = guess.charAt(i);
            int index = letter - 'A';

            if (index >= 0 && index < 26 && remaining[index] > 0) {
                result[i] = Result.PRESENT;
                remaining[index]--;
            } else {
                result[i] = Result.ABSENT;
            }
        }

        return result;
    }

    public static Color getColor(Result result) {
        return switch (result) {
            case CORRECT -> GREEN;
            case PRESENT -> ORANGE;
            case ABSENT -> GREY;
        };
    }

    public static boolean isWinningGuess(String secret, String guess) {
        return secret.equalsIgnoreCase(guess);
    }
}
