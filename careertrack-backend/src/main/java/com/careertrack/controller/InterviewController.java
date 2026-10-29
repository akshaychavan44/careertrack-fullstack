package com.careertrack.controller;

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
import com.careertrack.dto.InterviewRequest;
import com.careertrack.model.Interview;
import com.careertrack.repository.InterviewRepository;
import com.careertrack.repository.UserRepository;
import com.careertrack.repository.JobApplicationRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/interviews")
public class InterviewController {
  private final InterviewRepository interviewRepository;
  private final UserRepository userRepository;
  private final JobApplicationRepository applicationRepository;

  public InterviewController(InterviewRepository interviewRepository, UserRepository userRepository,
      JobApplicationRepository applicationRepository) {
    this.interviewRepository = interviewRepository;
    this.userRepository = userRepository;
    this.applicationRepository = applicationRepository;
  }

  @GetMapping
  public List<Interview> getInterviews(Authentication authentication) {
    return interviewRepository.findByUserIdOrderByInterviewDateAsc(currentUserId(authentication));
  }

  @PostMapping
  public Interview createInterview(@Valid @RequestBody InterviewRequest request, Authentication authentication) {
    String userId = currentUserId(authentication);
    requireOwnedApplication(request.getApplicationId(), userId);
    Interview interview = new Interview();
    applyRequest(interview, request);
    interview.setUserId(userId);
    return interviewRepository.save(interview);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Interview> updateInterview(@PathVariable String id,
      @Valid @RequestBody InterviewRequest request, Authentication authentication) {
    String userId = currentUserId(authentication);
    return interviewRepository.findById(id)
        .filter(interview -> interview.getUserId().equals(userId))
        .map(interview -> {
          requireOwnedApplication(request.getApplicationId(), userId);
          applyRequest(interview, request);
          return ResponseEntity.ok(interviewRepository.save(interview));
        })
        .orElse(ResponseEntity.notFound().build());
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteInterview(@PathVariable String id, Authentication authentication) {
    String userId = currentUserId(authentication);
    return interviewRepository.findById(id)
        .filter(interview -> interview.getUserId().equals(userId))
        .map(interview -> {
          interviewRepository.delete(interview);
          return ResponseEntity.noContent().<Void>build();
        })
        .orElse(ResponseEntity.notFound().build());
  }

  private void requireOwnedApplication(String applicationId, String userId) {
    applicationRepository.findById(applicationId).filter(app -> userId.equals(app.getUserId()))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
  }

  private void applyRequest(Interview interview, InterviewRequest request) {
    interview.setApplicationId(request.getApplicationId());
    interview.setRound(request.getRound());
    interview.setInterviewDate(request.getInterviewDate());
    interview.setMode(request.getMode());
    interview.setNotes(request.getNotes());
  }

  private String currentUserId(Authentication authentication) {
    return userRepository.findByEmail(authentication.getName()).orElseThrow().getId();
  }
}
