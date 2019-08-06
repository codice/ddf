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

var React = require('react')
var utils = require('react-addons-test-utils')
var expect = require('chai').expect

var configureStore = require('./configureStore')
var Announcements = require('./announcements.jsx')

var mock = function(type, message) {
  return {
    id: 0,
    title: 'Title',
    type: type || 'error',
    message: message || 'Unknown message.',
  }
}

describe('<Announcements/>', function() {
  it('should render correctly', function() {
    var store = configureStore([mock()])
    var node = utils.renderIntoDocument(<Announcements store={store} />)
    var found = utils.scryRenderedDOMComponentsWithClass(node, 'announcement')
    expect(found).to.have.lengthOf(1)
  })
})
