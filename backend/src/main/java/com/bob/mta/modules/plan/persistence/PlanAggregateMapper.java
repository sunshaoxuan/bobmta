package com.bob.mta.modules.plan.persistence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface PlanAggregateMapper {

    List<PlanEntity> findPlans(PlanQueryParameters parameters);

    int countPlans(PlanQueryParameters parameters);

    PlanEntity findPlanById(@Param("planId") String planId);

    List<PlanParticipantEntity> findParticipantsByPlanIds(@Param("planIds") Collection<String> planIds);

    List<PlanNodeEntity> findNodesByPlanIds(@Param("planIds") Collection<String> planIds);

    List<PlanNodeExecutionEntity> findExecutionsByPlanIds(@Param("planIds") Collection<String> planIds);

    List<PlanNodeAttachmentEntity> findAttachmentsByPlanIds(@Param("planIds") Collection<String> planIds);

    List<PlanActivityEntity> findActivitiesByPlanIds(@Param("planIds") Collection<String> planIds);

    List<PlanReminderRuleEntity> findReminderRulesByPlanIds(@Param("planIds") Collection<String> planIds);

    List<PlanNodeAttachmentEntity> findAttachmentsByPlanId(@Param("planId") String planId);

    List<PlanActivityEntity> findActivitiesByPlanId(@Param("planId") String planId);

    List<PlanReminderRuleEntity> findReminderRulesByPlanId(@Param("planId") String planId);

    void insertPlan(PlanEntity entity);

    void updatePlan(PlanEntity entity);

    void deletePlan(@Param("planId") String planId);

    void deleteParticipants(@Param("planId") String planId);

    void insertParticipants(@Param("participants") List<PlanParticipantEntity> participants);

    void deleteNodes(@Param("planId") String planId);

    void insertNodes(@Param("nodes") List<PlanNodeEntity> nodes);

    void deleteExecutions(@Param("planId") String planId);

    void insertExecutions(@Param("executions") List<PlanNodeExecutionEntity> executions);

    void deleteAttachments(@Param("planId") String planId);

    void insertAttachments(@Param("attachments") List<PlanNodeAttachmentEntity> attachments);

    void deleteActivities(@Param("planId") String planId);

    void insertActivities(@Param("activities") List<PlanActivityEntity> activities);

    void deleteReminderRules(@Param("planId") String planId);

    void insertReminderRules(@Param("rules") List<PlanReminderRuleEntity> rules);

    void updateReminderAudit(@Param("planId") String planId,
                             @Param("updatedAt") OffsetDateTime updatedAt,
                             @Param("updatedBy") String updatedBy);

    String nextPlanId();

    String nextNodeId();

    String nextReminderId();

    List<PlanStatusCountEntity> countPlansByStatus(PlanAnalyticsQueryParameters parameters);

    long countOverduePlans(PlanAnalyticsQueryParameters parameters);

    List<PlanUpcomingPlanEntity> findUpcomingPlans(PlanAnalyticsQueryParameters parameters);

    List<PlanOwnerLoadEntity> findOwnerLoads(PlanAnalyticsQueryParameters parameters);

    List<PlanRiskPlanEntity> findRiskPlans(PlanAnalyticsQueryParameters parameters);

    List<PlanBoardCustomerAggregateEntity> aggregateCustomers(PlanBoardQueryParameters parameters);

    List<PlanBoardTimeBucketEntity> aggregateTimeBuckets(PlanBoardQueryParameters parameters);

    List<PlanBoardPlanEntity> findPlansForBoard(PlanBoardQueryParameters parameters);
}
