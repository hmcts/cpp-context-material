package uk.gov.service;

public interface CacheService {
    String add(String key, String value);

    String get(String key);
}
