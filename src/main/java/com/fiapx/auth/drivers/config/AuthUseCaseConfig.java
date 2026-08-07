package com.fiapx.auth.drivers.config;

import com.fiapx.auth.drivers.persistence.UserJpaRepository;
import com.fiapx.auth.gateways.BCryptPasswordHasher;
import com.fiapx.auth.gateways.JpaUserRepository;
import com.fiapx.auth.gateways.JwtTokenGateway;
import com.fiapx.auth.usecases.AuthenticateUserHandler;
import com.fiapx.auth.usecases.IntrospectTokenHandler;
import com.fiapx.auth.usecases.RegisterUserHandler;
import com.fiapx.auth.usecases.ports.PasswordHasher;
import com.fiapx.auth.usecases.ports.TokenGateway;
import com.fiapx.auth.usecases.ports.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Monta os casos de uso.
 *
 * <p>A fiação mora aqui, e não em {@code @Service} sobre cada handler, para que os casos de
 * uso continuem sendo Java puro — instanciáveis em teste sem subir contexto do Spring.
 */
@Configuration
public class AuthUseCaseConfig {

  @Bean
  UserRepository userRepository(UserJpaRepository jpa) {
    return new JpaUserRepository(jpa);
  }

  @Bean
  PasswordHasher passwordHasher() {
    return new BCryptPasswordHasher();
  }

  @Bean
  TokenGateway tokenGateway(
      @Value("${app.jwt-secret}") String secret,
      @Value("${app.jwt-expiration-minutes}") long expirationMinutes) {
    return new JwtTokenGateway(secret, expirationMinutes);
  }

  @Bean
  RegisterUserHandler registerUserHandler(UserRepository users, PasswordHasher hasher) {
    return new RegisterUserHandler(users, hasher);
  }

  @Bean
  AuthenticateUserHandler authenticateUserHandler(
      UserRepository users, PasswordHasher hasher, TokenGateway tokens) {
    return new AuthenticateUserHandler(users, hasher, tokens);
  }

  @Bean
  IntrospectTokenHandler introspectTokenHandler(TokenGateway tokens) {
    return new IntrospectTokenHandler(tokens);
  }
}
