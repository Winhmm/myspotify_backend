package com.winhmm.myspotify.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UpdateEmailRequest {
    @NotBlank(message = "OTP code is required")
    @Pattern(regexp = "^\\d{6}$", message = "OTP code must be 6 digits")
    private String otpCode;

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
}
