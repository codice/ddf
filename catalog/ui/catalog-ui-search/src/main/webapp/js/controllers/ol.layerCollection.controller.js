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

define(['underscore',
    'jquery',
    'marionette',
    'openlayers',
    'properties',
    'js/controllers/common.layerCollection.controller'
], function (_, $, Marionette, ol, properties, CommonLayerController) {
    "use strict";

    var imageryProviderTypes = {
        OSM: ol.source.OSM,
        BM: ol.source.BingMaps,
        WMS: ol.source.TileWMS,
        WMT: ol.source.WMTS,
        MQ: ol.source.MapQuest,
        AGM: ol.source.XYZ,
        SI: ol.source.ImageStatic
    };

    var mockTileGrid = {
        getMinZoom: function(){
            return 0;
        },
        getMatrixId: function(){
            return 0;
        },
        getZForResolution: function(){
            return 0;
        },
        getResolution: function(){
            return 0;
        },
        getTileRangeForExtentAndResolution: function(){
            return {
                getWidth: function() {
                    return 0;
                },
                getHeight: function(){
                    return 0;
                }
            };
        },
        getTileRangeExtent: function(){
            return 0;
        },
        getTileSize: function(){
            return 0;
        },
        getTileRangeForExtentAndZ: function(){
            return {
                getWidth: function() {
                    return 0;
                },
                getHeight: function(){
                    return 0;
                }
            };
        }
    };

    var Controller = CommonLayerController.extend({
        initialize: function () {
            // there is no automatic chaining of initialize.
            CommonLayerController.prototype.initialize.apply(this, arguments);
        },
        makeMap: function (options) {
            var layers = [];

            this.collection.forEach(function (model, index) {
                var widgetLayer = this.makeWidgetLayer(model);
                layers.push(widgetLayer);
                this.layerForCid[model.id] = widgetLayer;
                widgetLayer.setZIndex(-(index + 1));
            }, this);

            var view = new ol.View({
                projection: ol.proj.get(properties.projection),
                center: ol.proj.transform([0, 0], 'EPSG:4326', properties.projection),
                zoom: options.zoom
            });
            var mapConfig = {
                layers: layers,
                target: options.element,
                view: view,
                interactions: ol.interaction.defaults({doubleClickZoom: false})
            };
            if (options.controls !== undefined) {
                mapConfig.controls = options.controls;
            }

            this.map = new ol.Map(mapConfig);
            this.isMapCreated = true;
            return this.map;
        },
        onDestroy: function () {
            if (this.isMapCreated) {
                this.map.setTarget(null);
                this.map = null;
            }
        },
        setAlpha: function (model) {
            var layer = this.layerForCid[model.id];
            layer.setOpacity(model.get('alpha'));
        },
        setShow: function (model) {
            var layer = this.layerForCid[model.id];
            layer.setVisible(model.shouldShowLayer());
        },
        reIndexLayers: function () {
            this.collection.forEach(function (model, index) {
                var widgetLayer = this.layerForCid[model.id];
                widgetLayer.setZIndex(-(index + 1));
            }, this);
        },
        makeWidgetLayer: function (model) {
            var typeStr = model.get('type');
            var type = imageryProviderTypes[typeStr];
            var initObj = _.omit(model.attributes, 'type', 'label', 'index', 'show', 'alpha', 'modelCid');
            var layerType = ol.layer.Tile;

            if (typeStr === 'OSM') {
                if (initObj.url && initObj.url.indexOf('/{z}/{x}/{y}') === -1) {
                    initObj.url = initObj.url + '/{z}/{x}/{y}.png';
                }
            } else if (typeStr === 'BM') {
                if (!initObj.imagerySet) {
                    initObj.imagerySet = initObj.url;
                }
            } else if (typeStr === 'WMS') {
                if (!initObj.params) {
                    initObj.params = {
                        LAYERS: initObj.layers
                    };
                    _.extend(initObj.params, initObj.parameters);
                }
            } else if (typeStr === 'SI') {
                layerType = ol.layer.Image;
                if (!initObj.imageExtent) {
                    initObj.imageExtent = ol.proj.get(properties.projection).getExtent();
                }
                if (initObj.parameters) {
                    _.extend(initObj, initObj.parameters);
                }
            } else if (typeStr === 'AGM'){
                if (initObj.url && initObj.url.indexOf('/tile/{z}/{y}/{x}') === -1) {
                    initObj.url = initObj.url + '/tile/{z}/{y}/{x}';
                }
            } else if (typeStr === 'WMT') {
                /* If tileMatrixSetID is present (Cesium WMTS keyword) set matrixSet (OpenLayers WMTS keyword) */
                if (initObj.tileMatrixSetID) {
                    initObj.matrixSet = initObj.tileMatrixSetID;
                }

                /* We must mock the tileGrid in order to prevent errors in OpenLayers before the GetCapabilities request returns */
                initObj.tileGrid = mockTileGrid;

                $.ajax({
                  url : initObj.url + '?request=GetCapabilities',
                  success : function(data)  {
                      var parser = new ol.format.WMTSCapabilities();
                      var result = parser.read(data);
                      var options = ol.source.WMTS.optionsFromCapabilities(result, {layer: initObj.layer, matrixSet: initObj.matrixSet});
                      /* Replace URL with Proxy URL */
                      options.urls = [initObj.url];
                      initObj = options;
                      var layer = new layerType({
                            visible: model.shouldShowLayer(),
                            preload: Infinity,
                            opacity: model.get('alpha'),
                            source: new type(initObj)
                        });
                      var olMapLayers = this.map.getLayers();
                      olMapLayers.forEach(function(existingLayer, index){
                        if (existingLayer === this.layerForCid[model.id]){
                            this.layerForCid[model.id] = layer;
                            olMapLayers.setAt(index, this.layerForCid[model.id]);
                        }
                      }.bind(this));
                  }.bind(this)
                });
            }

            return new layerType({
                visible: model.shouldShowLayer(),
                preload: Infinity,
                opacity: model.get('alpha'),
                source: new type(initObj)
            });
        }
    });

    return Controller;

});
