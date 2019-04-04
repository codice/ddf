import { combineReducers, createStore, applyMiddleware, compose } from 'redux'
import thunk from 'redux-thunk'

import searchApp from './search-app-reducer'

const rootReducer = combineReducers({
  searchApp,
})

// Expose dev tools if not in production
const composeEnhancers =
  process.env.NODE_ENV === 'production'
    ? compose
    : window.__REDUX_DEVTOOLS_EXTENSION_COMPOSE__ || compose

// Create a store with thunk middleware.
const store = createStore(rootReducer, composeEnhancers(applyMiddleware(thunk)))

export default store
