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
    'marionette',
    'openlayers',
    'properties',
    'js/controllers/common.layerCollection.controller'
], function (_, Marionette, ol, properties, CommonLayerController) {
    "use strict";

    var imageryProviderTypes = {
        OSM: ol.source.OSM,
        BM: ol.source.BingMaps,
        WMS: ol.source.TileWMS,
        MQ: ol.source.MapQuest,
        SI: ol.source.ImageStatic
    };

    var Controller = CommonLayerController.extend({
        initialize: function () {
            // there is no automatic chaining of initialize.
            CommonLayerController.prototype.initialize.apply(this, arguments);
        },
        makeMap: function (options) {
            var layers = [];

            this.collection.forEach(function (model) {
                var widgetLayer = this.makeWidgetLayer(model);
                layers.push(widgetLayer);
                this.layerForCid[model.cid] = widgetLayer;
            }, this);

            var view = new ol.View({
                projection: ol.proj.get(properties.projection),
                center: ol.proj.transform([0, 0], 'EPSG:4326', properties.projection),
                zoom: options.zoom
            });
            var mapConfig = {
                layers: layers,
                target: options.divId,
                view: view
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
            var layer = this.layerForCid[model.cid];
            layer.setOpacity(model.get('alpha'));
        },
        setShow: function (model) {
            var layer = this.layerForCid[model.cid];
            layer.setVisible(model.get('show'));
        },
        reIndexAll: function () {
            this.collection.sort();
            var olMapLayers = this.map.getLayers();
            olMapLayers.clear();
            this.collection.forEach(function (model) {
                var widgetLayer = this.layerForCid[model.cid];
                olMapLayers.push(widgetLayer);
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
            }
            return new layerType({
                visible: model.get('show'),
                preload: Infinity,
                opacity: model.get('alpha'),
                source: new type(initObj)
            });
        }
    });

    return Controller;

});
