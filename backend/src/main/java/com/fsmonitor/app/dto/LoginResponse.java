package com.fsmonitor.app.dto;

public class LoginResponse {
    private String token;
    private Boolean passwordChanged;
    private String username;
    private String email;

    public LoginResponse(String token, Boolean passwordChanged) {
        this.token = token;
        this.passwordChanged = passwordChanged;
    }

    public LoginResponse(String token, Boolean passwordChanged, String username, String email) {
        this.token = token;
        this.passwordChanged = passwordChanged;
        this.username = username;
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Boolean getPasswordChanged() {
        return passwordChanged;
    }

    public void setPasswordChanged(Boolean passwordChanged) {
        this.passwordChanged = passwordChanged;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
