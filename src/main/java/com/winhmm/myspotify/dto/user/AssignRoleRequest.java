package com.winhmm.myspotify.dto.user;

import com.winhmm.myspotify.enums.Role;
import jakarta.validation.constraints.NotNull;

public class AssignRoleRequest {
    @NotNull(message = "Role must not be null")
    private Role role;

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
