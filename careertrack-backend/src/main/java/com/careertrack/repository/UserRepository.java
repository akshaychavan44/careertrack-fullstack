package com.careertrack.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.careertrack.model.User;

public interface UserRepository extends MongoRepository<User, String> {
  Optional<User> findFirstByEmail(String email);
  List<User> findAllByEmail(String email);
  boolean existsByEmail(String email);

  default Optional<User> findByEmail(String email) {
    return findFirstByEmail(email);
  }
}
