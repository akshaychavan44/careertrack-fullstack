package com.careertrack.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {
  @Id
  private String id;
  private String name;
  @Indexed(unique = true)
  private String email;
  private String password;
  private String storeName;
  private boolean jewelerProfileCreated;

  public String getStoreName() { return storeName; }
  public void setStoreName(String storeName) { this.storeName = storeName; }
  public boolean isJewelerProfileCreated() { return jewelerProfileCreated; }
  public void setJewelerProfileCreated(boolean jewelerProfileCreated) { this.jewelerProfileCreated = jewelerProfileCreated; }

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public String getPassword() { return password; }
  public void setPassword(String password) { this.password = password; }
}
