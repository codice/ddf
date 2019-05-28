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
  clear() {
    this.set({
      id: undefined,
      direction: undefined,
    })
  },
  setUp(id) {
    this.set({
      id,
      direction: this.directions.up,
    })
  },
  setDown(id) {
    this.set({
      id,
      direction: this.directions.down,
    })
  },
  getDirection() {
    return this.get('direction')
  },
  isUp() {
    return this.getDirection() === this.directions.up
  },
  isDown() {
    return this.getDirection() === this.directions.down
  },
})

module.exports = Marionette.LayoutView.extend({
  tagName: CustomElements.register('layers'),
  setDefaultModel() {
    this.model = user.get('user>preferences')
  },
  events: {
    'click > .footer button': 'resetDefaults',
  },
  template,
  regions: {
    layers: '> .layers',
  },
  initialize(options) {
    if (options.model === undefined) {
      this.setDefaultModel()
    }
    this.listenToModel()
    this.setupFocusModel()
  },
  setupFocusModel() {
    this.focusModel = new FocusModel()
  },
  onRender() {
    this.layers.show(
      new LayerItemCollectionView({
        collection: this.model.get('mapLayers'),
        updateOrdering: this.updateOrdering.bind(this),
        focusModel: this.focusModel,
      })
    )
  },
  listenToModel() {
    this.stopListeningToModel()
    this.listenTo(
      this.model.get('mapLayers'),
      'change:alpha change:show',
      this.save
    )
  },
  stopListeningToModel() {
    this.stopListening(
      this.model.get('mapLayers'),
      'change:alpha change:show',
      this.save
    )
  },
  resetDefaults() {
    this.focusModel.clear()
    this.stopListeningToModel()
    this.model.get('mapLayers').forEach(viewLayer => {
      const name = viewLayer.get('name')
      const defaultConfig = _.find(
        properties.imageryProviders,
        layerObj => name === layerObj.name
      )
      viewLayer.set(defaultConfig)
    })
    this.model.get('mapLayers').sort()
    this.save()
    this.listenToModel()
  },
  updateOrdering() {
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
  save() {
    this.model.savePreferences()
  },
})
