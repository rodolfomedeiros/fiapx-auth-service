package com.fiapx.auth.usecases;

import com.fiapx.auth.entities.User;
import com.fiapx.auth.entities.exceptions.InvalidCredentialsException;
import com.fiapx.auth.usecases.ports.PasswordHasher;
import com.fiapx.auth.usecases.ports.TokenGateway;
import com.fiapx.auth.usecases.ports.UserRepository;
import java.util.Optional;

/** Valida credenciais e emite o token de acesso. */
public class AuthenticateUserHandler {

  private final UserRepository users;
  private final PasswordHasher hasher;
  private final TokenGateway tokens;

  public AuthenticateUserHandler(UserRepository users, PasswordHasher hasher, TokenGateway tokens) {
    this.users = users;
    this.hasher = hasher;
    this.tokens = tokens;
  }

  public String handle(String email, String rawPassword) {
    Optional<User> found = users.findByEmailIgnoreCase(User.normalizeEmail(email));

    // A comparação acontece mesmo sem conta, contra um hash descartável: sair antes faria
    // o login responder mais rápido para e-mails não cadastrados, e o tempo de resposta
    // entregaria quais endereços existem.
    String hash = found.map(User::getPasswordHash).orElseGet(hasher::absentUserHash);
    boolean matches = hasher.matches(rawPassword, hash);

    if (found.isEmpty() || !matches) {
      throw new InvalidCredentialsException();
    }
    return tokens.issue(found.get());
  }

  public long expiresInSeconds() {
    return tokens.expiresInSeconds();
  }
}
