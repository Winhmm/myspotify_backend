package com.winhmm.myspotify.dto.request;

import jakarta.validation.constraints.NotBlank;

public class AccountActionRequest {
    @NotBlank(message = "Password is required")
    private String password;

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}