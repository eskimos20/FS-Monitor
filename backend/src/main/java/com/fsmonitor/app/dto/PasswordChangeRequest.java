package com.fsmonitor.app.dto;

import com.fsmonitor.app.util.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PasswordChangeRequest {
    @NotBlank
    private String currentPassword;

    @NotBlank
    @Size(min = PasswordPolicy.MIN_LENGTH, max = 120)
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = PasswordPolicy.DESCRIPTION)
    private String newPassword;

    @NotBlank
    @Size(min = PasswordPolicy.MIN_LENGTH, max = 120)
    private String confirmPassword;

    public PasswordChangeRequest() {}

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
