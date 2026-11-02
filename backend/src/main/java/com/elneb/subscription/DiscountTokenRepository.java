package com.elneb.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface DiscountTokenRepository extends JpaRepository<DiscountToken, UUID> {
    Optional<DiscountToken> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
}
