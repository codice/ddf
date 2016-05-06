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
import eventStream from 'event-stream'

const one = { message: '1', timestamp: 1 }
const two = { message: '2', timestamp: 2 }

test('old entry (duplicate)', function (t) {
  t.timeoutAfter(3000)
  t.plan(2)

  var d = uniq()
  var seq = [one]

  d.pipe(eventStream.writeArray(function (err, list) {
    t.notOk(err)
    t.deepEqual(list, seq)
  }))

  eventStream.readArray([one, one]).pipe(d)
})

test('new entry', function (t) {
  t.timeoutAfter(3000)
  t.plan(2)

  var d = uniq()
  var seq = [one, two]

  d.pipe(eventStream.writeArray(function (err, list) {
    t.notOk(err)
    t.deepEqual(list, seq)
  }))

  eventStream.readArray([one, two]).pipe(d)
})

test('same timestamp, different entry', function (t) {
  t.timeoutAfter(3000)
  t.plan(2)

  var d = uniq()
  var onePointFive = { message: '.5', timestamp: 1 }

  var seq = [one, onePointFive]

  d.pipe(eventStream.writeArray(function (err, list) {
    t.notOk(err)
    t.deepEqual(list, seq)
  }))

  eventStream.readArray([one, onePointFive]).pipe(d)
})

test('same hashes from identical objects.', function (t) {
  t.timeoutAfter(3000)
  t.plan(2)

  var d = uniq()

  d.pipe(eventStream.writeArray(function (err, list) {
    t.notOk(err)
    t.equal(list[1], list[2])
  }))

  eventStream.readArray([one, one]).pipe(d)
})

test('different hashes from different objects.', function (t) {
  t.timeoutAfter(3000)
  t.plan(2)

  var d = uniq()

  d.pipe(eventStream.writeArray(function (err, list) {
    t.notOk(err)
    t.notEqual(list[1], list[2])
  }))

  eventStream.readArray([one, two]).pipe(d)
})
