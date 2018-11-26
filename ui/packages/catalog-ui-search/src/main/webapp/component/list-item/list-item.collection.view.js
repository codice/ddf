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
/*global define, alert*/
var Marionette = require('marionette')
var CustomElements = require('../../js/CustomElements.js')
var ListItemView = require('./list-item.view')

module.exports = Marionette.CollectionView.extend({
  setDefaultCollection: function() {
    this.collection = this.options.workspaceLists
  },
  tagName: CustomElements.register('list-item-collection'),
  childView: ListItemView,
  initialize: function(options) {
    if (!options.collection) {
      this.setDefaultCollection()
    }
  },
})
