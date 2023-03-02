module.exports = {
  env: {
    es2021: true,
    node: true
  },
  extends: 'standard-with-typescript',
  parserOptions: {
    project: './tsconfig.json',
    ecmaVersion: 'latest',
    sourceType: 'module'
  },
  rules: {
    'interface-over-type-literal': 'off',
    semi: 'error',
    '@typescript-eslint/no-invalid-void-type': 'off'
  }
}
