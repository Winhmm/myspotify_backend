package com.winhmm.myspotify.dto.response;

import com.winhmm.myspotify.enums.AccountStatus;
import com.winhmm.myspotify.enums.ArtistRequestStatus;
import com.winhmm.myspotify.enums.Role;

public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private AccountStatus accountStatus;
    private Role role;
    private ArtistRequestStatus artistRequestStatus;
    private String artistName;
    private String bio;

    public UserProfileResponse(Long id, String username, String email, String fullName,
                               String avatarUrl, AccountStatus accountStatus, Role role,
                               ArtistRequestStatus artistRequestStatus, String artistName, String bio) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.accountStatus = accountStatus;
        this.role = role;
        this.artistRequestStatus = artistRequestStatus;
        this.artistName = artistName;
        this.bio = bio;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getAvatarUrl() { return avatarUrl; }
    public AccountStatus getAccountStatus() { return accountStatus; }
    public Role getRole() { return role; }
    public ArtistRequestStatus getArtistRequestStatus() { return artistRequestStatus; }
    public String getArtistName() { return artistName; }
    public String getBio() { return bio; }
}
