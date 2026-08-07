package com.fiapx.auth.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fiapx.auth.entities.User;
import com.fiapx.auth.gateways.BCryptPasswordHasher;
import com.fiapx.auth.gateways.JwtTokenGateway;
import com.fiapx.auth.shared.exceptions.GlobalExceptionHandler;
import com.fiapx.auth.usecases.AuthenticateUserHandler;
import com.fiapx.auth.usecases.IntrospectTokenHandler;
import com.fiapx.auth.usecases.RegisterUserHandler;
import com.fiapx.auth.usecases.ports.PasswordHasher;
import com.fiapx.auth.usecases.ports.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Cobre a tradução HTTP: status, formato da resposta e validação de entrada. */
class AuthControllerTest {

  private static final String SECRET = "local-development-secret-must-have-at-least-32-bytes";
  private static final UUID ID = UUID.fromString("3f2504e0-4f89-11d3-9a0c-0305e82c3301");

  private UserRepository users;
  private JwtTokenGateway tokens;
  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    users = mock(UserRepository.class);
    tokens = new JwtTokenGateway(SECRET, 60);
    PasswordHasher hasher = new BCryptPasswordHasher();

    var controller =
        new AuthController(
            new RegisterUserHandler(users, hasher),
            new AuthenticateUserHandler(users, hasher, tokens),
            new IntrospectTokenHandler(tokens));

    mvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  private static User persistedUser(String senha) {
    var user = new User("Ana", "ana@example.com", new BCryptPasswordHasher().hash(senha));
    ReflectionTestUtils.setField(user, "id", ID);
    return user;
  }

  private static String credentials(String email, String senha) {
    return "{\"email\":\"" + email + "\",\"password\":\"" + senha + "\"}";
  }

  @Test
  void registraUsuarioNovoEDevolveOsDadosPublicos() throws Exception {
    when(users.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
    when(users.save(any(User.class)))
        .thenAnswer(
            invocation -> {
              User salvo = invocation.getArgument(0);
              ReflectionTestUtils.setField(salvo, "id", ID);
              return salvo;
            });

    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Ana\",\"email\":\"Ana@Example.COM\","
                        + "\"password\":\"senha-bem-longa\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(ID.toString()))
        .andExpect(jsonPath("$.email").value("ana@example.com"))
        .andExpect(jsonPath("$.password").doesNotExist());
  }

  @Test
  void recusaCadastroComEmailJaExistenteComo409() throws Exception {
    when(users.findByEmailIgnoreCase("ana@example.com"))
        .thenReturn(Optional.of(persistedUser("senha-bem-longa")));

    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Ana\",\"email\":\"ana@example.com\","
                        + "\"password\":\"senha-bem-longa\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  void recusaCadastroComSenhaCurtaOuEmailInvalido() throws Exception {
    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Ana\",\"email\":\"ana@example.com\",\"password\":\"curta\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Ana\",\"email\":\"sem-arroba\","
                        + "\"password\":\"senha-bem-longa\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void emiteTokenParaCredenciaisValidas() throws Exception {
    when(users.findByEmailIgnoreCase("ana@example.com"))
        .thenReturn(Optional.of(persistedUser("senha-bem-longa")));

    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials("ana@example.com", "senha-bem-longa")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token_type").value("Bearer"))
        .andExpect(jsonPath("$.access_token").isNotEmpty())
        .andExpect(jsonPath("$.expires_in").value(3600));
  }

  @Test
  void recusaLoginComSenhaErradaComo401() throws Exception {
    when(users.findByEmailIgnoreCase("ana@example.com"))
        .thenReturn(Optional.of(persistedUser("senha-bem-longa")));

    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials("ana@example.com", "senha-errada-longa")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void recusaLoginDeEmailInexistenteComo401() throws Exception {
    when(users.findByEmailIgnoreCase("ninguem@example.com")).thenReturn(Optional.empty());

    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials("ninguem@example.com", "senha-bem-longa")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void introspeccaoDevolveClaimsDeTokenValido() throws Exception {
    var token = tokens.issue(persistedUser("senha-bem-longa"));

    mvc.perform(post("/api/v1/auth/introspect").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(true))
        .andExpect(jsonPath("$.sub").value(ID.toString()))
        .andExpect(jsonPath("$.email").value("ana@example.com"))
        .andExpect(jsonPath("$.name").value("Ana"));
  }

  @Test
  void introspeccaoMarcaComoInativoQuandoOTokenNaoServe() throws Exception {
    mvc.perform(post("/api/v1/auth/introspect"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false))
        .andExpect(jsonPath("$.sub").doesNotExist());
    mvc.perform(post("/api/v1/auth/introspect").header("Authorization", "Bearer nao-e-um-jwt"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));
    mvc.perform(
            post("/api/v1/auth/introspect")
                .header("Authorization", tokens.issue(persistedUser("senha-bem-longa"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));
  }
}
