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
import reducer from '../../main/webapp/js/reducers'
import * as actions from '../../main/webapp/js/actions'
import random from './random-entry'

const hashOne = 'e59ff97941044f85df5297e1c302d260'
const hashTwo = '3d98044ff54aa08c6e57cec51a21966a'

test('initial state', t => {
  t.plan(2)

  const state = reducer();
  t.equal(state.logs.length, 0)
  t.deepEqual(state.filter, { level: 'ALL' })
})

test('filter logs', t => {
  t.plan(1)

  const state = reducer(reducer(), actions.filter({ level: 'DEBUG' }));
  t.equal(state.filter.level, 'DEBUG')
})

test('append logs', t => {
  t.plan(1)

  const action = actions.append([random()]);
  const state = reducer(reducer(), action);
  t.deepEqual(state.logs, action.entries)
})

test('truncate at maximum', t => {
  t.plan(2)

  const oldLogs = [];
  for (let i = 0; i < 5000; i++) {
    oldLogs.push({ hash: i, ...random() })
  }

  let action = actions.append(oldLogs);
  let state = reducer(reducer(), action);

  t.equal(state.logs.length, 5000, 'initial log list is not 5000 entries')

  const newLogs = [{ hash: 'newHash', ...random() }]

  action = actions.append(newLogs)
  state = reducer(state, action)

  t.equal(
    state.logs.length,
    5000,
    'log list is not being truncated at 5000 entries'
  )
})

test('set expandEntry, undefined + hashOne = hashOne', t => {
  t.plan(2)

  let state = reducer();
  t.equal(state.expandedHash, null, 'state has an undefined hash')
  state = reducer(state, actions.expandEntry(hashOne))
  t.equal(state.expandedHash, hashOne, 'state hash is changed to the new hash')
})

test('toggle expandEntry, hashOne + hashOne = undefined', t => {
  t.plan(2)

  let state = reducer(reducer(), actions.expandEntry(hashOne));
  t.equal(state.expandedHash, hashOne, 'state hash is set to hashOne')
  state = reducer(state, actions.expandEntry(hashOne))
  t.equal(
    state.expandedHash,
    null,
    'state hash is changed from hashOne to undefined'
  )
})

test('change expandEntry, hashOne + hashTwo = hashTwo', t => {
  t.plan(2)

  let state = reducer(reducer(), actions.expandEntry(hashOne));
  t.equal(state.expandedHash, hashOne, 'state hash is set to hashOne')
  state = reducer(state, actions.expandEntry(hashTwo))
  t.equal(state.expandedHash, hashTwo, 'state hash is changed to hashTwo')
})
