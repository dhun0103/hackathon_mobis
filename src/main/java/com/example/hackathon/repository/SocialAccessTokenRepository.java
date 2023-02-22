package com.example.hackathon.repository;

import com.example.hackathon.domain.SocialAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAccessTokenRepository extends JpaRepository<SocialAccessToken,Long> {

    Optional<SocialAccessToken> findByAccountEmail(String email);
}
