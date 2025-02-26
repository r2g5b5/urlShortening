package com.example.urlshortening.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class URLRequestDto {
    @NotBlank
    @Pattern(regexp = "^(http|https)://.*$", message = "URL must start with http or https")
    private String url;
}