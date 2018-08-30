var expect = require('chai').expect
var actions = require('./actions')
var configureStore = require('./configureStore')

var mock = function(type, message) {
  return {
    title: 'Title',
    type: type || 'error',
    message: message || 'Unknown message.',
  }
}

var apply = function(fns) {
  return function() {
    fns.shift().apply(null, arguments)
  }
}

describe('Announcement reducer', function() {
  it('should start empty', function() {
    var store = configureStore()
    expect(store.getState()).to.deep.equal([])
  })

  it('should add a new announcement', function() {
    var store = configureStore()
    store.dispatch(actions.announce(mock()))
    var state = store.getState()
    expect(state).to.have.lengthOf(1)
  })

  it('should dissmiss if not error', function(done) {
    var store = configureStore()

    var events = [
      function(action) {
        expect(store.getState()).to.have.lengthOf(1)
      },
      function() {
        expect(store.getState()).to.have.lengthOf(1)
      },
      function(action) {
        expect(store.getState()).to.have.lengthOf(0)
        done()
      },
    ]

    store.subscribe(apply(events))
    store.dispatch(actions.announce(mock('warn'), 1))
  })

  it('should remove an announcement', function(done) {
    var store = configureStore([mock()])

    var events = [
      function(action) {
        expect(store.getState()).to.have.lengthOf(1)
      },
      function(action) {
        expect(store.getState()).to.have.lengthOf(0)
        done()
      },
    ]

    store.subscribe(apply(events))
    store.dispatch(actions.remove(store.getState()[0].id, 1))
  })
})
