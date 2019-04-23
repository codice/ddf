const net = require('net');
const async = require('async');

// compute range from start to end non-inclusive
// => [start, start + 1, ... , end - 1]
const range = function(start, end) {
  if (start === end) return []
  return [start].concat(range(start + 1, end))
};

const startPort = 21000;
const portRange = 10;

// bind a tcp socket to a given port
const bind = function(port, done) {
  const s = net.createServer();

  s.on('error', done)
  s.on('listening', done)

  s.listen(port)

  return s
};

// check if a given port is avaiable by trying to binding and
// unbinding
const available = function(port, done) {
  const s = bind(port, function(err) {
    if (err) {
      done(false)
    } else {
      s.on('close', function() {
        done(true)
      }).close()
    }
  });
};

// try 'allocating' a range of ports by using convention.
// NOTE: don't unbind from the port until process exits; this
// convention is what prevents others call to allocatePorts
// from clobbering each other.
const allocatePorts = function(port, done) {
  const s = bind(port, function(err) {
    if (err) {
      return done(err)
    }

    const ports = range(port + 1, port + portRange);

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
  });
};

const maxPortStart = 65535 - portRange;

// retry port allocation going up by increments of portRange
const retryAllocate = function(port, done) {
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
};

// returns an array of available ports
module.exports = function(done) {
  retryAllocate(startPort, done)
}
