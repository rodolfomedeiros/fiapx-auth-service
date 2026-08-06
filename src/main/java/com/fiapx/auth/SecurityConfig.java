package com.fiapx.auth;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
@Configuration class SecurityConfig { @Bean SecurityFilterChain filterChain(HttpSecurity http) throws Exception { return http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(a -> a.anyRequest().permitAll()).build(); } }
