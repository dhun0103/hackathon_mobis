package com.example.hackathon.repository;

import com.example.hackathon.domain.RefreshToken;
import com.example.hackathon.domain.SocialAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<SocialAccessToken, Long> {

    Optional<RefreshToken> findByAccountEmail(String email);

}
