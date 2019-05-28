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

const template = require('./combined-map.hbs')
const Marionette = require('marionette')
const CustomElements = require('../../../js/CustomElements.js')
const CesiumView = require('../maps/cesium/cesium.view.js')
const OpenlayersView = require('../maps/openlayers/openlayers.view.js')
const featureDetection = require('../../singletons/feature-detection.js')

module.exports = Marionette.LayoutView.extend({
  tagName: CustomElements.register('combined-map'),
  template,
  regions: {
    mapContainer: '> .map-container',
  },
  onRender() {
    this.listenToOnce(featureDetection, 'change:cesium', this.render)
    if (featureDetection.supportsFeature('cesium')) {
      this.mapContainer.show(new CesiumView(this.options))
    } else {
      this.mapContainer.show(new OpenlayersView(this.options))
    }
  },
})
