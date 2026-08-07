package com.fiapx.auth.drivers.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Este serviço emite os tokens; quem os valida são os demais. Todas as rotas ficam abertas
 * de propósito — exigir autenticação em {@code /login} seria circular.
 */
@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
        .build();
  }
}
