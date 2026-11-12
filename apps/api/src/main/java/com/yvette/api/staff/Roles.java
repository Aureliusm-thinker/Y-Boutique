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
}
