import { createStore, compose, applyMiddleware } from 'redux'
import thunk from 'redux-thunk'

import { Map } from 'immutable'

import reducer from './reducer'

var enhancer

if (process.env.NODE_ENV === 'production') {
  enhancer = applyMiddleware(thunk)
}

if (process.env.NODE_ENV !== 'production') {
  const DevTools = require('./containers/dev-tools').default
  enhancer = compose(applyMiddleware(thunk), DevTools.instrument())
}

const store = createStore(reducer, Map(), enhancer)

if (module.hot) {
  module.hot.accept('./reducer', () => {
    const nextReducer = require('./reducer').default
    store.replaceReducer(nextReducer)
  })
}

export default store
