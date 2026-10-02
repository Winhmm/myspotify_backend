package com.winhmm.myspotify.entity;

import com.winhmm.myspotify.enums.AccountStatus;
import com.winhmm.myspotify.enums.ArtistRequestStatus;
import com.winhmm.myspotify.enums.Role;
import jakarta.persistence.*;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_artist_request_status", columnList = "artist_request_status")
        }
)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", length = 20, nullable = false)
    private AccountStatus accountStatus = AccountStatus.UNVERIFIED;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "artist_request_status", length = 20, nullable = false)
    private ArtistRequestStatus artistRequestStatus = ArtistRequestStatus.NONE;

    @Column(name = "artist_name", length = 100)
    private String artistName;

    @Column(length = 255)
    private String bio;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public AccountStatus getAccountStatus() { return accountStatus; }
    public void setAccountStatus(AccountStatus accountStatus) { this.accountStatus = accountStatus; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public ArtistRequestStatus getArtistRequestStatus() { return artistRequestStatus; }
    public void setArtistRequestStatus(ArtistRequestStatus artistRequestStatus) { this.artistRequestStatus = artistRequestStatus; }

    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
