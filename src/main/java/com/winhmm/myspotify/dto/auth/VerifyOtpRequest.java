package com.winhmm.myspotify.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class VerifyOtpRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email")
    private String email;

    @NotBlank(message = "OTP code is required")
    @Pattern(regexp = "^\\d{6}$", message = "OTP code must be 6 digits")
    private String otpCode;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
}