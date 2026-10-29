package com.careertrack.controller;

import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.careertrack.dto.JobApplicationRequest;
import com.careertrack.model.ApplicationStatus;
import com.careertrack.model.JobApplication;
import com.careertrack.repository.JobApplicationRepository;
import com.careertrack.repository.UserRepository;
import com.careertrack.repository.InterviewRepository;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/applications")
public class JobApplicationController {
  private final JobApplicationRepository applicationRepository;
  private final UserRepository userRepository;
  private final InterviewRepository interviewRepository;

  public JobApplicationController(JobApplicationRepository applicationRepository, UserRepository userRepository,
      InterviewRepository interviewRepository) {
    this.applicationRepository = applicationRepository;
    this.userRepository = userRepository;
    this.interviewRepository = interviewRepository;
  }

  @GetMapping
  public List<JobApplication> getApplications(Authentication authentication) {
    return applicationRepository.findByUserIdOrderByAppliedDateDesc(currentUserId(authentication));
  }

  @PostMapping
  public JobApplication createApplication(@Valid @RequestBody JobApplicationRequest request, Authentication authentication) {
    JobApplication application = new JobApplication();
    applyRequest(application, request);
    application.setUserId(currentUserId(authentication));
    return applicationRepository.save(application);
  }

  @PutMapping("/{id}")
  public ResponseEntity<JobApplication> updateApplication(@PathVariable String id,
      @Valid @RequestBody JobApplicationRequest request, Authentication authentication) {
    String userId = currentUserId(authentication);
    return applicationRepository.findById(id)
        .filter(app -> app.getUserId().equals(userId))
        .map(app -> {
          applyRequest(app, request);
          return ResponseEntity.ok(applicationRepository.save(app));
        })
        .orElse(ResponseEntity.notFound().build());
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteApplication(@PathVariable String id, Authentication authentication) {
    String userId = currentUserId(authentication);
    return applicationRepository.findById(id)
        .filter(app -> app.getUserId().equals(userId))
        .map(app -> {
          interviewRepository.deleteAll(interviewRepository.findByUserIdAndApplicationId(userId, id));
          applicationRepository.delete(app);
          return ResponseEntity.noContent().<Void>build();
        })
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/status/{status}")
  public List<JobApplication> getByStatus(@PathVariable ApplicationStatus status, Authentication authentication) {
    return applicationRepository.findByUserIdAndStatus(currentUserId(authentication), status);
  }

  private void applyRequest(JobApplication app, JobApplicationRequest request) {
    app.setCompanyName(request.getCompanyName());
    app.setJobTitle(request.getJobTitle());
    app.setLocation(request.getLocation());
    app.setJobType(request.getJobType());
    app.setStatus(request.getStatus() == null ? ApplicationStatus.APPLIED : request.getStatus());
    app.setAppliedDate(request.getAppliedDate() == null ? LocalDate.now() : request.getAppliedDate());
    app.setNotes(request.getNotes());
  }

  private String currentUserId(Authentication authentication) {
    return userRepository.findByEmail(authentication.getName()).orElseThrow().getId();
  }
}
