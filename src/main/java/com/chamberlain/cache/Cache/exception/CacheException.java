package com.chamberlain.cache.Cache.exception;


import com.chamberlain.cache.Cache.model.CacheResponse;
import io.lettuce.core.RedisCommandExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class CacheException {
    @ExceptionHandler(RedisCommandExecutionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<CacheResponse> handleRedisException(RedisCommandExecutionException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CacheResponse.builder()
                .message("Error while processing json, "+e.getMessage())
                .noOfEntries("0")
                .timeTake("0 ns")
                .status("Failure")
                .build());
    }

    @ExceptionHandler(CacheCustomException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<CacheResponse> handleCacheCustomException(CacheCustomException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(CacheResponse.builder()
                .message("Error while processing json, "+e.getMessage())
                .noOfEntries("0")
                .cacheKey(e.getKey())
                .timeTake("0 ns")
                .status("Failure")
                .build());
    }

    @ExceptionHandler({RuntimeException.class})
    public ResponseEntity<Object> handleRuntimeException(RuntimeException exception) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(exception.getMessage());
    }
}
