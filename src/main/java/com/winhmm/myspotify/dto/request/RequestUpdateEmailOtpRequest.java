package com.winhmm.myspotify.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RequestUpdateEmailOtpRequest {
    @NotBlank(message = "New email is required")
    @Email(message = "Invalid email")
    @Size(max = 100, message = "New email must be less than or equal to 100 characters")
    private String newEmail;

    public String getNewEmail() { return newEmail; }
    public void setNewEmail(String newEmail) { this.newEmail = newEmail; }
}
