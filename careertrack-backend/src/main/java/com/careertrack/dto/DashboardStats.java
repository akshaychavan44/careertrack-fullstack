package com.careertrack.dto;

public class DashboardStats {
  private long total;
  private long applied;
  private long interview;
  private long offer;
  private long rejected;

  public DashboardStats(long total, long applied, long interview, long offer, long rejected) {
    this.total = total;
    this.applied = applied;
    this.interview = interview;
    this.offer = offer;
    this.rejected = rejected;
  }

  public long getTotal() { return total; }
  public long getApplied() { return applied; }
  public long getInterview() { return interview; }
  public long getOffer() { return offer; }
  public long getRejected() { return rejected; }
}
