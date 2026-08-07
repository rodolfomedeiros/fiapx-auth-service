package com.fiapx.auth.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fiapx.auth.entities.User;
import com.fiapx.auth.entities.exceptions.EmailAlreadyRegisteredException;
import com.fiapx.auth.gateways.BCryptPasswordHasher;
import com.fiapx.auth.usecases.ports.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RegisterUserHandlerTest {

  private UserRepository users;
  private RegisterUserHandler handler;

  @BeforeEach
  void setUp() {
    users = mock(UserRepository.class);
    handler = new RegisterUserHandler(users, new BCryptPasswordHasher());
    when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void normalizaOEmailAntesDeProcurarEDeGravar() {
    when(users.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

    User registrado = handler.handle("Ana", "  Ana@Example.COM  ", "senha-bem-longa");

    verify(users).findByEmailIgnoreCase("ana@example.com");
    assertEquals("ana@example.com", registrado.getEmail());
  }

  @Test
  void guardaASenhaApenasComoHash() {
    when(users.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

    handler.handle("Ana", "ana@example.com", "senha-bem-longa");

    ArgumentCaptor<User> salvo = ArgumentCaptor.forClass(User.class);
    verify(users).save(salvo.capture());
    String hash = salvo.getValue().getPasswordHash();
    assertFalse(hash.contains("senha-bem-longa"), "o hash não pode conter a senha em claro");
    assertTrue(
        new BCryptPasswordHasher().matches("senha-bem-longa", hash),
        "o hash precisa validar a senha original");
  }

  @Test
  void recusaEmailJaCadastradoSemGravarNada() {
    when(users.findByEmailIgnoreCase("ana@example.com"))
        .thenReturn(Optional.of(new User("Ana", "ana@example.com", "hash")));

    assertThrows(
        EmailAlreadyRegisteredException.class,
        () -> handler.handle("Ana", "ana@example.com", "senha-bem-longa"));
    verify(users, never()).save(any(User.class));
  }

  @Test
  void trataMaiusculasComoAMesmaConta() {
    when(users.findByEmailIgnoreCase("ana@example.com"))
        .thenReturn(Optional.of(new User("Ana", "ana@example.com", "hash")));

    assertThrows(
        EmailAlreadyRegisteredException.class,
        () -> handler.handle("Ana", "ANA@EXAMPLE.COM", "senha-bem-longa"));
  }
}
