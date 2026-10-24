package com.careertrack.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.util.Optional;
import com.careertrack.model.User;
import com.careertrack.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class JewelerProfileServiceTest {
  private final UserRepository repository = mock(UserRepository.class);
  private final User user = new User();
  private MockRestServiceServer server;
  private JewelerProfileService service;
  private static final String URL = "https://example.com/api/v1/jewelers";

  @BeforeEach void setup() {
    user.setId("user-123");
    user.setEmail("owner@example.com");
    when(repository.findById(user.getId())).thenReturn(Optional.of(user));
    // The service sets its timeout factory before build; bind the mock at build time.
    var builder = RestClient.builder();
    var wrapped = spy(builder);
    doAnswer(invocation -> {
      server = MockRestServiceServer.bindTo(builder).build();
      return builder.build();
    }).when(wrapped).build();
    service = new JewelerProfileService(repository, wrapped, URL, "test-secret");
  }

  @Test void createsProfileWithOnlyStoreAndEmailAndSkipsRepeatLogin() {
    server.expect(requestTo(URL)).andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer test-secret"))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json("{\"storeName\":\"Example Jewellers\",\"email\":\"owner@example.com\"}", true))
        .andRespond(withSuccess("{\"id\":\"profile-123\"}", MediaType.APPLICATION_JSON));
    service.ensureProfile(user, "Example Jewellers");
    service.ensureProfile(user, "Example Jewellers");
    assertTrue(user.isJewelerProfileCreated());
    assertEquals("Example Jewellers", user.getStoreName());
    verify(repository, times(1)).save(user);
    server.verify();
  }

  @Test void failureDoesNotMarkCreatedAndAllowsRetry() {
    server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
    server.expect(requestTo(URL)).andRespond(withSuccess());
    assertThrows(JewelerProfileException.class, () -> service.ensureProfile(user, "Test Store"));
    assertFalse(user.isJewelerProfileCreated());
    verify(repository, never()).save(any());
    service.ensureProfile(user, "Test Store");
    assertTrue(user.isJewelerProfileCreated());
    server.verify();
  }

  @Test void conflictDoesNotAssumeProfileOwnership() {
    server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.CONFLICT));
    var error = assertThrows(JewelerProfileException.class, () -> service.ensureProfile(user, "Test Store"));
    assertTrue(error.getMessage().contains("already exists"));
    assertFalse(user.isJewelerProfileCreated());
    verify(repository, never()).save(any());
    server.verify();
  }

  @Test void missingKeyDoesNotSendRequest() {
    service = new JewelerProfileService(repository, RestClient.builder(), URL, "");
    assertThrows(JewelerProfileException.class, () -> service.ensureProfile(user, "Test Store"));
    verify(repository, never()).save(any());
  }
}
