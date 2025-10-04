import React, { type ReactNode } from '../../vendor/react/index.js';
import { Route, Routes } from '../../vendor/react-router-dom/index.js';

export const APP_ROUTE_PATHS = ['/', '/plans/:planId'] as const;

type AppRoutesProps = {
  renderApp: () => ReactNode;
};

export function AppRoutes({ renderApp }: AppRoutesProps) {
  return (
    <Routes>
      {APP_ROUTE_PATHS.map((path) => (
        <Route path={path} element={renderApp()} />
      ))}
    </Routes>
  );
}
