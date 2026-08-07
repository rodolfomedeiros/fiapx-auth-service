package com.fiapx.auth.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Usuário cadastrado.
 *
 * <p>Ao contrário do gear-up, o domínio e o mapeamento vivem na mesma classe. Separá-los
 * exigiria um mapper para quatro campos que nunca divergem do schema, e o custo não se paga
 * neste serviço.
 */
@Entity
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "created_at")
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at")
  private Instant updatedAt = Instant.now();

  protected User() {
    // exigido pelo JPA
  }

  public User(String name, String email, String passwordHash) {
    this.name = Objects.requireNonNull(name, "nome é obrigatório");
    this.email = normalizeEmail(email);
    this.passwordHash = Objects.requireNonNull(passwordHash, "hash da senha é obrigatório");
  }

  /**
   * O e-mail identifica a conta, então é sempre guardado e comparado em minúsculas — do
   * contrário, "Ana@example.com" e "ana@example.com" seriam duas contas.
   */
  public static String normalizeEmail(String email) {
    return Objects.requireNonNull(email, "e-mail é obrigatório").trim().toLowerCase(Locale.ROOT);
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
