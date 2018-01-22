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

import filter from '../../main/webapp/js/filter'
import random from './random-entry'

const getEntry = (item) => item.entry

test('level filter TRACE', (t) => {
  t.plan(1)

  const logs = [{ level: 'TRACE' }, { level: 'INFO' }, { level: 'ERROR' }].map(random)
  const filtered = filter({ level: 'TRACE' }, logs).map(getEntry)

  t.deepEqual(filtered, [logs[0], logs[1], logs[2]])
})

test('level filter WARN', (t) => {
  t.plan(1)

  const logs = [{ level: 'INFO' }, { level: 'WARN' }, { level: 'ERROR' }].map(random)
  const filtered = filter({ level: 'WARN' }, logs).map(getEntry)

  t.deepEqual(filtered, [logs[1], logs[2]])
})

test('level filter ERROR', (t) => {
  t.plan(1)

  const logs = [{ level: 'DEBUG' }, { level: 'WARN' }, { level: 'ERROR' }].map(random)
  const filtered = filter({ level: 'ERROR' }, logs).map(getEntry)

  t.deepEqual(filtered, [logs[2]])
})

test('text filter', (t) => {
  t.plan(1)

  const logs = [{ message: 'first' }, { message: 'second' }].map(random)
  const filtered = filter({ message: 'first' }, logs).map(getEntry)

  t.deepEqual(filtered, [logs[0]])
})

test('text filter (partial match)', (t) => {
  t.plan(1)

  const logs = [{ message: 'random' }].map(random)
  const filtered = filter({ message: 'rand' }, logs).map(getEntry)

  t.deepEqual(filtered, [logs[0]])
})

test('text filter (regex match)', (t) => {
  t.plan(1)

  const logs = [{ message: 'one two' }, { message: 'two one' }].map(random)
  const filtered = filter({ message: '^one' }, logs).map(getEntry)

  t.deepEqual(filtered, [logs[0]])
})

test('compound filter (logical AND)', (t) => {
  t.plan(1)

  const logs = [
    { level: 'DEBUG', message: 'first' },
    { level: 'WARN', message: 'second' }
  ].map(random)

  const filtered = filter({ level: 'DEBUG', message: 'second' }, logs).map(getEntry)

  t.deepEqual(filtered, [logs[1]])
})
