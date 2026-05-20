package com.careertrack.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.careertrack.dto.DashboardStats;
import com.careertrack.model.ApplicationStatus;
import com.careertrack.repository.JobApplicationRepository;
import com.careertrack.repository.UserRepository;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
  private final JobApplicationRepository applicationRepository;
  private final UserRepository userRepository;

  public DashboardController(JobApplicationRepository applicationRepository, UserRepository userRepository) {
    this.applicationRepository = applicationRepository;
    this.userRepository = userRepository;
  }

  @GetMapping("/stats")
  public DashboardStats stats(Authentication authentication) {
    String userId = userRepository.findByEmail(authentication.getName()).orElseThrow().getId();
    return new DashboardStats(
        applicationRepository.countByUserId(userId),
        applicationRepository.countByUserIdAndStatus(userId, ApplicationStatus.APPLIED),
        applicationRepository.countByUserIdAndStatus(userId, ApplicationStatus.INTERVIEW),
        applicationRepository.countByUserIdAndStatus(userId, ApplicationStatus.OFFER),
        applicationRepository.countByUserIdAndStatus(userId, ApplicationStatus.REJECTED)
    );
  }
}
