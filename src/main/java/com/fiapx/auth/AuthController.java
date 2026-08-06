package com.fiapx.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
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
  record RegisteredUser(UUID id, String name, String email) {}
  @JsonInclude(JsonInclude.Include.NON_NULL)
  record IntrospectionResponse(boolean active, String sub, String email, String name) {
    static IntrospectionResponse inactive() { return new IntrospectionResponse(false, null, null, null); }
  }
  /** Hash descartável comparado quando o e-mail não existe, para que o tempo de resposta não revele contas cadastradas. */
  private static final String ABSENT_USER_HASH = new BCryptPasswordEncoder().encode("absent-user-placeholder");
  @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
  RegisteredUser register(@Valid @RequestBody RegisterRequest request) {
    if (users.findByEmailIgnoreCase(request.email()).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT,"E-mail já cadastrado");
    User user=users.save(new User(request.name(), request.email().toLowerCase(), encoder.encode(request.password())));
    return new RegisteredUser(user.getId(),user.getName(),user.getEmail());
  }
  @PostMapping("/login") TokenResponse login(@Valid @RequestBody LoginRequest request) {
    User user=users.findByEmailIgnoreCase(request.email()).orElse(null);
    boolean matches=encoder.matches(request.password(), user!=null ? user.getPasswordHash() : ABSENT_USER_HASH);
    if (user==null || !matches) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Credenciais inválidas");
    return new TokenResponse(jwt.create(user.getId(),user.getEmail(),user.getName()),"Bearer",3600);
  }
  @PostMapping("/introspect") IntrospectionResponse introspect(@RequestHeader(value="Authorization",required=false) String authorization) {
    if (authorization==null || !authorization.startsWith("Bearer ")) return IntrospectionResponse.inactive();
    try { Claims c=jwt.parse(authorization.substring(7)); return new IntrospectionResponse(true,c.getSubject(),c.get("email",String.class),c.get("name",String.class)); }
    catch (Exception ignored) { return IntrospectionResponse.inactive(); }
  }
}
