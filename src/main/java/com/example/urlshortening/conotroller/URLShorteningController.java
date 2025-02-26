package com.example.urlshortening.conotroller;

import com.example.urlshortening.dto.URLRequestDto;
import com.example.urlshortening.dto.URLResponseDto;
import com.example.urlshortening.entities.URL;
import com.example.urlshortening.entities.URLNotFoundException;
import com.example.urlshortening.mapper.URLMapper;
import com.example.urlshortening.service.URLShorteningService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

@RestController
@RequestMapping("/urls")
public class URLShorteningController {
    private static final Logger logger = LoggerFactory.getLogger(URLShorteningController.class);

    private final URLShorteningService service;
    private final URLMapper mapper;

    public URLShorteningController(URLShorteningService service, URLMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<URLResponseDto> createShortUrl(@RequestBody @Valid URLRequestDto requestDto) {
        logger.info("Request received to shorten URL: {}", requestDto.getUrl());

        URL urlEntity = mapper.toEntity(requestDto);
        URL savedUrl = service.saveURL(urlEntity);

        String fullShortUrl = "www.shr.co/" + savedUrl.getShortUrl();
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{shortUrl}")
                .buildAndExpand(savedUrl.getShortUrl())
                .toUri();

        URLResponseDto responseDto = mapper.toResponseDto(savedUrl);
        responseDto.setShortUrl(fullShortUrl);

        return ResponseEntity.created(location).body(responseDto);
    }


    @GetMapping("/{shortUrl}")
    public ResponseEntity<Void> redirectToUrl(@PathVariable String shortUrl) {
        Optional<URL> url = service.findByShortUrl(shortUrl);
        if (url.isPresent()) {
            return ResponseEntity.status(301)
                    .location(URI.create("http://" + url.get().getUrl()))
                    .build();
        } else {
            logger.warn("URL not found for short URL: {}", shortUrl);
            throw new URLNotFoundException(shortUrl);
        }
    }
}
