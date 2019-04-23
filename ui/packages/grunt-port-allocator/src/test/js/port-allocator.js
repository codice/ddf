const net = require('net')

const tap = require('tap')
const allocator = require('../../main/js/lib/port-allocator.js')

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

    const s = net.createServer()

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
