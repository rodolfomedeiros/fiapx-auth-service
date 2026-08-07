package com.fiapx.auth.gateways;

import com.fiapx.auth.usecases.ports.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Implementa a porta de senha com BCrypt. */
public class BCryptPasswordHasher implements PasswordHasher {

  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  /**
   * Calculado uma única vez na subida: gerar um hash BCrypt a cada login sem conta custaria
   * mais do que a comparação que ele existe para disfarçar, e o próprio custo entregaria o
   * caso.
   */
  private final String absentUserHash = encoder.encode("absent-user-placeholder");

  @Override
  public String hash(String rawPassword) {
    return encoder.encode(rawPassword);
  }

  @Override
  public boolean matches(String rawPassword, String passwordHash) {
    return encoder.matches(rawPassword, passwordHash);
  }

  @Override
  public String absentUserHash() {
    return absentUserHash;
  }
}
