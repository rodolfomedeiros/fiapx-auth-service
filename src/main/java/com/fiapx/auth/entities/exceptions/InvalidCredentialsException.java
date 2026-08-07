package com.fiapx.auth.entities.exceptions;

/**
 * Login recusado.
 *
 * <p>Não distingue e-mail inexistente de senha errada: a diferença diria a um atacante
 * quais endereços estão cadastrados.
 */
public class InvalidCredentialsException extends RuntimeException {

  public InvalidCredentialsException() {
    super("Credenciais inválidas");
  }
}
