package com.fiapx.auth.gateways;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fiapx.auth.drivers.persistence.UserJpaRepository;
import com.fiapx.auth.entities.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** A adaptação é delegação pura, mas trocar um método pelo outro passaria despercebido. */
class JpaUserRepositoryTest {

  private final UserJpaRepository jpa = mock(UserJpaRepository.class);
  private final JpaUserRepository gateway = new JpaUserRepository(jpa);

  @Test
  void procuraPeloEmailNoSpringData() {
    User ana = new User("Ana", "ana@example.com", "hash");
    when(jpa.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(ana));

    assertSame(ana, gateway.findByEmailIgnoreCase("ana@example.com").orElseThrow());
  }

  @Test
  void devolveVazioQuandoOEmailNaoExiste() {
    when(jpa.findByEmailIgnoreCase("ninguem@example.com")).thenReturn(Optional.empty());

    assertTrue(gateway.findByEmailIgnoreCase("ninguem@example.com").isEmpty());
  }

  @Test
  void gravaOUsuarioEDevolveOQueFoiPersistido() {
    User ana = new User("Ana", "ana@example.com", "hash");
    when(jpa.save(ana)).thenReturn(ana);

    assertEquals("ana@example.com", gateway.save(ana).getEmail());
    verify(jpa).save(ana);
  }
}
