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
import batch from '../../main/webapp/js/batch'

const one = { message: '1', timestamp: 1 }

test('log added to array', function (t) {
  t.timeoutAfter(50)
  t.plan(2)

  var d = batch(1)

  d.on('data', function (entryList) {
    t.equal(entryList[0].timestamp, one.timestamp)
    t.equal(entryList[0].message, one.message)
  })

  d.write(one)
  d.end()
})
