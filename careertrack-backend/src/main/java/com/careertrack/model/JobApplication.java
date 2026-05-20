package com.careertrack.model;

import java.time.LocalDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "applications")
public class JobApplication {
  @Id
  private String id;
  private String userId;
  private String companyName;
  private String jobTitle;
  private String location;
  private String jobType;
  private ApplicationStatus status;
  private LocalDate appliedDate;
  private String notes;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getUserId() { return userId; }
  public void setUserId(String userId) { this.userId = userId; }
  public String getCompanyName() { return companyName; }
  public void setCompanyName(String companyName) { this.companyName = companyName; }
  public String getJobTitle() { return jobTitle; }
  public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
  public String getLocation() { return location; }
  public void setLocation(String location) { this.location = location; }
  public String getJobType() { return jobType; }
  public void setJobType(String jobType) { this.jobType = jobType; }
  public ApplicationStatus getStatus() { return status; }
  public void setStatus(ApplicationStatus status) { this.status = status; }
  public LocalDate getAppliedDate() { return appliedDate; }
  public void setAppliedDate(LocalDate appliedDate) { this.appliedDate = appliedDate; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
}
