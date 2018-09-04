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

import { MD5 } from 'object-hash'
import takeWhile from 'lodash/takeWhile'

const POLLING_INTERVAL = 5000

// for applying a filter to the logs
export const filter = filter => ({
  type: 'FILTER_LOGS',
  filter: filter,
})

// for appending new logs to the stored log list
// note: user will not lose context when scrolling
export const append = entries => ({
  type: 'APPEND_LOGS',
  entries: entries.map(entry => ({
    hash: entry.hash || MD5(entry),
    ...entry,
  })),
})

// used for expanding the max amount of stored logs displayed on screen
export const grow = () => ({
  type: 'GROW_DISPLAY_SIZE',
})

// expands logs with the given hash
export const expandEntry = hash => ({
  type: 'CHANGE_EXPANDED_ENTRY',
  hash: hash,
})

// called at beginning of fetching
// prevents slow networks from making duplicate calls to the backend
export const beginFetching = () => ({
  type: 'BEGIN_FETCHING',
})

// called at the end of fetching
export const endFetching = () => ({
  type: 'END_FETCHING',
})

// toggle polling to the backend for new logs
export const togglePolling = () => ({
  type: 'TOGGLE_POLLING',
})

export const showError = errorMessage => ({
  type: 'SHOW_ERROR',
  message: errorMessage,
})

export const dismissError = () => ({
  type: 'DISMISS_ERROR',
})

// retrieves new logs from the backend and appends any new ones
export const fetch = getLogs => (dispatch, getState) => {
  if (getState().isPolling && !getState().isFetching) {
    dispatch(beginFetching())
    getLogs((err, logs) => {
      if (err) {
        dispatch(showError('Network Error'))
      } else {
        const oldLogs = getState().logs
        const uniq = getUnique(oldLogs, logs.reverse())
        dispatch(append(uniq))
        dispatch(dismissError())
      }
      dispatch(endFetching())
    })
  }
}

export const getUnique = (oldLogs, newLogs) => {
  if (oldLogs.length === 0) {
    return newLogs
  }
  const oldHash = oldLogs[0].hash
  return takeWhile(newLogs, entry => MD5(entry) !== oldHash)
}

// timeout loop for polling the backend
export const fetchLoop = (getLogs, interval) => (dispatch, getState) => {
  dispatch(fetch(getLogs))
  setTimeout(() => dispatch(fetchLoop(getLogs)), interval || POLLING_INTERVAL)
}
