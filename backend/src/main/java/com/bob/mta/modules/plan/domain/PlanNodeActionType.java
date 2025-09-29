package com.bob.mta.modules.plan.domain;

/**
 * Describes how a plan node should be executed or which helper should be invoked when
 * the node reaches the active state. The design document enumerates a small, finite
 * list of supported interactions that can later be extended with additional handlers.
 */
public enum PlanNodeActionType {
    NONE,
    REMOTE,
    EMAIL,
    IM,
    LINK,
    FILE
}
