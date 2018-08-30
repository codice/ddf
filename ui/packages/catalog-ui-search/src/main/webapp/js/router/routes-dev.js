this._compilation.compiler.options.mode === 'development'
  ? {
      _dev: {
        patterns: ['_dev(/)'],
        component: 'dev/component/dev/dev',
        menu: {
          text: 'Developer Guide',
          classes: 'is-bold',
        },
      },
    }
  : {}
