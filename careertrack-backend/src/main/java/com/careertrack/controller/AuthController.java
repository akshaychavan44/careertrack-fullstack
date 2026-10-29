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
import com.careertrack.repository.JobApplicationRepository;
import com.careertrack.repository.UserRepository;
import com.careertrack.security.CustomUserDetailsService;
import com.careertrack.security.JwtService;
import jakarta.validation.Valid;
import com.careertrack.service.JewelerProfileService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final UserRepository userRepository;
  private final JobApplicationRepository applicationRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final CustomUserDetailsService userDetailsService;
  private final JwtService jwtService;
  private final JewelerProfileService jewelerProfileService;

  public AuthController(UserRepository userRepository, JobApplicationRepository applicationRepository,
      PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
      CustomUserDetailsService userDetailsService, JwtService jwtService,
      JewelerProfileService jewelerProfileService) {
    this.userRepository = userRepository;
    this.applicationRepository = applicationRepository;
    this.passwordEncoder = passwordEncoder;
    this.authenticationManager = authenticationManager;
    this.userDetailsService = userDetailsService;
    this.jwtService = jwtService;
    this.jewelerProfileService = jewelerProfileService;
  }

  @PostMapping("/register")
  public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
    String email = request.getEmail().trim();
    var existingUsers = userRepository.findAllByEmail(email);
    User user;
    boolean hasStoreName = request.getStoreName() != null && !request.getStoreName().trim().isEmpty();

    if (!existingUsers.isEmpty()) {
      user = existingUsers.get(0);
      boolean matchesPassword = passwordEncoder.matches(request.getPassword(), user.getPassword());
      // Only a pending external profile may be retried, using the original credentials.
      if (existingUsers.size() != 1 || !matchesPassword || user.isJewelerProfileCreated()
          || !hasStoreName || user.getStoreName() == null
          || !user.getStoreName().equals(request.getStoreName().trim())) {
        return ResponseEntity.badRequest().body("Email already registered");
      }
    } else {
      user = new User();
      user.setName(request.getName());
      user.setEmail(email);
      user.setPassword(passwordEncoder.encode(request.getPassword()));
      if (hasStoreName) {
        user.setStoreName(request.getStoreName().trim());
      }
      userRepository.save(user);
    }

    if (hasStoreName) {
      jewelerProfileService.ensureProfile(user, user.getStoreName(), request.getPassword());
    }

    var userDetails = userDetailsService.loadUserByUsername(email);
    String token = jwtService.generateToken(userDetails);
    return ResponseEntity.ok(new AuthResponse(token, user.getName(), user.getEmail()));
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
    User user = userRepository.findFirstByEmail(request.getEmail()).orElseThrow();
    var userDetails = userDetailsService.loadUserByUsername(user.getEmail());
    String token = jwtService.generateToken(userDetails);
    return ResponseEntity.ok(new AuthResponse(token, user.getName(), user.getEmail()));
  }
}
