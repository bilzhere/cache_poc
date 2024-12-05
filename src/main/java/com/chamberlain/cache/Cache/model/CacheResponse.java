package com.chamberlain.cache.Cache.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level= AccessLevel.PRIVATE)
@Getter
@Setter
@Builder
public class CacheResponse {
    String message;
    String timeTake;
    String status;
    String cacheKey;
    String noOfEntries;
    JsonNode data;
}
