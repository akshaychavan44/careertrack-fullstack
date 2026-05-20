package com.careertrack.dto;

import java.time.LocalDateTime;

public class InterviewRequest {
  private String applicationId;
  private String round;
  private LocalDateTime interviewDate;
  private String mode;
  private String notes;

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
