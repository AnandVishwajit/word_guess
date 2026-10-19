package com.guessword;

public record User(int id, String username, String role) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}