package com.yvette.api.catalog;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    Page<Product> findByStatusAndCategorySlug(ProductStatus status, String categorySlug, Pageable pageable);

    Optional<Product> findBySlugAndStatus(String slug, ProductStatus status);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    long countByStatus(ProductStatus status);

    long countByCategoryId(Long categoryId);

    @Query("select count(v) > 0 from ProductVariant v where lower(v.sku) = lower(:sku) and v.product.id <> :productId")
    boolean skuUsedByOtherProduct(String sku, Long productId);
}
