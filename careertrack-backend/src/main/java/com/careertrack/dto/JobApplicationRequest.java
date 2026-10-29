package com.careertrack.dto;

import java.time.LocalDate;
import com.careertrack.model.ApplicationStatus;
import jakarta.validation.constraints.NotBlank;

public class JobApplicationRequest {
  @NotBlank
  private String companyName;
  @NotBlank
  private String jobTitle;
  private String location;
  private String jobType;
  private ApplicationStatus status;
  private LocalDate appliedDate;
  private String notes;

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
