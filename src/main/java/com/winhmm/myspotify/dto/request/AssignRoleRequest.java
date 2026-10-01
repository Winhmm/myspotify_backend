package com.winhmm.myspotify.dto.request;

import com.winhmm.myspotify.enums.Role;

public class AssignRoleRequest {
    private Role role;

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
