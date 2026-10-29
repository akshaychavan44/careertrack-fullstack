package com.careertrack.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.careertrack.dto.UserResponse;
import com.careertrack.repository.UserRepository;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/users")
public class UserController {
  private final UserRepository userRepository;

  public UserController(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @GetMapping("/{id}")
  public ResponseEntity<UserResponse> getUserById(@PathVariable String id, Authentication authentication) {
    return userRepository.findById(id)
        .filter(user -> user.getEmail().equals(authentication.getName()))
        .map(user -> ResponseEntity.ok(new UserResponse(user)))
        .orElse(ResponseEntity.notFound().build());
  }
}
