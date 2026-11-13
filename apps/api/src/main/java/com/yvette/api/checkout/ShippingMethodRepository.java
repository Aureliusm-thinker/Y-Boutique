package com.yvette.api.checkout;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShippingMethodRepository extends JpaRepository<ShippingMethod, String> {

    List<ShippingMethod> findByActiveTrueOrderByPositionAsc();
}
