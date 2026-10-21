package com.winhmm.myspotify.repository;

import com.winhmm.myspotify.entity.SongLyrics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


/*
    =====================================================================
    SONGLYRICSREPOSITORY - TRUY VẤN BẢNG song_lyrics
    =====================================================================
*/
public interface SongLyricsRepository extends JpaRepository<SongLyrics, Long> {
    Optional<SongLyrics> findBySongId(Long songId);
}
