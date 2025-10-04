import { test } from 'node:test';
import assert from 'node:assert/strict';

import { createApiClient } from '../dist/api/client.js';
import { fetchPlanAnalytics } from '../dist/api/plans.js';

const originalFetch = globalThis.fetch;

test('fetchPlanAnalytics encodes filters into query parameters', async () => {
  const calls = [];
  globalThis.fetch = async (input, init = {}) => {
    calls.push({ input, init });
    return new Response(
      JSON.stringify({
        code: 0,
        message: 'ok',
        data: {
          totalPlans: 0,
          designCount: 0,
          scheduledCount: 0,
          inProgressCount: 0,
          completedCount: 0,
          canceledCount: 0,
          overdueCount: 0,
          upcomingPlans: [],
          ownerLoads: [],
          riskPlans: [],
        },
      }),
      {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }
    );
  };

  try {
    const client = createApiClient({ getLocale: () => 'zh-CN' });
    const result = await fetchPlanAnalytics(client, 'mock-token', {
      ownerId: 'alice',
      customerId: 'cust-01',
      tenantId: 'tenant-01',
      from: '2024-04-01T00:00:00Z',
      to: null,
    });
    assert.equal(result.totalPlans, 0);
  } finally {
    globalThis.fetch = originalFetch;
  }

  assert.equal(calls.length, 1);
  const url = new URL(calls[0].input, 'https://example.com');
  assert.equal(url.pathname, '/api/v1/plans/analytics');
  assert.equal(url.searchParams.get('ownerId'), 'alice');
  assert.equal(url.searchParams.get('customerId'), 'cust-01');
  assert.equal(url.searchParams.get('tenantId'), 'tenant-01');
  assert.equal(url.searchParams.get('from'), '2024-04-01T00:00:00Z');
  assert.equal(url.searchParams.has('to'), false);
});

test('fetchPlanAnalytics treats missing payload as network error', async () => {
  globalThis.fetch = async () =>
    new Response(
      JSON.stringify({ code: 0, message: 'ok', data: null }),
      {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }
    );

  try {
    const client = createApiClient({ getLocale: () => 'ja-JP' });
    await assert.rejects(
      fetchPlanAnalytics(client, 'token', {}),
      (error) => {
        assert.equal(error.type, 'network');
        return true;
      }
    );
  } finally {
    globalThis.fetch = originalFetch;
  }
});
