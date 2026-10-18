package com.careertrack.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthFilterTest {
  private final JwtService jwt = mock(JwtService.class);
  private final CustomUserDetailsService users = mock(CustomUserDetailsService.class);
  private final JwtAuthFilter filter = new JwtAuthFilter(jwt, users);
  private final FilterChain chain = mock(FilterChain.class);

  @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

  @Test void expiredTokenReturnsUnauthorized() throws Exception {
    var request = request("/api/applications");
    var response = new MockHttpServletResponse();
    when(jwt.extractUsername("old-token")).thenThrow(mock(ExpiredJwtException.class));
    filter.doFilter(request, response, chain);
    assertEquals(401, response.getStatus());
    verifyNoInteractions(chain);
  }

  @Test void oldTokenDoesNotBlockLogin() throws Exception {
    var request = request("/api/auth/login");
    var response = new MockHttpServletResponse();
    filter.doFilter(request, response, chain);
    verify(chain).doFilter(request, response);
    verifyNoInteractions(jwt, users);
  }

  @Test void preflightSkipsTokenParsing() throws Exception {
    var request = request("/api/applications");
    request.setMethod("OPTIONS");
    var response = new MockHttpServletResponse();
    filter.doFilter(request, response, chain);
    verify(chain).doFilter(request, response);
    verifyNoInteractions(jwt, users);
  }

  @Test void deletedUserReturnsUnauthorized() throws Exception {
    var request = request("/api/applications");
    var response = new MockHttpServletResponse();
    when(jwt.extractUsername("old-token")).thenReturn("deleted@example.com");
    when(users.loadUserByUsername("deleted@example.com"))
        .thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("missing"));
    filter.doFilter(request, response, chain);
    assertEquals(401, response.getStatus());
    verifyNoInteractions(chain);
  }

  private MockHttpServletRequest request(String path) {
    var request = new MockHttpServletRequest("GET", path);
    request.setServletPath(path);
    request.addHeader("Authorization", "Bearer old-token");
    return request;
  }
}
