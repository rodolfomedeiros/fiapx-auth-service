package com.fiapx.auth.shared.exceptions;

import com.fiapx.auth.entities.exceptions.EmailAlreadyRegisteredException;
import com.fiapx.auth.entities.exceptions.InvalidCredentialsException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduz as exceções de domínio em respostas HTTP.
 *
 * <p>Sem isto, cada handler precisaria conhecer códigos de status para sinalizar uma regra
 * de negócio — e o domínio passaria a depender do protocolo.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(EmailAlreadyRegisteredException.class)
  ResponseEntity<Map<String, String>> emailAlreadyRegistered(EmailAlreadyRegisteredException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("detail", e.getMessage()));
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  ResponseEntity<Map<String, String>> invalidCredentials(InvalidCredentialsException e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("detail", e.getMessage()));
  }
}
