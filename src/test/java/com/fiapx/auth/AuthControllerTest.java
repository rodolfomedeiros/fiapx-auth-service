package com.fiapx.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerTest {
  private static final String SECRET = "local-development-secret-must-have-at-least-32-bytes";
  private static final UUID ID = UUID.fromString("3f2504e0-4f89-11d3-9a0c-0305e82c3301");

  private UserRepository users;
  private JwtService jwt;
  private MockMvc mvc;

  @BeforeEach void setUp() {
    users = mock(UserRepository.class);
    jwt = new JwtService(SECRET, 60);
    mvc = MockMvcBuilders.standaloneSetup(new AuthController(users, jwt)).build();
  }

  private static User persistedUser(String senha) {
    var user = new User("Ana", "ana@example.com", new BCryptPasswordEncoder().encode(senha));
    ReflectionTestUtils.setField(user, "id", ID);
    return user;
  }

  private static String credentials(String email, String senha) {
    return "{\"email\":\"" + email + "\",\"password\":\"" + senha + "\"}";
  }

  @Test void registraUsuarioNovoComEmailNormalizadoESenhaComHash() throws Exception {
    when(users.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
    when(users.save(any(User.class))).thenAnswer(invocation -> {
      User salvo = invocation.getArgument(0);
      ReflectionTestUtils.setField(salvo, "id", ID);
      return salvo;
    });

    mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Ana\",\"email\":\"Ana@Example.COM\",\"password\":\"senha-bem-longa\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(ID.toString()))
        .andExpect(jsonPath("$.email").value("ana@example.com"))
        .andExpect(jsonPath("$.password").doesNotExist());

    var capturado = ArgumentCaptor.forClass(User.class);
    verify(users).save(capturado.capture());
    var hash = capturado.getValue().getPasswordHash();
    assertFalse(hash.contains("senha-bem-longa"), "o hash não pode conter a senha em claro");
    assertTrue(new BCryptPasswordEncoder().matches("senha-bem-longa", hash), "o hash precisa validar a senha original");
  }

  @Test void recusaCadastroComEmailJaExistente() throws Exception {
    when(users.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(persistedUser("senha-bem-longa")));

    mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Ana\",\"email\":\"ana@example.com\",\"password\":\"senha-bem-longa\"}"))
        .andExpect(status().isConflict());
  }

  @Test void recusaCadastroComSenhaCurtaOuEmailInvalido() throws Exception {
    mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Ana\",\"email\":\"ana@example.com\",\"password\":\"curta\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Ana\",\"email\":\"sem-arroba\",\"password\":\"senha-bem-longa\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test void emiteTokenParaCredenciaisValidas() throws Exception {
    when(users.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(persistedUser("senha-bem-longa")));

    mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials("ana@example.com", "senha-bem-longa")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token_type").value("Bearer"))
        .andExpect(jsonPath("$.access_token").isNotEmpty());
  }

  @Test void recusaLoginComSenhaErrada() throws Exception {
    when(users.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(persistedUser("senha-bem-longa")));

    mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials("ana@example.com", "senha-errada-longa")))
        .andExpect(status().isUnauthorized());
  }

  @Test void recusaLoginDeEmailInexistente() throws Exception {
    when(users.findByEmailIgnoreCase("ninguem@example.com")).thenReturn(Optional.empty());

    mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials("ninguem@example.com", "senha-bem-longa")))
        .andExpect(status().isUnauthorized());
  }

  @Test void introspeccaoDevolveClaimsDeTokenValido() throws Exception {
    var token = jwt.create(ID, "ana@example.com", "Ana");

    mvc.perform(post("/api/v1/auth/introspect").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(true))
        .andExpect(jsonPath("$.sub").value(ID.toString()))
        .andExpect(jsonPath("$.email").value("ana@example.com"))
        .andExpect(jsonPath("$.name").value("Ana"));
  }

  @Test void introspeccaoMarcaComoInativoQuandoOTokenNaoServe() throws Exception {
    mvc.perform(post("/api/v1/auth/introspect"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false)).andExpect(jsonPath("$.sub").doesNotExist());
    mvc.perform(post("/api/v1/auth/introspect").header("Authorization", "Bearer nao-e-um-jwt"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
    mvc.perform(post("/api/v1/auth/introspect").header("Authorization", jwt.create(ID, "ana@example.com", "Ana")))
        .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
  }

  @Test void introspeccaoRejeitaTokenExpirado() throws Exception {
    var expirado = new JwtService(SECRET, -1).create(ID, "ana@example.com", "Ana");

    mvc.perform(post("/api/v1/auth/introspect").header("Authorization", "Bearer " + expirado))
        .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
  }
}
