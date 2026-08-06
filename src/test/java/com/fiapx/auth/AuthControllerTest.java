package com.fiapx.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AuthControllerTest {
  @Test void passwordHashDoesNotExposePasswordAndCanBeVerified() {
    var encoder = new BCryptPasswordEncoder();
    var hash = encoder.encode("correct-horse-battery-staple");
    assertFalse(hash.contains("correct-horse-battery-staple"));
    assertTrue(encoder.matches("correct-horse-battery-staple", hash));
  }
}
