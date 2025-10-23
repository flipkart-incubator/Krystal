package com.flipkart.krystal.vajram.samples.sql;

public class User {
  private Integer id;
  private String name;
  private String emailId;

  public User() {}

  public User(Integer id, String name, String email) {
    this.id = id;
    this.name = name;
    this.emailId = email;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmailId() {
    return emailId;
  }

  public void setEmailId(String email) {
    this.emailId = email;
  }

  @Override
  public String toString() {
    return "User{" + "id=" + id + ", name='" + name + '\'' + ", email='" + emailId + '\'' + '}';
  }
}
