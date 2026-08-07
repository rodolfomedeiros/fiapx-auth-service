package com.fiapx.auth.controllers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fiapx.auth.entities.User;
import com.fiapx.auth.usecases.AuthenticateUserHandler;
import com.fiapx.auth.usecases.IntrospectTokenHandler;
import com.fiapx.auth.usecases.RegisterUserHandler;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Cadastro, login e introspecção. Só traduz HTTP: as regras estão nos handlers. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final RegisterUserHandler registerUser;
  private final AuthenticateUserHandler authenticateUser;
  private final IntrospectTokenHandler introspectToken;

  public AuthController(
      RegisterUserHandler registerUser,
      AuthenticateUserHandler authenticateUser,
      IntrospectTokenHandler introspectToken) {
    this.registerUser = registerUser;
    this.authenticateUser = authenticateUser;
    this.introspectToken = introspectToken;
  }

  record RegisterRequest(
      @NotBlank @Size(max = 100) String name,
      @Email @NotBlank String email,
      @NotBlank @Size(min = 8, max = 128) String password) {}

  record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

  record TokenResponse(String access_token, String token_type, long expires_in) {}

  record RegisteredUser(UUID id, String name, String email) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  record IntrospectionResponse(boolean active, String sub, String email, String name) {

    static IntrospectionResponse inactive() {
      return new IntrospectionResponse(false, null, null, null);
    }
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  RegisteredUser register(@Valid @RequestBody RegisterRequest request) {
    User user = registerUser.handle(request.name(), request.email(), request.password());
    return new RegisteredUser(user.getId(), user.getName(), user.getEmail());
  }

  @PostMapping("/login")
  TokenResponse login(@Valid @RequestBody LoginRequest request) {
    String token = authenticateUser.handle(request.email(), request.password());
    return new TokenResponse(token, "Bearer", authenticateUser.expiresInSeconds());
  }

  @PostMapping("/introspect")
  IntrospectionResponse introspect(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return introspectToken
        .handle(authorization)
        .map(
            claims ->
                new IntrospectionResponse(
                    true, claims.subject().toString(), claims.email(), claims.name()))
        .orElseGet(IntrospectionResponse::inactive);
  }
}
