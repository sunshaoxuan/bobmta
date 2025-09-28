#!/usr/bin/env node
import http from 'node:http';
import { promises as fs } from 'node:fs';
import { extname, join, resolve } from 'node:path';

const port = Number(process.env.PORT ?? 4173);
const rootDir = resolve(process.cwd());
const mimeTypes = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
};

async function serveFile(res, filePath) {
  try {
    const data = await fs.readFile(filePath);
    const type = mimeTypes[extname(filePath)] ?? 'application/octet-stream';
    res.writeHead(200, { 'Content-Type': type });
    res.end(data);
  } catch (error) {
    res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end('Not found');
  }
}

const server = http.createServer(async (req, res) => {
  const url = req.url?.split('?')[0] ?? '/';
  if (url === '/' || url === '/index.html') {
    await serveFile(res, join(rootDir, 'index.html'));
    return;
  }
  const target = join(rootDir, url.replace(/^\/+/, ''));
  await serveFile(res, target);
});

server.listen(port, () => {
  console.log(`Preview server running at http://localhost:${port}`);
  console.log('Press Ctrl+C to stop.');
});
