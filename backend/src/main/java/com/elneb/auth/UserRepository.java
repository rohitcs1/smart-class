package com.elneb.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<User> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
