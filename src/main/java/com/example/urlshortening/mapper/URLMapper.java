package com.example.urlshortening.mapper;

import com.example.urlshortening.dto.URLRequestDto;
import com.example.urlshortening.dto.URLResponseDto;
import com.example.urlshortening.entities.URL;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class URLMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    private static final String BASE_URL = "www.shr.co/";

    public URLResponseDto toResponseDto(URL url) {
        return new URLResponseDto(
                url.getId(),
                url.getUrl(),
                BASE_URL + url.getShortUrl(),
                formatDate(url.getCreatedDate())
        );
    }

    public URL toEntity(URLRequestDto dto) {
        return new URL(
                dto.getUrl(),
                LocalDateTime.now(),
                null
        );
    }

    private String formatDate(LocalDateTime createdDate) {
        return createdDate != null ? createdDate.format(FORMATTER) : null;
    }
}
