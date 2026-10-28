package com.careertrack.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
  @NotBlank
  private String name;
  @Email
  @NotBlank
  private String email;
  @NotBlank
  private String password;
  @Size(max = 200)
  private String storeName;

  public String getStoreName() { return storeName; }
  public void setStoreName(String storeName) { this.storeName = storeName; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public String getPassword() { return password; }
  public void setPassword(String password) { this.password = password; }
}
