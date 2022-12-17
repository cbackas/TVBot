require('esbuild').build({
  entryPoints: ['src/app.ts'],
  bundle: true,
  platform: 'node',
  format: 'esm',
  target: 'es2022',
  outfile: 'dist/bundle.mjs',
  banner: {
    js: "import { createRequire } from 'module';const require = createRequire(import.meta.url);"
  }
}).catch(() => process.exit(1))
