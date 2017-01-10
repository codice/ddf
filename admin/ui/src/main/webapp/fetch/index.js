import 'whatwg-fetch'

import { Map } from 'immutable'
import { combineReducers } from 'redux-immutable'

// selectors

export const getException = (state) => state.get('exception')
export const isSubmitting = (state, id) => state.get('submitting').has(id)

// actions

export const setException = (ex) => ({ type: 'fetch/SET_EXCEPTION', ex })
export const clearException = () => ({ type: 'fetch/CLEAR_EXCEPTION' })

export const start = (id) => ({ type: 'fetch/START', id })
export const end = (id) => ({ type: 'fetch/END', id })

// async actions

export const fetch = (url, options, client = window.fetch) => async (dispatch) => {
  const { id, ...opts } = options

  if (id !== undefined) {
    dispatch(start(id))
  }

  const res = await client(url, { credentials: 'same-origin', ...opts })

  if (id !== undefined) {
    dispatch(end(id))
  }

  if (res.status === 500) {
    const json = await res.json()
    dispatch(setException({ ...json, url, ...opts }))

    // Throw exception to avoid handling this request because
    // something went terribly bad!
    throw Error('Internal Server Error')
  }

  return res
}

// additional fetch helpers
const make = (method) =>
  (url, options, client) => fetch(url, { method, ...options }, client)

export const get = make('GET')
export const post = make('POST')
export const put = make('PUT')
export const del = make('DELETE')

// reducers

const exception = (state = null, { type, ex } = {}) => {
  switch (type) {
    case 'fetch/SET_EXCEPTION':
      return ex
    case 'fetch/CLEAR_EXCEPTION':
      return null
    default: return state
  }
}

const submitting = (state = Map(), { type, id } = {}) => {
  switch (type) {
    case 'fetch/START':
      return state.set(id, true)
    case 'fetch/END':
      return state.delete(id)
    default:
      return state
  }
}

export default combineReducers({ submitting, exception })
