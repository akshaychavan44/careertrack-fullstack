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

@RestController
@RequestMapping("/api/interviews")
public class InterviewController {
  private final InterviewRepository interviewRepository;
  private final UserRepository userRepository;

  public InterviewController(InterviewRepository interviewRepository, UserRepository userRepository) {
    this.interviewRepository = interviewRepository;
    this.userRepository = userRepository;
  }

  @GetMapping
  public List<Interview> getInterviews(Authentication authentication) {
    return interviewRepository.findByUserIdOrderByInterviewDateAsc(currentUserId(authentication));
  }

  @PostMapping
  public Interview createInterview(@RequestBody InterviewRequest request, Authentication authentication) {
    Interview interview = new Interview();
    applyRequest(interview, request);
    interview.setUserId(currentUserId(authentication));
    return interviewRepository.save(interview);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Interview> updateInterview(@PathVariable String id,
      @RequestBody InterviewRequest request, Authentication authentication) {
    String userId = currentUserId(authentication);
    return interviewRepository.findById(id)
        .filter(interview -> interview.getUserId().equals(userId))
        .map(interview -> {
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
