package com.fiapx.auth;

import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/v1/auth")
public class AuthController {
  private final UserRepository users; private final JwtService jwt; private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
  AuthController(UserRepository users, JwtService jwt) { this.users=users; this.jwt=jwt; }
  record RegisterRequest(@NotBlank @Size(max=100) String name, @Email @NotBlank String email, @NotBlank @Size(min=8,max=128) String password) {}
  record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
  record TokenResponse(String access_token, String token_type, long expires_in) {}
  @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
  Map<String,Object> register(@Valid @RequestBody RegisterRequest request) {
    if (users.findByEmailIgnoreCase(request.email()).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT,"E-mail já cadastrado");
    User user=users.save(new User(request.name(), request.email().toLowerCase(), encoder.encode(request.password())));
    return Map.of("id",user.getId(),"name",user.getName(),"email",user.getEmail());
  }
  @PostMapping("/login") TokenResponse login(@Valid @RequestBody LoginRequest request) {
    User user=users.findByEmailIgnoreCase(request.email()).filter(u -> encoder.matches(request.password(),u.getPasswordHash())).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Credenciais inválidas"));
    return new TokenResponse(jwt.create(user),"Bearer",3600);
  }
  @PostMapping("/introspect") Map<String,Object> introspect(@RequestHeader(value="Authorization",required=false) String authorization) {
    try { if (authorization==null || !authorization.startsWith("Bearer ")) throw new IllegalArgumentException(); Claims c=jwt.parse(authorization.substring(7)); return Map.of("active",true,"sub",c.getSubject(),"email",c.get("email",String.class),"name",c.get("name",String.class)); }
    catch (Exception ignored) { return Map.of("active",false); }
  }
}
