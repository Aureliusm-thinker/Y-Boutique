package com.yvette.api.order;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByNumber(String number);

    /** Row lock: payment callbacks, expiry and staff actions on one order never interleave. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.id = :id")
    Optional<Order> findForUpdate(Long id);

    @Query("select o.id from CustomerOrder o where o.status = com.yvette.api.order.Order$Status.PENDING_PAYMENT and o.placedAt < :before")
    List<Long> findUnpaidPlacedBefore(Instant before);

    List<Order> findByCustomerIdOrderByPlacedAtDesc(Long customerId);

    List<Order> findByEmailIgnoreCaseAndCustomerIdIsNull(String email);

    List<Order> findByCustomerId(Long customerId);

    @Query(value = "SELECT nextval('order_number_seq')", nativeQuery = true)
    long nextNumber();
}
