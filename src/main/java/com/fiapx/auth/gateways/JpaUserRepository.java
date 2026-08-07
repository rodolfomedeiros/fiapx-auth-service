package com.fiapx.auth.gateways;

import com.fiapx.auth.drivers.persistence.UserJpaRepository;
import com.fiapx.auth.entities.User;
import com.fiapx.auth.usecases.ports.UserRepository;
import java.util.Optional;

/**
 * Liga a porta de persistência ao Spring Data.
 *
 * <p>É uma classe, e não uma interface estendendo as duas, porque {@code JpaRepository}
 * declara {@code <S extends T> S save(S)}: herdar de ambas colocaria duas assinaturas de
 * {@code save} diferentes na mesma interface.
 */
public class JpaUserRepository implements UserRepository {

  private final UserJpaRepository jpa;

  public JpaUserRepository(UserJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<User> findByEmailIgnoreCase(String email) {
    return jpa.findByEmailIgnoreCase(email);
  }

  @Override
  public User save(User user) {
    return jpa.save(user);
  }
}
