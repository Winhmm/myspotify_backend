package com.winhmm.myspotify.repository;

import com.winhmm.myspotify.entity.User;
import com.winhmm.myspotify.enums.ArtistRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    List<User> findByArtistRequestStatus(ArtistRequestStatus status);
}
