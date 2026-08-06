package com.fiapx.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
  private static final String SECRET = "local-development-secret-must-have-at-least-32-bytes";
  private static final UUID SUBJECT = UUID.fromString("3f2504e0-4f89-11d3-9a0c-0305e82c3301");

  private JwtService service(long expirationMinutes) { return new JwtService(SECRET, expirationMinutes); }

  @Test void carregaSubjectEClaimsDoUsuarioNoTokenEmitido() {
    var claims = service(60).parse(service(60).create(SUBJECT, "ana@example.com", "Ana"));
    assertEquals(SUBJECT.toString(), claims.getSubject());
    assertEquals("ana@example.com", claims.get("email", String.class));
    assertEquals("Ana", claims.get("name", String.class));
  }

  @Test void rejeitaTokenExpirado() {
    var expirado = service(-1).create(SUBJECT, "ana@example.com", "Ana");
    assertThrows(io.jsonwebtoken.ExpiredJwtException.class, () -> service(60).parse(expirado));
  }

  @Test void rejeitaTokenAssinadoComOutraChave() {
    var deOutroEmissor = new JwtService("outro-segredo-igualmente-longo-com-32-bytes-ok", 60).create(SUBJECT, "ana@example.com", "Ana");
    assertThrows(io.jsonwebtoken.security.SignatureException.class, () -> service(60).parse(deOutroEmissor));
  }

  @Test void rejeitaTokenComPayloadAdulterado() {
    var partes = service(60).create(SUBJECT, "ana@example.com", "Ana").split("\\.");
    var adulterado = partes[0] + "." + java.util.Base64.getUrlEncoder().withoutPadding()
        .encodeToString("{\"sub\":\"outro-usuario\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8)) + "." + partes[2];
    assertThrows(io.jsonwebtoken.JwtException.class, () -> service(60).parse(adulterado));
  }

  @Test void rejeitaTokenMalformado() {
    assertThrows(io.jsonwebtoken.JwtException.class, () -> service(60).parse("nao-e-um-jwt"));
  }
}
