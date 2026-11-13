package com.yvette.api.promotion;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    Optional<Promotion> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    @Query("select p from Promotion p where p.code is null and p.active = true")
    List<Promotion> findActiveAutomatic();

    /** Row lock so the usage limit holds when many people use the last uses of a code at once. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Promotion p where p.id = :id")
    Optional<Promotion> findForUpdate(Long id);

    List<Promotion> findAllByOrderByCreatedAtDesc();
}
