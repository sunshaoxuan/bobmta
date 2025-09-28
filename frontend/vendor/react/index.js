import {
  useState,
  useEffect,
  useMemo,
  useCallback,
  useRef,
} from './hooks.js';

export { useState, useEffect, useMemo, useCallback, useRef } from './hooks.js';

export const TEXT_ELEMENT = Symbol('react.text');
export const Fragment = Symbol('react.fragment');

export const StrictMode = ({ children }) => children ?? null;

export const forwardRef = (render) => {
  const ForwardComponent = (props) => render(props, null);
  ForwardComponent.displayName = render.name || 'ForwardRef';
  return ForwardComponent;
};

const normalizeChild = (child, target) => {
  if (Array.isArray(child)) {
    child.forEach((value) => normalizeChild(value, target));
    return;
  }
  if (child === null || child === undefined || child === false) {
    return;
  }
  if (typeof child === 'object') {
    target.push(child);
    return;
  }
  target.push({ type: TEXT_ELEMENT, props: { nodeValue: String(child), children: [] } });
};

export function createElement(type, props, ...children) {
  const resolvedChildren = [];
  children.forEach((child) => normalizeChild(child, resolvedChildren));
  const finalProps = { ...(props ?? {}), children: resolvedChildren };
  return { type, props: finalProps };
}

const React = {
  createElement,
  Fragment,
  StrictMode,
  useState,
  useEffect,
  useMemo,
  useCallback,
  useRef,
  forwardRef,
};

export default React;
