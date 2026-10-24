package com.careertrack.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.careertrack.repository.UserRepository;
import com.careertrack.security.CustomUserDetailsService;
import com.careertrack.security.JwtService;
import com.careertrack.service.JewelerProfileService;
import com.careertrack.service.JewelerProfileException;
import com.careertrack.model.User;
import java.util.Optional;
import org.springframework.security.core.userdetails.UserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerTest {
  @Test void invalidCredentialsReturnUsefulUnauthorizedResponse() throws Exception {
    var manager = mock(AuthenticationManager.class);
    var profiles = mock(JewelerProfileService.class);
    when(manager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));
    var controller = new AuthController(mock(UserRepository.class), mock(PasswordEncoder.class),
        manager, mock(CustomUserDetailsService.class), mock(JwtService.class), profiles);
    MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new ApiExceptionHandler()).build()
        .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"test@example.com\",\"password\":\"wrong\",\"storeName\":\"Test Jewellers\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Email or password is incorrect."));
    verifyNoInteractions(profiles);
  }

  @Test void missingStoreNameIsRejectedBeforeAuthentication() throws Exception {
    var manager = mock(AuthenticationManager.class);
    var profiles = mock(JewelerProfileService.class);
    var controller = new AuthController(mock(UserRepository.class), mock(PasswordEncoder.class),
        manager, mock(CustomUserDetailsService.class), mock(JwtService.class), profiles);
    MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new ApiExceptionHandler()).build()
        .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"test@example.com\",\"password\":\"password\"}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(manager, profiles);
  }

  @Test void loginCreatesProfileAndFailureCanBeRetried() throws Exception {
    var repository = mock(UserRepository.class);
    var manager = mock(AuthenticationManager.class);
    var profiles = mock(JewelerProfileService.class);
    var detailsService = mock(CustomUserDetailsService.class);
    var jwt = mock(JwtService.class);
    var user = new User();
    user.setId("user-123");
    user.setEmail("test@example.com");
    user.setName("Test Owner");
    when(repository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    var details = mock(UserDetails.class);
    when(detailsService.loadUserByUsername(user.getEmail())).thenReturn(details);
    when(jwt.generateToken(details)).thenReturn("test-token");
    doThrow(new JewelerProfileException("Please retry profile creation.")).doNothing()
        .when(profiles).ensureProfile(user, "Test Jewellers");
    var controller = new AuthController(repository, mock(PasswordEncoder.class), manager, detailsService, jwt, profiles);
    var mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new ApiExceptionHandler()).build();
    String payload = "{\"email\":\"test@example.com\",\"password\":\"password\",\"storeName\":\"  Test Jewellers  \"}";
    mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(payload))
        .andExpect(status().isBadGateway()).andExpect(jsonPath("$.message").value("Please retry profile creation."));
    verifyNoInteractions(jwt);
    mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(payload))
        .andExpect(status().isOk()).andExpect(jsonPath("$.token").value("test-token"));
    var order = inOrder(manager, profiles);
    order.verify(manager).authenticate(any());
    order.verify(profiles).ensureProfile(user, "Test Jewellers");
  }
}
