package com.elneb.teacher;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface RfidCardRepository extends JpaRepository<RfidCard, UUID> {
    boolean existsByCardUidIgnoreCase(String cardUid);
    Optional<RfidCard> findByCardUidIgnoreCase(String cardUid);
    Optional<RfidCard> findByTeacherId(UUID teacherId);
}
