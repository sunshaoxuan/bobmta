export type PlanRoute =
  | { type: 'list'; planId: null }
  | { type: 'detail'; planId: string }
  | { type: 'unknown'; path: string };

const PLAN_SEGMENT = 'plans';

function normalizePathname(pathname: string): string {
  if (!pathname) {
    return '/';
  }
  if (!pathname.startsWith('/')) {
    return `/${pathname}`;
  }
  return pathname;
}

export function parsePlanRoute(pathname: string): PlanRoute {
  const normalized = normalizePathname(pathname);
  const trimmed = normalized.replace(/\/+/g, '/');
  const segments = trimmed.replace(/^\/+|\/+$/g, '').split('/').filter((segment) => segment.length > 0);
  if (segments.length === 0) {
    return { type: 'list', planId: null };
  }
  if (segments[0] !== PLAN_SEGMENT) {
    return { type: 'unknown', path: normalized };
  }
  if (segments.length === 1) {
    return { type: 'list', planId: null };
  }
  const planId = segments[1] ? decodeURIComponent(segments[1]) : '';
  if (!planId) {
    return { type: 'unknown', path: normalized };
  }
  return { type: 'detail', planId };
}

export function buildPlanDetailPath(planId: string): string {
  const trimmed = planId.trim();
  if (!trimmed) {
    return `/${PLAN_SEGMENT}`;
  }
  return `/${PLAN_SEGMENT}/${encodeURIComponent(trimmed)}`;
}
