export { createRoot, type Root } from './index.js';

declare const ReactDOMClient: {
  createRoot: typeof createRoot;
};

export default ReactDOMClient;
