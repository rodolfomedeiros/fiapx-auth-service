package com.fiapx.auth.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fiapx.auth.entities.TokenClaims;
import com.fiapx.auth.entities.User;
import com.fiapx.auth.gateways.JwtTokenGateway;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class IntrospectTokenHandlerTest {

  private static final String SECRET = "local-development-secret-must-have-at-least-32-bytes";
  private static final UUID ID = UUID.fromString("3f2504e0-4f89-11d3-9a0c-0305e82c3301");

  private final JwtTokenGateway tokens = new JwtTokenGateway(SECRET, 60);
  private final IntrospectTokenHandler handler = new IntrospectTokenHandler(tokens);

  private static User ana() {
    User user = new User("Ana", "ana@example.com", "hash");
    ReflectionTestUtils.setField(user, "id", ID);
    return user;
  }

  @Test
  void devolveAsClaimsDeUmTokenValido() {
    Optional<TokenClaims> claims = handler.handle("Bearer " + tokens.issue(ana()));

    assertTrue(claims.isPresent());
    assertEquals(ID, claims.get().subject());
    assertEquals("ana@example.com", claims.get().email());
    assertEquals("Ana", claims.get().name());
  }

  @Test
  void naoAceitaCabecalhoAusente() {
    assertTrue(handler.handle(null).isEmpty());
  }

  @Test
  void naoAceitaCabecalhoForaDoEsquemaBearer() {
    assertTrue(handler.handle(tokens.issue(ana())).isEmpty(), "token cru, sem o prefixo");
    assertTrue(handler.handle("Basic YWRtaW46YWRtaW4=").isEmpty());
  }

  @Test
  void naoAceitaTokenQueNaoValida() {
    assertTrue(handler.handle("Bearer nao-e-um-jwt").isEmpty());
  }

  @Test
  void naoAceitaTokenExpirado() {
    String expirado = new JwtTokenGateway(SECRET, -1).issue(ana());

    assertTrue(handler.handle("Bearer " + expirado).isEmpty());
  }
}
