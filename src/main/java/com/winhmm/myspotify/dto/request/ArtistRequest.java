package com.winhmm.myspotify.dto.request;

public class ArtistRequest {
    private String artistName;
    private String bio;

    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}