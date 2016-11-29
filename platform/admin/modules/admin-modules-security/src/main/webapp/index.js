import inject from 'react-tap-event-plugin'
import React from 'react'
import { render } from 'react-dom'
import App from './app'

inject()

if (process.env.NODE_ENV === 'production') {
  render(<App />, document.getElementById('root'))
}

if (process.env.NODE_ENV !== 'production') {
  window.React = React

  const AppContainer = require('react-hot-loader').AppContainer

  render(
    <AppContainer errorReporter={({ error }) => { throw error }}>
      <App />
    </AppContainer>,
    document.getElementById('root'))

  module.hot.accept('./app', () => {
    // If you use Webpack 2 in ES modules mode, you can
    // use <App /> here rather than require() a <NextApp />.
    try {
      const NextApp = require('./app').default
      render(
        <AppContainer errorReporter={({ error }) => { throw error }}>
          <NextApp />
        </AppContainer>,
        document.getElementById('root'))
    } catch (e) {}
  })
}
