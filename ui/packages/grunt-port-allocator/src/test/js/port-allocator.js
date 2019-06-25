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
var net = require('net')

var tap = require('tap')
var allocator = require('../../main/js/lib/port-allocator.js')

tap.test('single port allocation', function(t) {
  t.plan(2)

  allocator(function(err, ports) {
    t.notOk(err)
    t.ok(ports, 'get the array of ports')
  })
})

tap.test('try binding to alocated port', function(t) {
  t.plan(2)

  allocator(function(err, ports) {
    t.notOk(err)

    var s = net.createServer()

    s.on('error', function() {
      t.fail('could not bind to allocated port')
    })

    s.on('listening', function() {
      t.pass('seems legit')
    })

    s.listen(ports[0])
  })
})

// the ports bound in the test will be effected by the previous
// tests since the allocator keeps the inital port bound
tap.test('multiple port allocation', function(t) {
  t.plan(3)

  allocator(function(err, a) {
    t.notOk(err)
    allocator(function(err, b) {
      t.notOk(err)

      t.notSame(a, b)
    })
  })
})

// need to kill process because it won't exit because of all the
// bound ports
tap.test('kill', function(t) {
  t.end()
  process.exit()
})
