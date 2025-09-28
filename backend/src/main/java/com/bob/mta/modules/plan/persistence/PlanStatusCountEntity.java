package com.bob.mta.modules.plan.persistence;

import com.bob.mta.modules.plan.domain.PlanStatus;

public record PlanStatusCountEntity(PlanStatus status, long total) {
}
