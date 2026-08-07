package com.fiapx.auth.usecases.ports;

import com.fiapx.auth.entities.User;
import java.util.Optional;

/** Porta de saída para a persistência de usuários. Sem Spring, sem JPA. */
public interface UserRepository {

  Optional<User> findByEmailIgnoreCase(String email);

  User save(User user);
}
