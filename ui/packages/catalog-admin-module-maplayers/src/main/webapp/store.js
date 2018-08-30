import { createStore, applyMiddleware, compose } from 'redux'
import thunk from 'redux-thunk'

import { Map } from 'immutable'

import reducer from './reducer'

const composeEnhancers = window.__REDUX_DEVTOOLS_EXTENSION_COMPOSE__ || compose

const exceptionLoggger = store => next => action => {
  try {
    return next(action)
  } catch (e) {
    console.error(e)
    throw e
  }
}

const store = createStore(
  reducer,
  Map(),
  composeEnhancers(applyMiddleware(exceptionLoggger, thunk))
)

if (module.hot) {
  module.hot.accept('./reducer', () => {
    const nextReducer = require('./reducer').default
    store.replaceReducer(nextReducer)
  })
}

export default store
