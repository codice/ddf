import React from 'react'
import { Provider } from 'react-redux'
import store from './store'
import SearchesContainer from './searches-container'

export default function SearchRoot() {
  return (
    <Provider store={store}>
      <SearchesContainer />
    </Provider>
  )
}
