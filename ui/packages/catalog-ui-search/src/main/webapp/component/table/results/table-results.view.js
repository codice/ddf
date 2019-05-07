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

const TableView = require('../table.view')
const HeaderView = require('../../visualization/table/thead.view.js')
const BodyView = require('../../visualization/table/tbody.view.js')
const $ = require('jquery')

function getOriginalEvent(e) {
  if (e.constructor === MouseEvent) {
    return e
  }
  return getOriginalEvent(e.originalEvent)
}

module.exports = TableView.extend({
  className: 'is-results',
  getHeaderView: function() {
    return new HeaderView({
      selectionInterface: this.options.selectionInterface,
    })
  },
  getBodyView: function() {
    return new BodyView({
      selectionInterface: this.options.selectionInterface,
      collection: this.options.selectionInterface.getActiveSearchResults(),
    })
  },
  events: {
    resize: 'resize',
  },
  resize: function(e) {
    e = getOriginalEvent(e)
    const newWidth = this.$el.find('table').width() + e.movementX
    this.$el.find('table').css('width', newWidth)
  },
})
