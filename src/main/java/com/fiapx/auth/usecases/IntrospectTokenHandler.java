package com.fiapx.auth.usecases;

import com.fiapx.auth.entities.TokenClaims;
import com.fiapx.auth.usecases.ports.TokenGateway;
import java.util.Optional;

/** Diz aos demais serviços se um token serve, e de quem ele é. */
public class IntrospectTokenHandler {

  private static final String BEARER_PREFIX = "Bearer ";

  private final TokenGateway tokens;

  public IntrospectTokenHandler(TokenGateway tokens) {
    this.tokens = tokens;
  }

  /**
   * Vazio para cabeçalho ausente, fora do esquema Bearer ou com token que não valida — o
   * chamador não precisa distinguir os casos, e detalhar o motivo só ajudaria quem estivesse
   * sondando.
   */
  public Optional<TokenClaims> handle(String authorizationHeader) {
    if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
      return Optional.empty();
    }
    return tokens.read(authorizationHeader.substring(BEARER_PREFIX.length()));
  }
}
