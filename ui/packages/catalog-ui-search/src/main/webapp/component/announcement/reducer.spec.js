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

describe('Announcement reducer', () => {
  it('should start empty', () => {
    const store = configureStore()
    expect(store.getState()).to.deep.equal([])
  })

  it('should add a new announcement', () => {
    const store = configureStore()
    store.dispatch(actions.announce(mock()))
    const state = store.getState()
    expect(state).to.have.lengthOf(1)
  })

  it('should dissmiss if not error', done => {
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

  it('should remove an announcement', done => {
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
