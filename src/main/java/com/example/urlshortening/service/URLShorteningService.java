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

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Transactional
public class URLShorteningService {

    private static final Logger logger = LoggerFactory.getLogger(URLShorteningService.class);

    private final URLRepository urlRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String CACHE_PREFIX = "shortUrl:";
    private static final long CACHE_EXPIRATION = 1L; // 1 hour
    private static final int MAX_ATTEMPTS = 5;
    private static final int SHORT_CODE_LENGTH = 5;
    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom random = new SecureRandom();

    public URLShorteningService(URLRepository urlRepository, RedisTemplate<String, String> redisTemplate) {
        this.urlRepository = urlRepository;
        this.redisTemplate = redisTemplate;
    }

    @Cacheable(value = "urlCache", key = "#shortUrl", unless = "#result == null")
    public Optional<URL> findByShortUrl(String shortUrl) {
        String cacheKey = CACHE_PREFIX + shortUrl;
        String cachedUrl = redisTemplate.opsForValue().get(cacheKey);

        if (cachedUrl != null) {
            logger.debug("Cache hit: {}", shortUrl);
            redisTemplate.expire(cacheKey, CACHE_EXPIRATION, TimeUnit.HOURS);
            return Optional.of(new URL(cachedUrl, LocalDateTime.now(), shortUrl));
        }

        logger.debug("Cache miss: {}", shortUrl);
        return urlRepository.findByShortUrl(shortUrl);
    }

    @CachePut(value = "urlCache", key = "#result.shortUrl")
    public URL saveURL(URL url) {
        if (url.getUrl() == null || url.getUrl().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty.");
        }

        url.setUrl(url.getUrl().replaceAll("^(https?://)", ""));

        if (url.getShortUrl() == null || url.getShortUrl().isEmpty()) {
            url.setShortUrl(generateUniqueShortCode());
        } else if (isShortUrlExists(url.getShortUrl())) {
            throw new IllegalArgumentException("Custom short URL already exists.");
        }

        URL savedUrl = urlRepository.save(url);
        logger.info("Short URL generated: {} → {}", savedUrl.getShortUrl(), savedUrl.getUrl());
        redisTemplate.opsForValue().set(CACHE_PREFIX + savedUrl.getShortUrl(), savedUrl.getUrl(), CACHE_EXPIRATION, TimeUnit.HOURS);
        return savedUrl;
    }

    private String generateUniqueShortCode() {
        String shortUrl;
        int attempts = 0;
        do {
            shortUrl = generateShortCode();
            attempts++;

            if (attempts > MAX_ATTEMPTS) {
                logger.error("Failed to generate a unique short URL after {} attempts.", MAX_ATTEMPTS);
                throw new IllegalStateException("Failed to generate unique short URL.");
            }
        } while (isShortUrlExists(shortUrl));

        return shortUrl;
    }

    private boolean isShortUrlExists(String shortUrl) {
        String cacheKey = CACHE_PREFIX + shortUrl;
        Boolean existsInCache = redisTemplate.hasKey(cacheKey);
        if (Boolean.TRUE.equals(existsInCache)) {
            return true;
        }
        return urlRepository.findByShortUrl(shortUrl).isPresent();
    }

    private String generateShortCode() {
        return random.ints(SHORT_CODE_LENGTH, 0, CHARACTERS.length())
                .mapToObj(CHARACTERS::charAt)
                .map(Object::toString)
                .collect(Collectors.joining());
    }
}
