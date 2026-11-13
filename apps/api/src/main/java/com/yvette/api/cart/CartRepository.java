package com.yvette.api.cart;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByToken(UUID token);

    Optional<Cart> findFirstByCustomerIdOrderByUpdatedAtDesc(Long customerId);
}
