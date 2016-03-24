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
import uniq from '../../main/webapp/js/uniq'

const one = { message: '1', timestamp: 1 }
const two = { message: '2', timestamp: 2 }

test('old entry (duplicate)', function (t) {
  t.timeoutAfter(25)
  t.plan(2)

  var d = uniq()

  d.on('data', function (entry) {
    t.equal(entry.message, one.message)
    t.equal(entry.timestamp, one.timestamp)
  })

  d.write(one)
  d.write(one)
  d.end()
})

test('new entry', function (t) {
  t.timeoutAfter(25)
  t.plan(4)

  var d = uniq()
  var count = 0
  var seq = [one, two]

  d.on('data', function (entry) {
    t.equal(entry.message, seq[count].message)
    t.equal(entry.timestamp, seq[count++].timestamp)
  })

  d.write(one)
  d.write(two)
  d.end()
})

test('same timestamp, different entry', function (t) {
  t.timeoutAfter(25)
  t.plan(4)

  var d = uniq()
  var onePointFive = { message: '.5', timestamp: 1 }

  var count = 0
  var seq = [one, onePointFive]
  d.on('data', function (entry) {
    t.equal(entry.message, seq[count].message)
    t.equal(entry.timestamp, seq[count++].timestamp)
  })

  d.write(one)
  d.write(onePointFive)
  d.end()
})
