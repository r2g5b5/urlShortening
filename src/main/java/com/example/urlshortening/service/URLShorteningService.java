package com.example.urlshortening.service;

import com.example.urlshortening.entities.URL;
import com.example.urlshortening.repo.URLRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class URLShorteningService {

    private static final Logger logger = LoggerFactory.getLogger(URLShorteningService.class);

    private final URLRepository urlRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private static final String CACHE_PREFIX = "shortUrl:";
    private static final long CACHE_EXPIRATION = 1L; // 1 hour

    public URLShorteningService(URLRepository urlRepository, RedisTemplate<String, String> redisTemplate) {
        this.urlRepository = urlRepository;
        this.redisTemplate = redisTemplate;
    }


    @Cacheable(value = "urlCache", key = "#shortUrl", unless = "#result == null")
    public Optional<URL> findByShortUrl(String shortUrl) {
        logger.debug("Checking cache for short URL: {}", shortUrl);
        return urlRepository.findByShortUrl(shortUrl);
    }

    @CachePut(value = "urlCache", key = "#result.shortUrl")
    @Transactional
    public URL saveURL(URL url) {
        url.setUrl(url.getUrl().replaceAll("^(https?://)", ""));
        if (url.getShortUrl() == null) {
            url.setShortUrl(generateUniqueShortCode());
        }

        URL savedUrl = urlRepository.save(url);
        logger.info("Saved URL with short URL: {}", savedUrl.getShortUrl());

        redisTemplate.opsForValue().set(CACHE_PREFIX + savedUrl.getShortUrl(), savedUrl.getUrl(), CACHE_EXPIRATION, TimeUnit.HOURS);
        return savedUrl;
    }


    private String generateUniqueShortCode() {
        String shortUrl;
        int attempts = 0;
        do {
            shortUrl = generateShortCode();
            attempts++;
            if (attempts > 5) {
                logger.error("Failed to generate a unique short URL after multiple attempts.");
                throw new IllegalStateException("Failed to generate unique short URL.");
            }
        } while (urlRepository.findByShortUrl(shortUrl).isPresent());
        return shortUrl;
    }


    private String generateShortCode() {
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0, 5);
    }
}
