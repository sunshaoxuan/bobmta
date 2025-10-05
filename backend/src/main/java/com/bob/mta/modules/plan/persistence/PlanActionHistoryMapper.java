package com.bob.mta.modules.plan.persistence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PlanActionHistoryMapper {

    void insert(PlanActionHistoryEntity entity);

    List<PlanActionHistoryEntity> findByPlanId(@Param("planId") String planId);

    void deleteByPlanId(@Param("planId") String planId);
}
