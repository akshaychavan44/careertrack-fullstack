package com.careertrack.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.careertrack.model.*;
import com.careertrack.repository.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TrackingControllerTest {
  private final UserRepository users = mock(UserRepository.class);
  private final JobApplicationRepository applications = mock(JobApplicationRepository.class);
  private final InterviewRepository interviews = mock(InterviewRepository.class);
  private final UsernamePasswordAuthenticationToken principal =
      new UsernamePasswordAuthenticationToken("owner@example.com", null);
  private MockMvc mvc;
  private static final String INTERVIEW = """
      {"applicationId":"app-1","round":"Technical","interviewDate":"2026-10-29T10:00:00","mode":"Online"}
      """;

  @BeforeEach void setup() {
    User owner = new User();
    owner.setId("owner");
    owner.setEmail("owner@example.com");
    when(users.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
    when(applications.save(any())).thenAnswer(call -> call.getArgument(0));
    when(interviews.save(any())).thenAnswer(call -> call.getArgument(0));
    mvc = MockMvcBuilders.standaloneSetup(
        new JobApplicationController(applications, users, interviews),
        new InterviewController(interviews, users, applications), new UserController(users),
        new DashboardController(applications, users))
        .setControllerAdvice(new ApiExceptionHandler()).build();
  }

  @Test void invalidApplicationsAreRejected() throws Exception {
    for (String body : List.of("{}", "{\"companyName\":\" \",\"jobTitle\":\"Engineer\"}")) {
      mvc.perform(post("/api/applications").principal(principal)
          .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    }
    verify(applications, never()).save(any());
  }

  @Test void applicationDefaultsAreApplied() throws Exception {
    mvc.perform(post("/api/applications").principal(principal).contentType(MediaType.APPLICATION_JSON)
        .content("{\"companyName\":\"Company\",\"jobTitle\":\"Engineer\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPLIED"))
        .andExpect(jsonPath("$.appliedDate").isNotEmpty()).andExpect(jsonPath("$.userId").value("owner"));
  }

  @Test void invalidStatusAndMalformedDateReturnBadRequest() throws Exception {
    mvc.perform(get("/api/applications/status/INVALID").principal(principal)).andExpect(status().isBadRequest());
    mvc.perform(post("/api/applications").principal(principal).contentType(MediaType.APPLICATION_JSON)
        .content("{\"companyName\":\"Company\",\"jobTitle\":\"Engineer\",\"appliedDate\":\"bad\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test void anotherUsersApplicationCannotBeEditedOrDeleted() throws Exception {
    when(applications.findById("app-1")).thenReturn(Optional.of(application("other")));
    mvc.perform(put("/api/applications/app-1").principal(principal).contentType(MediaType.APPLICATION_JSON)
        .content("{\"companyName\":\"Company\",\"jobTitle\":\"Engineer\"}"))
        .andExpect(status().isNotFound());
    mvc.perform(delete("/api/applications/app-1").principal(principal)).andExpect(status().isNotFound());
    verify(applications, never()).save(any());
    verify(applications, never()).delete(any());
    verifyNoInteractions(interviews);
  }

  @Test void deletingApplicationAlsoDeletesItsInterviews() throws Exception {
    JobApplication app = application("owner");
    Interview interview = new Interview();
    when(applications.findById("app-1")).thenReturn(Optional.of(app));
    when(interviews.findByUserIdAndApplicationId("owner", "app-1")).thenReturn(List.of(interview));
    mvc.perform(delete("/api/applications/app-1").principal(principal)).andExpect(status().isNoContent());
    verify(interviews).deleteAll(List.of(interview));
    verify(applications).delete(app);
  }

  @Test void interviewMustReferenceAnOwnedApplication() throws Exception {
    mvc.perform(post("/api/interviews").principal(principal).contentType(MediaType.APPLICATION_JSON)
        .content(INTERVIEW)).andExpect(status().isNotFound());
    when(applications.findById("app-1")).thenReturn(Optional.of(application("other")));
    mvc.perform(post("/api/interviews").principal(principal).contentType(MediaType.APPLICATION_JSON)
        .content(INTERVIEW)).andExpect(status().isNotFound());
    verify(interviews, never()).save(any());
    when(applications.findById("app-1")).thenReturn(Optional.of(application("owner")));
    mvc.perform(post("/api/interviews").principal(principal).contentType(MediaType.APPLICATION_JSON)
        .content(INTERVIEW)).andExpect(status().isOk()).andExpect(jsonPath("$.userId").value("owner"));
  }

  @Test void interviewCannotBeMovedToAnotherUsersApplication() throws Exception {
    Interview interview = new Interview();
    interview.setUserId("owner");
    when(interviews.findById("interview-1")).thenReturn(Optional.of(interview));
    when(applications.findById("app-1")).thenReturn(Optional.of(application("other")));
    mvc.perform(put("/api/interviews/interview-1").principal(principal).contentType(MediaType.APPLICATION_JSON)
        .content(INTERVIEW)).andExpect(status().isNotFound());
    verify(interviews, never()).save(any());
  }

  @Test void incompleteInterviewsAreRejected() throws Exception {
    mvc.perform(post("/api/interviews").principal(principal).contentType(MediaType.APPLICATION_JSON)
        .content("{}")).andExpect(status().isBadRequest());
    verify(interviews, never()).save(any());
  }

  @Test void usersCannotReadAnotherUsersProfile() throws Exception {
    User other = new User();
    other.setEmail("other@example.com");
    when(users.findById("other")).thenReturn(Optional.of(other));
    mvc.perform(get("/api/users/other").principal(principal)).andExpect(status().isNotFound());
  }

  private JobApplication application(String owner) {
    JobApplication app = new JobApplication();
    app.setId("app-1");
    app.setUserId(owner);
    return app;
  }
}
