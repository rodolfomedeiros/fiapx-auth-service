package com.fiapx.auth;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "users")
public class User {
  @Id @GeneratedValue private UUID id;
  @Column(nullable = false) private String name;
  @Column(nullable = false, unique = true) private String email;
  @Column(name = "password_hash", nullable = false) private String passwordHash;
  @Column(name = "created_at") private Instant createdAt = Instant.now();
  @Column(name = "updated_at") private Instant updatedAt = Instant.now();
  protected User() {}
  User(String name, String email, String passwordHash) { this.name=name; this.email=email; this.passwordHash=passwordHash; }
  public UUID getId() { return id; } public String getName() { return name; }
  public String getEmail() { return email; } public String getPasswordHash() { return passwordHash; }
}
