package com.fiapx.auth;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
interface UserRepository extends JpaRepository<User, UUID> { Optional<User> findByEmailIgnoreCase(String email); }
