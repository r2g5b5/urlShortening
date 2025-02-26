package com.example.urlshortening.repo;

import com.example.urlshortening.entities.URL;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface URLRepository extends JpaRepository<URL, Integer> {
    Optional<URL> findByShortUrl(String shortUrl);
}