package com.fiapx.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
class JwtService {
  private final SecretKey key; private final long expirationMinutes;
  JwtService(@Value("${app.jwt-secret}") String secret, @Value("${app.jwt-expiration-minutes}") long expirationMinutes) {
    this.key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); this.expirationMinutes=expirationMinutes;
  }
  String create(UUID subject, String email, String name) { Instant now=Instant.now(); return Jwts.builder().subject(subject.toString()).claim("email", email).claim("name", name).issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(expirationMinutes*60))).signWith(key).compact(); }
  Claims parse(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
}
