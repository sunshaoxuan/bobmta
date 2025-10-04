import { test } from 'node:test';
import assert from 'node:assert/strict';

import { APP_ROUTE_PATHS } from '../dist/router/AppRoutes.js';

test('AppRoutes declares root and plan detail paths', () => {
  assert.deepEqual(Array.from(APP_ROUTE_PATHS), ['/', '/plans/:planId']);
});
