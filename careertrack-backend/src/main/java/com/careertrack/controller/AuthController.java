package com.careertrack.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.careertrack.dto.AuthResponse;
import com.careertrack.dto.LoginRequest;
import com.careertrack.dto.RegisterRequest;
import com.careertrack.model.User;
import com.careertrack.repository.UserRepository;
import com.careertrack.security.CustomUserDetailsService;
import com.careertrack.security.JwtService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final CustomUserDetailsService userDetailsService;
  private final JwtService jwtService;

  public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
      AuthenticationManager authenticationManager, CustomUserDetailsService userDetailsService, JwtService jwtService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.authenticationManager = authenticationManager;
    this.userDetailsService = userDetailsService;
    this.jwtService = jwtService;
  }

  @PostMapping("/register")
  public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
      return ResponseEntity.badRequest().body("Email already registered");
    }

    User user = new User();
    user.setName(request.getName());
    user.setEmail(request.getEmail());
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    userRepository.save(user);

    var userDetails = userDetailsService.loadUserByUsername(user.getEmail());
    String token = jwtService.generateToken(userDetails);
    return ResponseEntity.ok(new AuthResponse(token, user.getName(), user.getEmail()));
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
    User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
    var userDetails = userDetailsService.loadUserByUsername(user.getEmail());
    String token = jwtService.generateToken(userDetails);
    return ResponseEntity.ok(new AuthResponse(token, user.getName(), user.getEmail()));
  }
}
