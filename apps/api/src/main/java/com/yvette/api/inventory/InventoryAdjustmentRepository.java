package com.yvette.api.inventory;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, Long> {

    List<InventoryAdjustment> findTop50ByVariantIdOrderByCreatedAtDesc(Long variantId);
}
