import { createStore, compose, applyMiddleware } from 'redux'
import thunk from 'redux-thunk'

import { Map, fromJS } from 'immutable'

import reducer from './reducer'

var enhancer

if (process.env.NODE_ENV === 'production') {
  enhancer = applyMiddleware(thunk)
}

if (process.env.NODE_ENV !== 'production') {
  const DevTools = require('./containers/dev-tools').default
  const persistState = require('redux-devtools').persistState
  const debugSession =
    (window.location.href.match(/[?&]debug_session=([^&#]+)\b/) || [])[1]
  enhancer = compose(
    applyMiddleware(thunk),
    DevTools.instrument(),
    persistState(debugSession, fromJS)
  )
}

const store = createStore(reducer, Map(), enhancer)

if (module.hot) {
  module.hot.accept('./reducer', () => {
    const nextReducer = require('./reducer').default
    store.replaceReducer(nextReducer)
  })
}

export default store
