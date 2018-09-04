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

define([
  'underscore',
  'marionette',
  'cesium',
  'js/controllers/common.layerCollection.controller',
  'properties',
], function(_, Marionette, Cesium, CommonLayerController, properties) {
  'use strict'

  var imageryProviderTypes = {
    OSM: Cesium.OpenStreetMapImageryProvider,
    AGM: Cesium.ArcGisMapServerImageryProvider,
    BM: Cesium.BingMapsImageryProvider,
    WMS: Cesium.WebMapServiceImageryProvider,
    WMT: Cesium.WebMapTileServiceImageryProvider,
    TMS: Cesium.TileMapServiceImageryProvider,
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
      this.map = new Cesium.Viewer(options.divId, options.cesiumOptions)

      this.collection.forEach(function(model) {
        var type = imageryProviderTypes[model.get('type')]
        var initObj = _.omit(
          model.attributes,
          'type',
          'label',
          'index',
          'modelCid'
        )

        /* Set the tiling scheme for WMTS imagery providers that are EPSG:4326 */
        if (
          model.get('type') === 'WMT' &&
          properties.projection === 'EPSG:4326'
        ) {
          initObj.tilingScheme = new Cesium.GeographicTilingScheme()
        }

        var provider = new type(initObj)
        var layer = this.map.imageryLayers.addImageryProvider(provider)
        this.layerForCid[model.cid] = layer
        layer.alpha = model.get('alpha')
        layer.show = model.get('show')
      }, this)

      this.isMapCreated = true
      return this.map
    },
    onDestroy: function() {
      if (this.isMapCreated) {
        this.map.destroy()
        this.map = null
      }
    },
    setAlpha: function(model) {
      var layer = this.layerForCid[model.cid]
      layer.alpha = model.get('alpha')
    },
    setShow: function(model) {
      var layer = this.layerForCid[model.cid]
      layer.show = model.get('show')
    },
    reIndexLayers: function() {
      /*
             removing/re-adding the layers causes visible "re-render" of entire map;
             raising/lowering is smoother.
             */
      this.collection.forEach(function(model, index) {
        var layer = this.layerForCid[model.cid]
        var prevIndex = this.map.imageryLayers.indexOf(layer)
        var indexChange = index - prevIndex
        var count = Math.abs(indexChange)
        var method = indexChange > 0 ? 'raise' : 'lower'
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

  return Controller
})
