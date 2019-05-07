const expect = require('chai').expect
const actions = require('./actions')
const configureStore = require('./configureStore')

const mock = function(type, message) {
  return {
    title: 'Title',
    type: type || 'error',
    message: message || 'Unknown message.',
  }
}

const apply = function(fns) {
  return function() {
    fns.shift().apply(null, arguments)
  }
}

describe('Announcement reducer', function() {
  it('should start empty', function() {
    const store = configureStore()
    expect(store.getState()).to.deep.equal([])
  })

  it('should add a new announcement', function() {
    const store = configureStore()
    store.dispatch(actions.announce(mock()))
    const state = store.getState()
    expect(state).to.have.lengthOf(1)
  })

  it('should dissmiss if not error', function(done) {
    const store = configureStore()

    const events = [
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
    const store = configureStore([mock()])

    const events = [
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
