package com.example.urlshortening.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.stereotype.Indexed;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "urls")
@Indexed
public class URL {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String url;
    private LocalDateTime createdDate = LocalDateTime.now();
    @Column(unique = true)
    private String shortUrl;

    public URL(String url, LocalDateTime createdDate, String shortUrl) {
        this.url = url;
        this.createdDate = createdDate;
        this.shortUrl = shortUrl;
    }
}
