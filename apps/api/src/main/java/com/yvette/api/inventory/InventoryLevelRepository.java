package com.yvette.api.inventory;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface InventoryLevelRepository extends JpaRepository<InventoryLevel, Long> {

    /** Row lock so concurrent adjustments (and later, checkouts) can't lose updates. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryLevel i where i.variantId = :variantId")
    Optional<InventoryLevel> findForUpdate(Long variantId);

    record LowStockRow(Long variantId, String sku, String size, String colour, Long productId, String productTitle,
                       int onHand, int reserved, int lowStockThreshold) {}

    @Query("""
            select new com.yvette.api.inventory.InventoryLevelRepository$LowStockRow(
                v.id, v.sku, v.size, v.colour, p.id, p.title, i.onHand, i.reserved, i.lowStockThreshold)
            from InventoryLevel i, com.yvette.api.catalog.ProductVariant v join v.product p
            where v.id = i.variantId
              and p.status <> com.yvette.api.catalog.ProductStatus.ARCHIVED
              and (i.onHand - i.reserved) <= i.lowStockThreshold
            order by (i.onHand - i.reserved) asc, p.title asc
            """)
    List<LowStockRow> findLowStock();
}
