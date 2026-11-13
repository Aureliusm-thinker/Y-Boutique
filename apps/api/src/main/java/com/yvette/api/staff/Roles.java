package com.yvette.api.staff;

/**
 * Spring Security expressions for the permission matrix in docs/03-actors-and-use-cases.md §4.
 * Use with {@code @PreAuthorize(Roles.CATALOG_WRITE)}.
 */
public final class Roles {

    private Roles() {}

    public static final String ANY_STAFF =
            "hasAnyRole('OWNER','MANAGER','INVENTORY','FULFILMENT','SUPPORT','MARKETING')";

    /** View products & categories (Marketing is read-only). */
    public static final String CATALOG_READ = "hasAnyRole('OWNER','MANAGER','INVENTORY','MARKETING')";

    /** Create/edit products, pricing, categories. */
    public static final String CATALOG_WRITE = "hasAnyRole('OWNER','MANAGER','INVENTORY')";

    /** Stock adjustments. (Fulfilment will get "restock returns" via the returns flow in Sprint 5.) */
    public static final String INVENTORY_WRITE = "hasAnyRole('OWNER','MANAGER','INVENTORY')";

    /** Manage staff accounts and roles. */
    public static final String OWNER_ONLY = "hasRole('OWNER')";

    /** See orders (Inventory read-only for stock questions). */
    public static final String ORDERS_READ = "hasAnyRole('OWNER','MANAGER','INVENTORY','FULFILMENT','SUPPORT')";

    /** Pack, ship and mark delivered. */
    public static final String ORDERS_FULFIL = "hasAnyRole('OWNER','MANAGER','FULFILMENT')";

    /** Notes on orders and customers. */
    public static final String ORDERS_NOTE = "hasAnyRole('OWNER','MANAGER','FULFILMENT','SUPPORT')";

    /** Refunds and cancelling paid orders. Support may refund up to yvette.refunds.support-limit. */
    public static final String REFUNDS = "hasAnyRole('OWNER','MANAGER','SUPPORT')";

    /** Look up customers. */
    public static final String CUSTOMERS_READ = "hasAnyRole('OWNER','MANAGER','SUPPORT')";

    /** Approve/reject returns (Fulfilment receives and restocks them). */
    public static final String RETURNS_APPROVE = "hasAnyRole('OWNER','MANAGER','SUPPORT')";
    public static final String RETURNS_RECEIVE = "hasAnyRole('OWNER','MANAGER','SUPPORT','FULFILMENT')";

    /** Moderate reviews. */
    public static final String REVIEWS = "hasAnyRole('OWNER','MANAGER','SUPPORT','MARKETING')";

    /** Discount codes, sales and site content. */
    public static final String MARKETING = "hasAnyRole('OWNER','MANAGER','MARKETING')";

    /** Sales reports (others see their own slice on the dashboard). */
    public static final String REPORTS = "hasAnyRole('OWNER','MANAGER','MARKETING')";
}
