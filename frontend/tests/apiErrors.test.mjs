import { test } from 'node:test';
import assert from 'node:assert/strict';

import {
  formatApiErrorMessage,
  extractApiErrorCode,
} from '../dist/utils/apiErrors.js';

const translate = (key, values = {}) => {
  if (key === 'backendErrorStatus') {
    return `status:${values.status}`;
  }
  if (key === 'backendErrorNetwork') {
    return 'network';
  }
  throw new Error(`unexpected key ${key}`);
};

test('formatApiErrorMessage prioritizes backend message and appends code', () => {
  const result = formatApiErrorMessage(
    { type: 'status', status: 403, code: 'PLAN-403', message: 'operation not allowed' },
    translate
  );
  assert.equal(result, 'operation not allowed (PLAN-403)');
});

test('formatApiErrorMessage falls back to status translation when message missing', () => {
  const result = formatApiErrorMessage(
    { type: 'status', status: 404, code: 'NOT_FOUND' },
    translate
  );
  assert.equal(result, 'status:404 (NOT_FOUND)');
});

test('formatApiErrorMessage falls back to network translation', () => {
  const result = formatApiErrorMessage({ type: 'network' }, translate);
  assert.equal(result, 'network');
});

test('extractApiErrorCode normalizes values', () => {
  assert.equal(extractApiErrorCode({ type: 'status', status: 500, code: 'ERR-500' }), 'ERR-500');
  assert.equal(extractApiErrorCode({ type: 'status', status: 500, code: 401 }), 401);
  assert.equal(extractApiErrorCode({ type: 'network' }), null);
});
