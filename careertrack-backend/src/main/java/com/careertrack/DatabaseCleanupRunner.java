package com.careertrack;

import java.util.HashSet;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.careertrack.model.User;
import com.careertrack.repository.UserRepository;

@Component
public class DatabaseCleanupRunner implements CommandLineRunner {
  private final UserRepository userRepository;

  public DatabaseCleanupRunner(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public void run(String... args) {
    try {
      Set<String> seen = new HashSet<>();
      for (User user : userRepository.findAll()) {
        if (user.getEmail() == null) continue;
        String email = user.getEmail().trim().toLowerCase();
        if (!seen.add(email)) {
          userRepository.deleteById(user.getId());
        }
      }
    } catch (Exception ignored) {
    }
  }
}
