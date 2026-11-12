package com.yvette.api.staff;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffRepository extends JpaRepository<StaffUser, Long> {

    Optional<StaffUser> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRoleAndActiveTrue(StaffRole role);

    List<StaffUser> findAllByOrderByFullNameAsc();
}
