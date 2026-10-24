package com.careertrack.service;

import java.time.Duration;
import java.net.http.HttpClient;
import java.util.Map;
import com.careertrack.model.User;
import com.careertrack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class JewelerProfileService {
  private final UserRepository userRepository;
  private final RestClient client;
  private final String apiUrl;
  private final String apiKey;

  public JewelerProfileService(UserRepository userRepository, RestClient.Builder builder,
      @Value("${jeweler.api.url}") String apiUrl, @Value("${jeweler.api.key}") String apiKey) {
    this.userRepository = userRepository;
    this.apiUrl = apiUrl;
    this.apiKey = apiKey;
    var httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    var factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(Duration.ofSeconds(20));
    this.client = builder.requestFactory(factory).build();
  }

  // Re-read within the lock so registration retries share the saved result.
  public synchronized void ensureProfile(User authenticatedUser, String storeName) {
    User user = userRepository.findById(authenticatedUser.getId()).orElseThrow();
    if (user.isJewelerProfileCreated()) return;
    if (apiKey == null || apiKey.isBlank()) {
      throw new JewelerProfileException("Jeweller profile creation is not configured. Please contact the administrator.");
    }

    try {
      client.post().uri(apiUrl)
          .contentType(MediaType.APPLICATION_JSON)
          .headers(headers -> headers.setBearerAuth(apiKey))
          .body(Map.of("storeName", storeName, "email", user.getEmail()))
          .retrieve().toBodilessEntity();
    } catch (RestClientResponseException exception) {
      if (exception.getStatusCode().value() == 409) {
        throw new JewelerProfileException("A jeweller profile already exists for this email. Please contact the administrator to link it.");
      }
      throw new JewelerProfileException("Could not create your jeweller profile. Please try registering again shortly with the same email and password.");
    } catch (RestClientException exception) {
      throw new JewelerProfileException("Could not confirm jeweller profile creation. Please try registering again shortly with the same email and password.");
    }
    user.setStoreName(storeName);
    user.setJewelerProfileCreated(true);
    userRepository.save(user);
  }
}
