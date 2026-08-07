package com.fiapx.auth.gateways;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fiapx.auth.entities.User;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenGatewayTest {

  private static final String SECRET = "local-development-secret-must-have-at-least-32-bytes";
  private static final UUID ID = UUID.fromString("3f2504e0-4f89-11d3-9a0c-0305e82c3301");

  private static JwtTokenGateway gateway(long expirationMinutes) {
    return new JwtTokenGateway(SECRET, expirationMinutes);
  }

  private static User ana() {
    User user = new User("Ana", "ana@example.com", "hash");
    ReflectionTestUtils.setField(user, "id", ID);
    return user;
  }

  @Test
  void carregaSubjectEClaimsDoUsuarioNoTokenEmitido() {
    var claims = gateway(60).read(gateway(60).issue(ana())).orElseThrow();

    assertEquals(ID, claims.subject());
    assertEquals("ana@example.com", claims.email());
    assertEquals("Ana", claims.name());
  }

  @Test
  void rejeitaTokenExpirado() {
    assertTrue(gateway(60).read(gateway(-1).issue(ana())).isEmpty());
  }

  @Test
  void rejeitaTokenAssinadoComOutraChave() {
    var deOutroEmissor =
        new JwtTokenGateway("outro-segredo-igualmente-longo-com-32-bytes-ok", 60).issue(ana());

    assertTrue(gateway(60).read(deOutroEmissor).isEmpty());
  }

  @Test
  void rejeitaTokenComPayloadAdulterado() {
    var partes = gateway(60).issue(ana()).split("\\.");
    var adulterado =
        partes[0]
            + "."
            + Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString("{\"sub\":\"outro-usuario\"}".getBytes(StandardCharsets.UTF_8))
            + "."
            + partes[2];

    assertTrue(gateway(60).read(adulterado).isEmpty());
  }

  @Test
  void rejeitaTokenMalformado() {
    assertTrue(gateway(60).read("nao-e-um-jwt").isEmpty());
  }

  @Test
  void converteAValidadeDeMinutosParaSegundos() {
    assertEquals(3600, gateway(60).expiresInSeconds());
    assertEquals(900, gateway(15).expiresInSeconds());
  }
}
