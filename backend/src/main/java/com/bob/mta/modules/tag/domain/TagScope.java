package com.bob.mta.modules.tag.domain;

public enum TagScope {
    CUSTOMER,
    PLAN,
    BOTH;

    public boolean supports(TagEntityType entityType) {
        return this == BOTH
                || (this == CUSTOMER && entityType == TagEntityType.CUSTOMER)
                || (this == PLAN && entityType == TagEntityType.PLAN);
    }
}
