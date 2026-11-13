package com.yvette.api.payment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByProviderAndProviderReference(String provider, String providerReference);

    List<Payment> findByOrderIdOrderByCreatedAtAsc(Long orderId);

    Optional<Payment> findFirstByOrderIdAndKindOrderByCreatedAtDesc(Long orderId, Payment.Kind kind);

    Optional<Payment> findFirstByOrderIdAndKindAndStatusOrderByCreatedAtDesc(Long orderId, Payment.Kind kind,
                                                                             Payment.Status status);
}
