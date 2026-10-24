package com.careertrack.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.careertrack.repository.UserRepository;
import com.careertrack.security.CustomUserDetailsService;
import com.careertrack.security.JwtService;
import com.careertrack.service.JewelerProfileService;
import com.careertrack.service.JewelerProfileException;
import com.careertrack.model.User;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerTest {
  private final UserRepository repository = mock(UserRepository.class);
  private final AuthenticationManager manager = mock(AuthenticationManager.class);
  private final PasswordEncoder encoder = mock(PasswordEncoder.class);
  private final JewelerProfileService profiles = mock(JewelerProfileService.class);
  private final CustomUserDetailsService detailsService = mock(CustomUserDetailsService.class);
  private final JwtService jwt = mock(JwtService.class);
  private MockMvc mvc;
  private static final String LOGIN = "{\"email\":\"test@example.com\",\"password\":\"password\"}";
  private static final String REGISTER = "{\"name\":\"Test Owner\",\"email\":\"test@example.com\",\"password\":\"password\",\"storeName\":\"  Test Jewellers  \"}";

  @BeforeEach void setup() {
    var details = mock(UserDetails.class);
    when(detailsService.loadUserByUsername("test@example.com")).thenReturn(details);
    when(jwt.generateToken(details)).thenReturn("test-token");
    when(encoder.encode("password")).thenReturn("hashed-password");
    when(repository.findByEmail("test@example.com")).thenReturn(Optional.empty());
    when(repository.save(any(User.class))).thenAnswer(invocation -> {
      User user = invocation.getArgument(0);
      user.setId("user-123");
      return user;
    });
    var controller = new AuthController(repository, encoder, manager, detailsService, jwt, profiles);
    mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new ApiExceptionHandler()).build();
  }

  @Test void invalidCredentialsReturnUsefulUnauthorizedResponse() throws Exception {
    when(manager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));
    mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Email or password is incorrect."));
    verifyNoInteractions(profiles);
  }

  @Test void registrationRequiresStoreName() throws Exception {
    mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Test Owner\",\"email\":\"test@example.com\",\"password\":\"password\"}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(repository, profiles);
  }

  @Test void registrationSavesStoreAndCreatesProfileBeforeIssuingToken() throws Exception {
    mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER))
        .andExpect(status().isOk()).andExpect(jsonPath("$.token").value("test-token"));
    var saved = ArgumentCaptor.forClass(User.class);
    var order = inOrder(repository, profiles, jwt);
    order.verify(repository).save(saved.capture());
    assertEquals("Test Jewellers", saved.getValue().getStoreName());
    assertEquals("hashed-password", saved.getValue().getPassword());
    order.verify(profiles).ensureProfile(saved.getValue(), "Test Jewellers");
    order.verify(jwt).generateToken(any());
  }

  @Test void failedRegistrationCanBeRetriedWithSameCredentials() throws Exception {
    doThrow(new JewelerProfileException("Please retry registration.")).doNothing()
        .when(profiles).ensureProfile(any(User.class), eq("Test Jewellers"));
    mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER))
        .andExpect(status().isBadGateway()).andExpect(jsonPath("$.message").value("Please retry registration."));
    verify(jwt, never()).generateToken(any());
    var saved = ArgumentCaptor.forClass(User.class);
    verify(repository).save(saved.capture());
    when(repository.findByEmail("test@example.com")).thenReturn(Optional.of(saved.getValue()));
    when(encoder.matches("password", "hashed-password")).thenReturn(true);
    mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER))
        .andExpect(status().isOk()).andExpect(jsonPath("$.token").value("test-token"));
    verify(repository, times(1)).save(any());
    verify(profiles, times(2)).ensureProfile(saved.getValue(), "Test Jewellers");
  }

  @Test void duplicateRegistrationDoesNotCreateAnotherProfile() throws Exception {
    User user = existingUser();
    user.setJewelerProfileCreated(true);
    when(repository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(profiles);
    verify(repository, never()).save(any());
  }

  @Test void pendingRegistrationRequiresMatchingPasswordToRetry() throws Exception {
    when(repository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser()));
    when(encoder.matches("password", "hashed-password")).thenReturn(false);
    mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(profiles);
  }

  @Test void loginWorksWithoutStoreNameAndDoesNotCallJewelerApi() throws Exception {
    when(repository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser()));
    mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
        .andExpect(status().isOk()).andExpect(jsonPath("$.token").value("test-token"));
    verify(manager).authenticate(any());
    verifyNoInteractions(profiles);
  }

  private User existingUser() {
    User user = new User();
    user.setId("user-123");
    user.setName("Test Owner");
    user.setEmail("test@example.com");
    user.setPassword("hashed-password");
    user.setStoreName("Test Jewellers");
    return user;
  }
}
