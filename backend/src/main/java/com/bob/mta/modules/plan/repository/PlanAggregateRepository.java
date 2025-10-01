package com.bob.mta.modules.plan.repository;

/**
 * Aggregates the different persistence concerns for a plan so the service layer can
 * coordinate persistence of the core definition, reminder policies, timeline and attachments
 * independently.
 */
public interface PlanAggregateRepository {

    PlanRepository plans();

    PlanReminderPolicyRepository reminderPolicies();

    PlanTimelineRepository timelines();

    PlanAttachmentRepository attachments();
}

