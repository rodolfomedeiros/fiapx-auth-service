package com.fiapx.auth.usecases;

import com.fiapx.auth.entities.User;
import com.fiapx.auth.entities.exceptions.EmailAlreadyRegisteredException;
import com.fiapx.auth.usecases.ports.PasswordHasher;
import com.fiapx.auth.usecases.ports.UserRepository;

/** Cadastra um usuário novo, recusando e-mail já em uso. */
public class RegisterUserHandler {

  private final UserRepository users;
  private final PasswordHasher hasher;

  public RegisterUserHandler(UserRepository users, PasswordHasher hasher) {
    this.users = users;
    this.hasher = hasher;
  }

  public User handle(String name, String email, String rawPassword) {
    String normalized = User.normalizeEmail(email);
    if (users.findByEmailIgnoreCase(normalized).isPresent()) {
      throw new EmailAlreadyRegisteredException();
    }
    return users.save(new User(name, normalized, hasher.hash(rawPassword)));
  }
}
