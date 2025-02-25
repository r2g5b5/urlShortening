package com.example.urlshortening.repo;

import com.example.urlshortening.entities.URL;
import org.springframework.data.repository.CrudRepository;

public interface  URLCacheRepository extends CrudRepository<URL, String> {

}