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

import java.time.LocalDateTime;
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
    private static final int MAX_SHORT_CODE_GENERATION_ATTEMPTS = 5;
    private static final int SHORT_CODE_LENGTH = 7;

    public URLShorteningService(URLRepository urlRepository, RedisTemplate<String, String> redisTemplate) {
        this.urlRepository = urlRepository;
        this.redisTemplate = redisTemplate;
    }

    @Cacheable(value = "urlCache", key = "#shortUrl", unless = "#result == null")
    public Optional<URL> findByShortUrl(String shortUrl) {
        logger.debug("Checking cache for short URL: {}", shortUrl);
        String cachedUrl = redisTemplate.opsForValue().get(CACHE_PREFIX + shortUrl);
        if (cachedUrl != null) {
            logger.debug("Cache hit for short URL: {}", shortUrl);
            return Optional.of(new URL(cachedUrl, LocalDateTime.now(), shortUrl));
        }
        logger.debug("Cache miss for short URL: {}", shortUrl);
        return urlRepository.findByShortUrl(shortUrl);
    }

    @CachePut(value = "urlCache", key = "#result.shortUrl")
    @Transactional
    public URL saveURL(URL url) {
        if (url.getUrl() == null || url.getUrl().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty.");
        }
        url.setUrl(url.getUrl().replaceAll("^(https?://)", ""));

        if (url.getShortUrl() == null || url.getShortUrl().isEmpty()) {
            url.setShortUrl(generateUniqueShortCode());
        } else {
            if (isShortUrlExists(url.getShortUrl())) {
                throw new IllegalArgumentException("Custom short URL already exists.");
            }
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
            if (attempts > MAX_SHORT_CODE_GENERATION_ATTEMPTS) {
                logger.error("Failed to generate a unique short URL after {} attempts.", MAX_SHORT_CODE_GENERATION_ATTEMPTS);
                throw new IllegalStateException("Failed to generate unique short URL.");
            }
        } while (isShortUrlExists(shortUrl));
        return shortUrl;
    }

    private boolean isShortUrlExists(String shortUrl) {
        logger.debug("Checking Redis for short URL: {}", shortUrl);
        if (redisTemplate.hasKey(CACHE_PREFIX + shortUrl)) {
            logger.debug("Short URL {} found in Redis.", shortUrl);
            return true;
        } else {
            logger.debug("Short URL {} not found in Redis. Falling back to database check.", shortUrl);
        }
        logger.debug("Checking database for short URL: {}", shortUrl);
        if (urlRepository.findByShortUrl(shortUrl).isPresent()) {
            logger.debug("Short URL {} found in the database.", shortUrl);
            return true;
        } else {
            logger.debug("Short URL {} not found in the database.", shortUrl);
        }
        logger.debug("Short URL {} is unique.", shortUrl);
        return false;
    }

    private String generateShortCode() {
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0, SHORT_CODE_LENGTH);
    }

}
