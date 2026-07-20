package com.aditisingh.Quicklink.repository;

import com.aditisingh.Quicklink.entity.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    boolean existsByShortCode(String shortCode);

    Optional<ShortUrl> findByShortCode(String shortCode);

    List<ShortUrl> findAllByOwner_EmailOrderByCreatedAtDesc(String email);

    @Modifying
    @Query("update ShortUrl shortUrl set shortUrl.clickCount = shortUrl.clickCount + 1 where shortUrl.id = :id")
    int incrementClickCount(@Param("id") Long id);
}
