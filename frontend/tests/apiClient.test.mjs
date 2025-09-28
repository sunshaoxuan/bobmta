import { test } from 'node:test';
import assert from 'node:assert/strict';

import { createApiClient } from '../dist/api/client.js';

const originalFetch = globalThis.fetch;

test('api client attaches localization and auth headers', async () => {
  const calls = [];
  globalThis.fetch = async (input, init = {}) => {
    calls.push({ input, init });
    return new Response(JSON.stringify({ ok: true }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    });
  };

  try {
    const client = createApiClient({ getLocale: () => 'zh-CN' });
    await client.get('/mock-endpoint', { authToken: 'mock-token' });
  } finally {
    globalThis.fetch = originalFetch;
  }

  assert.equal(calls.length, 1);
  const headers = new Headers(calls[0].init.headers);
  assert.equal(headers.get('accept-language'), 'zh-CN');
  assert.equal(headers.get('authorization'), 'Bearer mock-token');
});

test('api client maps network failures to ApiError', async () => {
  globalThis.fetch = async () => {
    throw new Error('offline');
  };

  try {
    const client = createApiClient({ getLocale: () => 'ja-JP' });
    await assert.rejects(client.get('/fail'), (error) => {
      assert.equal(error.type, 'network');
      return true;
    });
  } finally {
    globalThis.fetch = originalFetch;
  }
});
