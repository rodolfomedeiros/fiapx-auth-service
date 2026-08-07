package com.fiapx.auth.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fiapx.auth.entities.User;
import com.fiapx.auth.entities.exceptions.InvalidCredentialsException;
import com.fiapx.auth.gateways.JwtTokenGateway;
import com.fiapx.auth.usecases.ports.PasswordHasher;
import com.fiapx.auth.usecases.ports.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AuthenticateUserHandlerTest {

  private static final String SECRET = "local-development-secret-must-have-at-least-32-bytes";

  private UserRepository users;
  private PasswordHasher hasher;
  private AuthenticateUserHandler handler;

  @BeforeEach
  void setUp() {
    users = mock(UserRepository.class);
    hasher = mock(PasswordHasher.class);
    when(hasher.absentUserHash()).thenReturn("hash-descartavel");
    handler = new AuthenticateUserHandler(users, hasher, new JwtTokenGateway(SECRET, 60));
  }

  private static User cadastrada() {
    User user = new User("Ana", "ana@example.com", "hash-da-ana");
    ReflectionTestUtils.setField(
        user, "id", java.util.UUID.fromString("3f2504e0-4f89-11d3-9a0c-0305e82c3301"));
    return user;
  }

  @Test
  void emiteTokenParaCredenciaisValidas() {
    when(users.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(cadastrada()));
    when(hasher.matches("senha-bem-longa", "hash-da-ana")).thenReturn(true);

    assertNotNull(handler.handle("ana@example.com", "senha-bem-longa"));
  }

  @Test
  void recusaSenhaErrada() {
    when(users.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(cadastrada()));
    when(hasher.matches(anyString(), anyString())).thenReturn(false);

    assertThrows(
        InvalidCredentialsException.class, () -> handler.handle("ana@example.com", "errada"));
  }

  @Test
  void recusaEmailInexistente() {
    when(users.findByEmailIgnoreCase("ninguem@example.com")).thenReturn(Optional.empty());

    assertThrows(
        InvalidCredentialsException.class,
        () -> handler.handle("ninguem@example.com", "senha-bem-longa"));
  }

  @Test
  void comparaContraUmHashDescartavelQuandoAContaNaoExiste() {
    when(users.findByEmailIgnoreCase("ninguem@example.com")).thenReturn(Optional.empty());

    assertThrows(
        InvalidCredentialsException.class,
        () -> handler.handle("ninguem@example.com", "senha-bem-longa"));

    // O custo do BCrypt precisa ser pago também no caminho sem conta; pular a comparação
    // deixaria o tempo de resposta denunciar quais e-mails existem.
    verify(hasher, times(1)).matches("senha-bem-longa", "hash-descartavel");
  }

  @Test
  void normalizaOEmailAntesDeProcurar() {
    when(users.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(cadastrada()));
    when(hasher.matches("senha-bem-longa", "hash-da-ana")).thenReturn(true);

    handler.handle("ANA@Example.com ", "senha-bem-longa");

    verify(users).findByEmailIgnoreCase("ana@example.com");
  }

  @Test
  void informaAValidadeDoTokenVindaDaConfiguracao() {
    assertEquals(3600, handler.expiresInSeconds());
  }
}
