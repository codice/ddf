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

const _ = require('underscore')
const $ = require('jquery')
const template = require('./low-bandwidth-map.hbs')
const Marionette = require('marionette')
const CustomElements = require('../../../js/CustomElements.js')
const CombinedMapView = require('../combined-map/combined-map.view.js')
const OpenlayersView = require('../maps/openlayers/openlayers.view.js')
const router = require('../../router/router.js')

module.exports = Marionette.LayoutView.extend({
  tagName: CustomElements.register('low-bandwidth-map'),
  template: template,
  regions: {
    mapContainer: ' .map-container',
  },

  events: {
    'click .low-bandwidth-button': 'continueLoading',
    'click .low-bandwidth-button-close': 'closeMap',
  },

  initialize: function(options) {
    this.options = _.extend({}, options, {
      lowBandwidth: router.get('lowBandwidth'),
    })
  },

  onRender: function() {
    if (!this.options.lowBandwidth) {
      this.continueLoading()
    }
  },

  continueLoading: function() {
    this.$el.find('.low-bandwidth-confirmation').addClass('is-hidden')
    if (this.options.desiredContainer === 'cesium') {
      this.mapContainer.show(new CombinedMapView(this.options))
    } else {
      this.mapContainer.show(new OpenlayersView(this.options))
    }
  },

  closeMap: function() {
    this.options.container.close()
  },
})
