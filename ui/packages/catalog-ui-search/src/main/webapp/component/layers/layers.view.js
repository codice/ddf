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
/* global require*/
const _ = require('underscore')
const Marionette = require('marionette')
const Backbone = require('backbone')
const properties = require('../../js/properties.js')
const template = require('./layers.hbs')
const LayerItemCollectionView = require('../layer-item/layer-item.collection.view.js')
const user = require('../singletons/user-instance.js')
const CustomElements = require('../../js/CustomElements.js')

// this is to track focus, since on reordering rerenders and loses focus
const FocusModel = Backbone.Model.extend({
  defaults: {
    id: undefined,
    direction: undefined,
  },
  directions: {
    up: 'up',
    down: 'down',
  },
  clear: function() {
    this.set({
      id: undefined,
      direction: undefined,
    })
  },
  setUp: function(id) {
    this.set({
      id: id,
      direction: this.directions.up,
    })
  },
  setDown: function(id) {
    this.set({
      id: id,
      direction: this.directions.down,
    })
  },
  getDirection: function() {
    return this.get('direction')
  },
  isUp: function() {
    return this.getDirection() === this.directions.up
  },
  isDown: function() {
    return this.getDirection() === this.directions.down
  },
})

module.exports = Marionette.LayoutView.extend({
  tagName: CustomElements.register('layers'),
  setDefaultModel: function() {
    this.model = user.get('user>preferences')
  },
  events: {
    'click > .footer button': 'resetDefaults',
  },
  template: template,
  regions: {
    layers: '> .layers',
  },
  initialize: function(options) {
    if (options.model === undefined) {
      this.setDefaultModel()
    }
    this.listenToModel()
    this.setupFocusModel()
  },
  setupFocusModel: function() {
    this.focusModel = new FocusModel()
  },
  onRender: function() {
    this.layers.show(
      new LayerItemCollectionView({
        collection: this.model.get('mapLayers'),
        updateOrdering: this.updateOrdering.bind(this),
        focusModel: this.focusModel,
      })
    )
  },
  listenToModel: function() {
    this.stopListeningToModel()
    this.listenTo(
      this.model.get('mapLayers'),
      'change:alpha change:show',
      this.save
    )
  },
  stopListeningToModel: function() {
    this.stopListening(
      this.model.get('mapLayers'),
      'change:alpha change:show',
      this.save
    )
  },
  resetDefaults: function() {
    this.focusModel.clear()
    this.stopListeningToModel()
    this.model.get('mapLayers').forEach(function(viewLayer) {
      const name = viewLayer.get('name')
      const defaultConfig = _.find(properties.imageryProviders, function(
        layerObj
      ) {
        return name === layerObj.name
      })
      viewLayer.set(defaultConfig)
    })
    this.model.get('mapLayers').sort()
    this.save()
    this.listenToModel()
  },
  updateOrdering: function() {
    _.forEach(
      this.$el.find(`${CustomElements.getNamespace()}layer-item`),
      (element, index) => {
        this.model
          .get('mapLayers')
          .get(element.getAttribute('data-id'))
          .set('order', index)
      }
    )
    this.model.get('mapLayers').sort()
    this.save()
  },
  save: function() {
    this.model.savePreferences()
  },
})
