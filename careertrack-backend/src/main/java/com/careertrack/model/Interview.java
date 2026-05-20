package com.careertrack.model;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "interviews")
public class Interview {
  @Id
  private String id;
  private String userId;
  private String applicationId;
  private String round;
  private LocalDateTime interviewDate;
  private String mode;
  private String notes;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getUserId() { return userId; }
  public void setUserId(String userId) { this.userId = userId; }
  public String getApplicationId() { return applicationId; }
  public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
  public String getRound() { return round; }
  public void setRound(String round) { this.round = round; }
  public LocalDateTime getInterviewDate() { return interviewDate; }
  public void setInterviewDate(LocalDateTime interviewDate) { this.interviewDate = interviewDate; }
  public String getMode() { return mode; }
  public void setMode(String mode) { this.mode = mode; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
}
