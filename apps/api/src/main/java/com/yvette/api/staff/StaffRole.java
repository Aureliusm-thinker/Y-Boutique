package com.yvette.api.staff;

/** Back-office roles. Permissions per role are defined in docs/03-actors-and-use-cases.md §4. */
public enum StaffRole {
    OWNER,
    MANAGER,
    INVENTORY,
    FULFILMENT,
    SUPPORT,
    MARKETING
}
