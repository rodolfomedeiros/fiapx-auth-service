package com.fiapx.auth.drivers.persistence;

import com.fiapx.auth.entities.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório do Spring Data. Fica no driver; os casos de uso enxergam apenas a porta. */
public interface UserJpaRepository extends JpaRepository<User, UUID> {

  Optional<User> findByEmailIgnoreCase(String email);
}
