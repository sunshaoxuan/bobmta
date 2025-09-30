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
      assert.equal(error.message, 'offline');
      return true;
    });
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test('api client surfaces status error message and code when available', async () => {
  globalThis.fetch = async () =>
    new Response(
      JSON.stringify({ code: 'PLAN-403', message: 'operation not allowed' }),
      {
        status: 403,
        headers: { 'Content-Type': 'application/json' },
      }
    );

  try {
    const client = createApiClient({ getLocale: () => 'ja-JP' });
    await assert.rejects(client.get('/forbidden'), (error) => {
      assert.equal(error.type, 'status');
      assert.equal(error.status, 403);
      assert.equal(error.code, 'PLAN-403');
      assert.equal(error.message, 'operation not allowed');
      return true;
    });
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test('api client falls back to raw text errors when json parsing fails', async () => {
  globalThis.fetch = async () =>
    new Response('permission denied', {
      status: 403,
      headers: { 'Content-Type': 'text/plain' },
    });

  try {
    const client = createApiClient({ getLocale: () => 'ja-JP' });
    await assert.rejects(client.get('/text-error'), (error) => {
      assert.equal(error.type, 'status');
      assert.equal(error.status, 403);
      assert.equal(error.message, 'permission denied');
      return true;
    });
  } finally {
    globalThis.fetch = originalFetch;
  }
});
