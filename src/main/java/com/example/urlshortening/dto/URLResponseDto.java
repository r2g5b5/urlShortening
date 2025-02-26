package com.example.urlshortening.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class URLResponseDto {
    private int id;
    private String url;
    private String shortUrl;
    private String createdDate;
}