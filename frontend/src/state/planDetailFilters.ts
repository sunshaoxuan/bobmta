import type { PlanDetailPayload } from '../api/types';
import { extractTimelineCategories } from '../utils/planTimeline.js';

export type PlanDetailFilters = {
  timeline: {
    categories: string[];
    activeCategory: string | null;
  };
};

export type PlanDetailFilterSnapshot = {
  timeline: {
    category: string | null;
  };
};

export const INITIAL_PLAN_DETAIL_FILTERS: PlanDetailFilters = {
  timeline: {
    categories: [],
    activeCategory: null,
  },
};

export const INITIAL_PLAN_DETAIL_FILTER_SNAPSHOT: PlanDetailFilterSnapshot = {
  timeline: {
    category: null,
  },
};

export function derivePlanDetailFilters(
  payload: PlanDetailPayload,
  snapshot: PlanDetailFilterSnapshot | null
): { filters: PlanDetailFilters; snapshot: PlanDetailFilterSnapshot } {
  const categories = extractTimelineCategories(payload.timeline);
  const persistedCategory = snapshot?.timeline.category ?? null;
  const activeCategory =
    persistedCategory && categories.includes(persistedCategory) ? persistedCategory : null;

  const filters: PlanDetailFilters = {
    timeline: {
      categories,
      activeCategory,
    },
  };

  const nextSnapshot: PlanDetailFilterSnapshot = {
    timeline: {
      category: activeCategory,
    },
  };

  return { filters, snapshot: nextSnapshot };
}

export function applyTimelineCategorySelection(
  filters: PlanDetailFilters,
  category: string | null
): { filters: PlanDetailFilters; snapshot: PlanDetailFilterSnapshot } {
  const normalized =
    category && filters.timeline.categories.includes(category) ? category : null;

  const nextFilters: PlanDetailFilters = {
    timeline: {
      categories: filters.timeline.categories,
      activeCategory: normalized,
    },
  };

  const snapshot: PlanDetailFilterSnapshot = {
    timeline: {
      category: normalized,
    },
  };

  return { filters: nextFilters, snapshot };
}
