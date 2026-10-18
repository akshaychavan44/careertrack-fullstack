package com.careertrack.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
  public ResponseEntity<?> authentication() {
    return ResponseEntity.status(401).body(Map.of("message", "Email or password is incorrect."));
  }

  @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
  public ResponseEntity<?> validation() {
    return ResponseEntity.badRequest().body(Map.of("message", "Please complete all required fields with valid values."));
  }

  @ExceptionHandler(org.springframework.dao.DataAccessException.class)
  public ResponseEntity<?> database() {
    return ResponseEntity.status(503).body(Map.of("message", "Database is unavailable. Please try again shortly."));
  }
}
