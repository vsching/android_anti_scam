import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';
import fs from 'fs';

export default defineConfig({
  plugins: [
    react(),
    {
      name: 'serve-prototype-screens',
      configureServer(server) {
        const screensDir = path.resolve(__dirname, '..', 'screens');

        server.middlewares.use((req, res, next) => {
          if (!req.url || !req.url.startsWith('/screens/')) {
            return next();
          }

          const filePath = path.join(screensDir, req.url.replace('/screens/', ''));
          const safePath = path.resolve(filePath);

          // Security: ensure we're still within screensDir
          if (!safePath.startsWith(screensDir)) {
            return next();
          }

          if (fs.existsSync(safePath) && fs.statSync(safePath).isFile()) {
            const ext = path.extname(safePath).toLowerCase();
            const mimeTypes: Record<string, string> = {
              '.html': 'text/html',
              '.css': 'text/css',
              '.js': 'application/javascript',
              '.json': 'application/json',
              '.png': 'image/png',
              '.svg': 'image/svg+xml',
            };
            res.setHeader('Content-Type', mimeTypes[ext] || 'application/octet-stream');
            res.end(fs.readFileSync(safePath));
          } else {
            next();
          }
        });
      },
    },
  ],
  server: {
    fs: {
      allow: ['..', '../..'],
    },
  },
});
