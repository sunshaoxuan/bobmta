package com.bob.mta.modules.plan.repository;

import com.bob.mta.modules.plan.domain.PlanActionHistory;
import com.bob.mta.modules.plan.persistence.PlanActionHistoryMapper;
import com.bob.mta.modules.plan.persistence.PlanPersistenceMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@ConditionalOnBean(PlanActionHistoryMapper.class)
public class PlanPersistenceActionHistoryRepository implements PlanActionHistoryRepository {

    private final PlanActionHistoryMapper mapper;

    public PlanPersistenceActionHistoryRepository(PlanActionHistoryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void append(PlanActionHistory history) {
        mapper.insert(PlanPersistenceMapper.toActionHistoryEntity(history));
    }

    @Override
    public List<PlanActionHistory> findByPlanId(String planId) {
        return mapper.findByPlanId(planId).stream()
                .map(PlanPersistenceMapper::toActionHistory)
                .toList();
    }

    @Override
    public void deleteByPlanId(String planId) {
        mapper.deleteByPlanId(planId);
    }
}
