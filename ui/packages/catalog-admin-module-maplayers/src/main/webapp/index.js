import inject from 'react-tap-event-plugin'
import React from 'react'
import ReactDOM from 'react-dom'
import { AppContainer } from '@connexta/ace/react-hot-loader'

import App from './app'

const render = Component =>
  ReactDOM.render(
    <AppContainer>
      <Component />
    </AppContainer>,
    document.getElementById('root')
  )

inject()
render(App)

if (process.env.NODE_ENV !== 'production') {
  module.hot.accept('./app', () => render(require('./app').default))
}
