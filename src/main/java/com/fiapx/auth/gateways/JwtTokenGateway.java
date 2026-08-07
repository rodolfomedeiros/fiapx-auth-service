package com.fiapx.auth.gateways;

import com.fiapx.auth.entities.TokenClaims;
import com.fiapx.auth.entities.User;
import com.fiapx.auth.usecases.ports.TokenGateway;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;

/** Implementa a porta de token com JWT assinado em HMAC-SHA. */
public class JwtTokenGateway implements TokenGateway {

  private final SecretKey key;
  private final long expirationMinutes;

  public JwtTokenGateway(String secret, long expirationMinutes) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMinutes = expirationMinutes;
  }

  @Override
  public String issue(User user) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(user.getId().toString())
        .claim("email", user.getEmail())
        .claim("name", user.getName())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(expiresInSeconds())))
        .signWith(key)
        .compact();
  }

  @Override
  public Optional<TokenClaims> read(String token) {
    try {
      Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
      return Optional.of(
          new TokenClaims(
              UUID.fromString(claims.getSubject()),
              claims.get("email", String.class),
              claims.get("name", String.class)));
    } catch (JwtException | IllegalArgumentException invalid) {
      // IllegalArgumentException cobre o token cujo subject não é um UUID, que é tão
      // inservível quanto um mal assinado.
      return Optional.empty();
    }
  }

  @Override
  public long expiresInSeconds() {
    return expirationMinutes * 60;
  }
}
