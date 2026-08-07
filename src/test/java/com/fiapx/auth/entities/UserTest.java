package com.fiapx.auth.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class UserTest {

  @Test
  void normalizaOEmailParaMinusculasSemEspacos() {
    assertEquals("ana@example.com", User.normalizeEmail("  Ana@Example.COM  "));
  }

  @Test
  void oConstrutorJaGuardaOEmailNormalizado() {
    assertEquals("ana@example.com", new User("Ana", "ANA@EXAMPLE.COM", "hash").getEmail());
  }

  @Test
  void recusaCamposObrigatoriosNulos() {
    assertThrows(NullPointerException.class, () -> new User(null, "ana@example.com", "hash"));
    assertThrows(NullPointerException.class, () -> new User("Ana", null, "hash"));
    assertThrows(NullPointerException.class, () -> new User("Ana", "ana@example.com", null));
  }
}
