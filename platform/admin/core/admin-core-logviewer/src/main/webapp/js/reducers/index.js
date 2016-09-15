/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

import { combineReducers } from 'redux'

// total number of log entries to keep in memory that a
// user can filter through
const MAX_LOG_ENTRIES = 5000
const INITIAL_DISPLAY_SIZE = 25
const GROW_SIZE = 50

const filter = (state = { level: 'ALL' }, { type, filter } = {}) => {
  switch (type) {
    case 'FILTER_LOGS':
      return { ...state, ...filter }
    default:
      return state
  }
}

const logs = (state = [], { type, entries } = {}) => {
  switch (type) {
    case 'APPEND_LOGS':
      return entries.concat(state).slice(0, MAX_LOG_ENTRIES)
    default:
      return state
  }
}

const displaySize = (state = INITIAL_DISPLAY_SIZE, { type } = {}) => {
  switch (type) {
    case 'GROW_DISPLAY_SIZE':
      return state + GROW_SIZE
    default:
      return state
  }
}

const expandedHash = (state = null, { type, hash } = {}) => {
  switch (type) {
    case 'CHANGE_EXPANDED_ENTRY':
      return (state === hash || hash === undefined) ? null : hash
    default:
      return state
  }
}

const isPolling = (state = false, { type } = {}) => {
  switch (type) {
    case 'TOGGLE_POLLING':
      return (!state)
    case 'SHOW_ERROR':
      return false
    default:
      return state
  }
}

const isFetching = (state = false, { type } = {}) => {
  switch (type) {
    case 'BEGIN_FETCHING':
      return true
    case 'END_FETCHING':
      return false
    default:
      return state
  }
}

const errorState = (state = { isInError: false }, { type, message } = {}) => {
  switch (type) {
    case 'SHOW_ERROR':
      return { isInError: true, message: message }
    case 'DISMISS_ERROR':
      return { isInError: false }
    default:
      return state
  }
}

export default combineReducers({ logs, filter, displaySize, expandedHash, isPolling, isFetching, errorState })
