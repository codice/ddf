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
var async = require('async')

// compute range from start to end non-inclusive
// => [start, start + 1, ... , end - 1]
var range = function(start, end) {
  if (start === end) return []
  return [start].concat(range(start + 1, end))
}

var startPort = 21000
var portRange = 10

// bind a tcp socket to a given port
var bind = function(port, done) {
  var s = net.createServer()

  s.on('error', done)
  s.on('listening', done)

  s.listen(port)

  return s
}

// check if a given port is avaiable by trying to binding and
// unbinding
var available = function(port, done) {
  var s = bind(port, function(err) {
    if (err) {
      done(false)
    } else {
      s.on('close', function() {
        done(true)
      }).close()
    }
  })
}

// try 'allocating' a range of ports by using convention.
// NOTE: don't unbind from the port until process exits; this
// convention is what prevents others call to allocatePorts
// from clobbering each other.
var allocatePorts = function(port, done) {
  var s = bind(port, function(err) {
    if (err) {
      return done(err)
    }

    var ports = range(port + 1, port + portRange)

    // sweep ports for a quick check that they are all
    // actually free
    async.every(ports, available, function(allAvailable) {
      if (!allAvailable) {
        s.on('close', function() {
          done(true) // signal that an error occurred
        }).close()
      } else {
        done(null, ports)
      }
    })
  })
}

var maxPortStart = 65535 - portRange

// retry port allocation going up by increments of portRange
var retryAllocate = function(port, done) {
  if (port > maxPortStart) {
    done(new Error('why you have no ports!?!'))
  } else {
    allocatePorts(port, function(err, ports) {
      if (err) {
        retryAllocate(port + portRange, done)
      } else {
        done(null, ports)
      }
    })
  }
}

// returns an array of available ports
module.exports = function(done) {
  retryAllocate(startPort, done)
}
