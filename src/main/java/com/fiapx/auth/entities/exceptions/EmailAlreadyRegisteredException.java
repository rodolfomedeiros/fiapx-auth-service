package com.fiapx.auth.entities.exceptions;

/** Cadastro recusado porque o e-mail já pertence a uma conta. */
public class EmailAlreadyRegisteredException extends RuntimeException {

  public EmailAlreadyRegisteredException() {
    super("E-mail já cadastrado");
  }
}
