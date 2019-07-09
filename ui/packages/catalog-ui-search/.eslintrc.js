module.exports = {
    rules: {
      'no-console': 'off',
      '@connexta/connexta/no-absolute-urls': 2,
    },
    globals: {
      "define": "writable",
    },
    env: {
      "browser": true,
      "node": true,
      "mocha": true
    },
    settings: {
      react: { version: 'detect' },
    },
    parserOptions: {
      ecmaVersion: 6,
      sourceType: 'module',
      ecmaFeatures: { jsx: true },
    },
    plugins: ['react', '@connexta/connexta'],
    parser: 'babel-eslint',
  }