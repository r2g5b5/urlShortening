package com.example.urlshortening.entities;

public class URLNotFoundException extends RuntimeException{
    public URLNotFoundException(Object id) {
        super("URL not found with id " + id);
    }
}