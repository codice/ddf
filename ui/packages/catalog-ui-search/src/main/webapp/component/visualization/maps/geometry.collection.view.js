/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

var Backbone = require('backbone')
var Marionette = require('marionette')
var $ = require('jquery')
var _ = require('underscore')
var store = require('../../../js/store.js')
var GeometryView = require('./geometry.view')

var GeometryCollectionView = Marionette.CollectionView.extend({
  childView: GeometryView,
  childViewOptions: function() {
    return {
      map: this.options.map,
      selectionInterface: this.options.selectionInterface,
      clusterCollection: this.options.clusterCollection,
    }
  },
  initialize: function(options) {
    this.render = _.throttle(this.render, 200)
    this.options.map.onLeftClick(this.onMapLeftClick.bind(this))
    this.render()
  },
  onMapLeftClick: function(event, mapEvent) {
    if (
      mapEvent.mapTarget &&
      mapEvent.mapTarget !== 'userDrawing' &&
      !store.get('content').get('drawing')
    ) {
      if (event.shiftKey) {
        this.handleShiftClick(mapEvent.mapTarget)
      } else if (event.ctrlKey || event.metaKey) {
        this.handleCtrlClick(mapEvent.mapTarget)
      } else {
        this.handleClick(mapEvent.mapTarget)
      }
    }
  },
  handleClick: function(id) {
    if (id.constructor === String) {
      this.options.selectionInterface.clearSelectedResults()
      this.options.selectionInterface.addSelectedResult(this.collection.get(id))
    }
  },
  handleCtrlClick: function(id) {
    if (id.constructor === String) {
      this.options.selectionInterface.addSelectedResult(this.collection.get(id))
    }
  },
  handleShiftClick: function(id) {
    if (id.constructor === String) {
      this.options.selectionInterface.addSelectedResult(this.collection.get(id))
    }
  },
})

module.exports = GeometryCollectionView
