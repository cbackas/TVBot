import { build } from 'esbuild'
import start from '@es-exec/esbuild-plugin-start'

/** @type import('esbuild').BuildOptions */
const config = {
  entryPoints: ['src/app.ts'],
  bundle: true,
  platform: 'node',
  format: 'esm',
  target: 'es2022',
  outfile: 'dist/bundle.mjs',
  sourcemap: 'inline',
  banner: {
    js: "import { createRequire } from 'module';const require = createRequire(import.meta.url);"
  },
  external: [
    '@prisma/client'
  ]
}

/** @type import('@es-exec/esbuild-plugin-start').ESLintPluginOptions */
const startOptions = {
  script: 'node --enable-source-maps dist/bundle.mjs'
}

if (process.env.NODE_ENV === 'development') {
  config.watch = true
  config.plugins = [start(startOptions)]
}

build(config).catch(() => process.exit(1))

