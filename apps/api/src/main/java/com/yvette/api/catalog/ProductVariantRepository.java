package com.yvette.api.catalog;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    /** Variants with their product (and its images) loaded, for carts and orders. */
    @Query("select v from ProductVariant v join fetch v.product p where v.id in :ids")
    List<ProductVariant> findWithProduct(Collection<Long> ids);
}
