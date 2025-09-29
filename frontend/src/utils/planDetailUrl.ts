export type PlanDetailUrlState = {
  planId: string | null;
  timelineCategory: string | null;
  hasTimelineCategory: boolean;
};

const PLAN_PARAM_KEY = 'plan';
const TIMELINE_CATEGORY_PARAM_KEY = 'timelineCategory';

export function parsePlanDetailUrlState(search: string): PlanDetailUrlState {
  const params = new URLSearchParams(search);
  const planId = normalizeParam(params.get(PLAN_PARAM_KEY));
  const hasTimelineCategory = params.has(TIMELINE_CATEGORY_PARAM_KEY);
  const timelineCategory = hasTimelineCategory
    ? normalizeParam(params.get(TIMELINE_CATEGORY_PARAM_KEY))
    : null;

  return {
    planId,
    timelineCategory,
    hasTimelineCategory,
  };
}

export function buildPlanDetailSearch(
  search: string,
  state: { planId?: string | null; timelineCategory?: string | null }
): string {
  const params = new URLSearchParams(search);

  if ('planId' in state) {
    applyParam(params, PLAN_PARAM_KEY, state.planId);
  }
  if ('timelineCategory' in state) {
    applyParam(params, TIMELINE_CATEGORY_PARAM_KEY, state.timelineCategory);
  }

  const next = params.toString();
  return next ? `?${next}` : '';
}

function applyParam(params: URLSearchParams, key: string, value: string | null | undefined) {
  if (value === null || typeof value === 'undefined' || value.trim().length === 0) {
    params.delete(key);
    return;
  }
  params.set(key, value);
}

function normalizeParam(value: string | null): string | null {
  if (!value) {
    return null;
  }
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}
