import type { PlanTimelineEntry } from '../api/types';

export type TimelineFilterResult = {
  categories: string[];
  activeCategories: string[];
  filteredEntries: PlanTimelineEntry[];
  isFilterActive: boolean;
  isFilteredEmpty: boolean;
};

export function extractTimelineCategories(
  entries: readonly PlanTimelineEntry[]
): string[] {
  const seen = new Set<string>();
  const categories: string[] = [];
  for (const entry of entries) {
    if (!entry.category) {
      continue;
    }
    if (seen.has(entry.category)) {
      continue;
    }
    seen.add(entry.category);
    categories.push(entry.category);
  }
  return categories;
}

export function filterTimelineEntries(
  entries: readonly PlanTimelineEntry[],
  categories: readonly string[]
): PlanTimelineEntry[] {
  if (!categories || categories.length === 0) {
    return entries.slice();
  }
  const allowed = new Set(categories);
  return entries.filter((entry) => entry.category && allowed.has(entry.category));
}

export function isTimelineHighlightVisible(
  entries: readonly PlanTimelineEntry[],
  categories: readonly string[],
  highlightId: string | null
): boolean {
  if (!highlightId) {
    return true;
  }
  const filtered = filterTimelineEntries(entries, categories);
  return filtered.some((entry) => entry.id === highlightId);
}

export function deriveTimelineFilter(
  entries: readonly PlanTimelineEntry[],
  selectedCategory: string | null
): TimelineFilterResult {
  const categories = extractTimelineCategories(entries);
  const normalizedSelection =
    typeof selectedCategory === 'string' && selectedCategory !== ''
      ? selectedCategory
      : null;
  const activeCategories = normalizedSelection ? [normalizedSelection] : [];
  const filteredEntries = filterTimelineEntries(entries, activeCategories);
  return {
    categories,
    activeCategories,
    filteredEntries,
    isFilterActive: activeCategories.length > 0,
    isFilteredEmpty: entries.length > 0 && filteredEntries.length === 0,
  };
}
