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

test('initial state', function (t) {
  t.plan(2)

  var state = reducer()
  t.equal(state.logs.length, 0)
  t.deepEqual(state.filter, { level: 'ALL' })
})

test('filter logs', function (t) {
  t.plan(1)

  var state = reducer(reducer(), actions.filter({ level: 'DEBUG' }))
  t.equal(state.filter.level, 'DEBUG')
})

test('append logs', function (t) {
  t.plan(1)

  var entries = [random()]
  var state = reducer(reducer(), actions.append(entries))
  t.deepEqual(state.logs, entries)
})
