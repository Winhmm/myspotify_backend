package com.winhmm.myspotify.dto.request;

public class ChangePasswordRequest {
    private String otpCode;
    private String newPassword;

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
