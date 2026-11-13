package com.yvette.api.customer;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
            select c from Customer c
            where lower(c.email) like :q or lower(c.fullName) like :q or c.phone like :q
            order by c.createdAt desc""")
    Page<Customer> search(String q, Pageable pageable);

    Page<Customer> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
