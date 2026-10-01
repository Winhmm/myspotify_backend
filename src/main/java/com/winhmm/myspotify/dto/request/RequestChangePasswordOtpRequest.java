package com.winhmm.myspotify.dto.request;

import jakarta.validation.constraints.NotBlank;

public class RequestChangePasswordOtpRequest {
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
}