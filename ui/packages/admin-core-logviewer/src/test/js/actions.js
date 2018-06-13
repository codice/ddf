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

import test from 'tape'
import { MD5 } from 'object-hash'

import { fetch } from '../../main/webapp/js/actions'
import random from './random-entry'

test('append to empty log list', (t) => {
  t.plan(1)

  const logs = [random()].map((entry) => ({hash: MD5(entry), ...entry}))
  const getLogs = (fn) => fn(null, logs)

  const dispatch = ({ type, entries }) => {
    if (type === 'APPEND_LOGS') {
      t.deepEqual(entries, logs)
    }
  }

  const getState = () => ({
    isPolling: true,
    isFetching: false,
    logs: []
  })

  fetch(getLogs)(dispatch, getState)
})

test('append with old logs', (t) => {
  t.plan(1)

  const oldLogs = [random()]
  const getLogs = (fn) => fn(null, oldLogs)

  const dispatch = ({ type, entries }) => {
    if (type === 'APPEND_LOGS') {
      t.deepEqual(entries, [])
    }
  }

  const getState = () => ({
    isPolling: true,
    isFetching: false,
    logs: oldLogs.map((entry) => ({hash: MD5(entry), ...entry}))
  })

  fetch(getLogs)(dispatch, getState)
})

test('append with new logs', (t) => {
  t.plan(1)

  const oldLogs = [random()].map((entry) => ({hash: 'firsthash', ...entry}))
  const newLogs = [random()].map((entry) => ({hash: 'secondhash', ...entry}))
  const getLogs = (fn) => fn(null, newLogs)

  const dispatch = ({ type, entries }) => {
    if (type === 'APPEND_LOGS') {
      t.deepEqual(entries, newLogs)
    }
  }

  const getState = () => ({
    isPolling: true,
    isFetching: false,
    logs: oldLogs
  })

  fetch(getLogs)(dispatch, getState)
})
