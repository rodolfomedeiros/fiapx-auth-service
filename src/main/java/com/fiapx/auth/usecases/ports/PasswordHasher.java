package com.fiapx.auth.usecases.ports;

/** Porta de saída para o algoritmo de hash de senha. */
public interface PasswordHasher {

  String hash(String rawPassword);

  boolean matches(String rawPassword, String passwordHash);

  /**
   * Hash de um valor fixo, para comparar quando a conta não existe.
   *
   * <p>Está na porta porque a defesa é do caso de uso, não do algoritmo: sem comparar
   * contra algo, o login responderia mais rápido para e-mails não cadastrados e revelaria
   * quais endereços têm conta.
   */
  String absentUserHash();
}
