package com.winhmm.myspotify.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ArtistRequest {
    @NotBlank(message = "Artist name is required")
    @Size(max = 100, message = "Artist name must not exceed 100 characters")
    private String artistName;

    @Size(max = 255, message = "Bio must not exceed 255 characters")
    private String bio;

    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}