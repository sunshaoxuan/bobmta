package com.bob.mta.common.tenant;

import org.springframework.stereotype.Component;

/**
 * Resolves the current tenant identifier for multi-tenant aware repositories.
 *
 * <p>The current implementation keeps things simple by returning a static default tenant
 * identifier. This can be replaced with request scoped or security context based resolution
 * once tenant onboarding is implemented.</p>
 */
@Component
public class TenantContext {

    public static final String DEFAULT_TENANT_ID = "tenant-001";

    public String getCurrentTenantId() {
        return DEFAULT_TENANT_ID;
    }
}
