package com.fiapx.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
class JwtService {
  private final SecretKey key; private final long expirationMinutes;
  JwtService(@Value("${app.jwt-secret}") String secret, @Value("${app.jwt-expiration-minutes}") long expirationMinutes) {
    this.key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); this.expirationMinutes=expirationMinutes;
  }
  String create(User user) { Instant now=Instant.now(); return Jwts.builder().subject(user.getId().toString()).claim("email", user.getEmail()).claim("name", user.getName()).issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(expirationMinutes*60))).signWith(key).compact(); }
  Claims parse(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
}
