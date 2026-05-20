package com.careertrack.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.careertrack.model.Interview;

public interface InterviewRepository extends MongoRepository<Interview, String> {
  List<Interview> findByUserIdOrderByInterviewDateAsc(String userId);
  List<Interview> findByUserIdAndApplicationId(String userId, String applicationId);
}
