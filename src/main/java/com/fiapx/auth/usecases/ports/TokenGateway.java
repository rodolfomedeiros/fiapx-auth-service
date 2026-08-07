package com.fiapx.auth.usecases.ports;

import com.fiapx.auth.entities.TokenClaims;
import com.fiapx.auth.entities.User;
import java.util.Optional;

/** Porta de saída para emissão e leitura de tokens de acesso. */
public interface TokenGateway {

  String issue(User user);

  /** Vazio para token malformado, expirado ou assinado por outra chave. */
  Optional<TokenClaims> read(String token);

  /** Validade do token emitido, para o cliente saber quando renovar. */
  long expiresInSeconds();
}
