package com.example.urlshortening.entities;

public class URLNotFoundException extends RuntimeException {
    public URLNotFoundException(String shortUrl) {
        super("URL not found for short URL: " + shortUrl);
    }
}