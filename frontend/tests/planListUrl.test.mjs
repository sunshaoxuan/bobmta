import { test } from 'node:test';
import assert from 'node:assert/strict';

import {
  buildPlanListSearch,
  parsePlanListUrlState,
} from '../dist/utils/planListUrl.js';

const DEFAULT_PAGE_SIZE = 10;

test('parsePlanListUrlState normalizes filters and pagination', () => {
  const state = parsePlanListUrlState(
    '?owner=  alice &status= ACTIVE &keyword= keyword &from=2025-01-01&to= 2025-01-31 &page=2&size=20'
  );
  assert.deepEqual(state.filters, {
    owner: 'alice',
    status: 'ACTIVE',
    keyword: 'keyword',
    from: '2025-01-01',
    to: '2025-01-31',
  });
  assert.equal(state.page, 2);
  assert.equal(state.pageSize, 20);
  assert.equal(state.view, 'table');
});

test('parsePlanListUrlState falls back to defaults when params missing', () => {
  const state = parsePlanListUrlState('');
  assert.deepEqual(state.filters, {
    owner: '',
    status: '',
    keyword: '',
    from: '',
    to: '',
  });
  assert.equal(state.page, 0);
  assert.equal(state.pageSize, DEFAULT_PAGE_SIZE);
  assert.equal(state.view, 'table');
});

test('buildPlanListSearch updates filter and pagination params', () => {
  const next = buildPlanListSearch('?foo=bar&owner=old&size=50', {
    filters: {
      owner: 'alice',
      status: 'ACTIVE',
      keyword: '',
      from: '',
      to: '',
    },
    page: 3,
    pageSize: 20,
    view: 'customer',
  });
  assert.equal(next, '?foo=bar&owner=alice&size=20&status=ACTIVE&page=3&view=customer');
});

test('buildPlanListSearch removes empty and default params', () => {
  const next = buildPlanListSearch('?owner=bob&page=2&size=10&foo=bar', {
    filters: {
      owner: '',
      status: '',
      keyword: '',
      from: '',
      to: '',
    },
    page: 0,
    pageSize: DEFAULT_PAGE_SIZE,
    view: 'table',
  });
  assert.equal(next, '?foo=bar');
});

test('parsePlanListUrlState reads custom view when present', () => {
  const state = parsePlanListUrlState('?view=calendar&owner=alice');
  assert.equal(state.view, 'calendar');
  assert.equal(state.filters.owner, 'alice');
});

test('buildPlanListSearch removes invalid view values', () => {
  const next = buildPlanListSearch('?view=calendar', {
    view: 'table',
  });
  assert.equal(next, '');
});
