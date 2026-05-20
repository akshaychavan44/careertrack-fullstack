package com.careertrack.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.careertrack.model.ApplicationStatus;
import com.careertrack.model.JobApplication;

public interface JobApplicationRepository extends MongoRepository<JobApplication, String> {
  List<JobApplication> findByUserIdOrderByAppliedDateDesc(String userId);
  List<JobApplication> findByUserIdAndStatus(String userId, ApplicationStatus status);
  long countByUserId(String userId);
  long countByUserIdAndStatus(String userId, ApplicationStatus status);
}
