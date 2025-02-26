package com.example.urlshortening.entities;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(ControllerAdvice.class);

    @ExceptionHandler(URLNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String urlNotFoundHandler(URLNotFoundException ex) {
        logger.error("Error: {}", ex.getMessage());
        return ex.getMessage();
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String constraintViolation(ConstraintViolationException e) {
        logger.error("Validation error: {}", e.getConstraintViolations());
        return e.getConstraintViolations().toString();
    }
}