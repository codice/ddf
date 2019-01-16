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
/*global define*/
/*jshint newcap: false, bitwise: false */

const _ = require('underscore')
const Marionette = require('marionette')
const Cesium = require('cesium')
const CommonLayerController = require('./common.layerCollection.controller.js')
const properties = require('../properties.js')

const DEFAULT_HTTPS_PORT = 443
const DEFAULT_HTTP_PORT = 80

var imageryProviderTypes = {
  OSM: Cesium.createOpenStreetMapImageryProvider,
  AGM: Cesium.ArcGisMapServerImageryProvider,
  BM: Cesium.BingMapsImageryProvider,
  WMS: Cesium.WebMapServiceImageryProvider,
  WMT: Cesium.WebMapTileServiceImageryProvider,
  TMS: Cesium.createTileMapServiceImageryProvider,
  GE: Cesium.GoogleEarthImageryProvider,
  CT: Cesium.CesiumTerrainProvider,
  AGS: Cesium.ArcGisImageServerTerrainProvider,
  VRW: Cesium.VRTheWorldTerrainProvider,
  SI: Cesium.SingleTileImageryProvider,
}

var Controller = CommonLayerController.extend({
  initialize: function() {
    // there is no automatic chaining of initialize.
    CommonLayerController.prototype.initialize.apply(this, arguments)
  },
  makeMap: function(options) {
    // must create cesium map after containing DOM is attached.
    this.map = new Cesium.Viewer(options.element, options.cesiumOptions)

    this.collection.forEach(function(model) {
      if (model.get('show')) {
        this.initLayer(model)
      }
    }, this)

    this.isMapCreated = true
    return this.map
  },
  initLayer: function(model) {
    var type = imageryProviderTypes[model.get('type')]
    var initObj = _.omit(model.attributes, 'type', 'label', 'index', 'modelCid')

    if (model.get('type') === 'WMT') {
      /* If matrixSet is present (OpenLayers WMTS keyword) set tileMatrixSetID (Cesium WMTS keyword) */
      if (initObj.matrixSet) {
        initObj.tileMatrixSetID = initObj.matrixSet
      }
      /* Set the tiling scheme for WMTS imagery providers that are EPSG:4326 */
      if (properties.projection === 'EPSG:4326') {
        initObj.tilingScheme = new Cesium.GeographicTilingScheme()
      }
    }

    var provider = new type(initObj)

    /*
      Optionally add this provider as a TrustedServer. This sets withCredentials = true
      on the XmlHttpRequests for CORS.
    */
    if (model.get('withCredentials')) {
      const url = require('url')
      var parsedUrl = url.parse(provider.url)
      var port = parsedUrl.port
      if (!port) {
        port =
          parsedUrl.protocol === 'https:'
            ? DEFAULT_HTTPS_PORT
            : DEFAULT_HTTP_PORT
      }
      Cesium.TrustedServers.add(parsedUrl.hostname, port)
    }
    var layer = this.map.imageryLayers.addImageryProvider(provider, 0) // the collection is sorted by order, so later things should go at bottom of stack
    this.layerForCid[model.id] = layer
    layer.alpha = model.get('alpha')
    layer.show = model.shouldShowLayer()
  },
  onDestroy: function() {
    if (this.isMapCreated) {
      this.map.destroy()
      this.map = null
    }
  },
  setAlpha: function(model) {
    var layer = this.layerForCid[model.id]
    layer.alpha = model.get('alpha')
  },
  setShow: function(model) {
    if (!this.layerForCid[model.id]) {
      this.initLayer(model)
    }
    var layer = this.layerForCid[model.id]
    layer.show = model.shouldShowLayer()
  },
  /*
    removing/re-adding the layers causes visible "re-render" of entire map;
    raising/lowering is smoother.
    raising means to move to a higher index.  higher indexes are displayed on top of lower indexes.
    so we have to reverse the order property here to make it display correctly.  
    in other words, order 1 means highest index.
  */
  reIndexLayers: function() {
    this.collection.forEach(function(model, index) {
      var layer = this.layerForCid[model.id]
      var previousOrder = this.map.imageryLayers.indexOf(layer) + 1
      var currentOrder = this.collection.length - model.get('order') // order is backwards on cesium (higher indexes are displayed above lower)
      var method = currentOrder > previousOrder ? 'raise' : 'lower' // raise means move to higher index :(
      var count = Math.abs(currentOrder - previousOrder)
      // console.log(method + " " + model.get('name') + " " + count);  // useful for debugging!
      _.times(
        count,
        function() {
          this.map.imageryLayers[method](layer)
        },
        this
      )
    }, this)
  },
})

Controller.imageryProviderTypes = imageryProviderTypes

module.exports = Controller
