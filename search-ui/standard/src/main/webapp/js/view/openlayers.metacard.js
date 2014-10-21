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
        'backbone',
        'marionette',
        'underscore',
        'openlayers',
        'direction',
        'wreqr'
    ],
    function (Backbone, Marionette, _, ol, dir, wreqr) {
        "use strict";
        var Views = {};


        Views.PointView = Marionette.ItemView.extend({
            modelEvents: {
                'change:context': 'toggleSelection'
            },
            initialize: function (options) {
                this.geoController = options.geoController;
                if(! options.ignoreEvents) {
                    this.listenTo(this.geoController, 'click:left', this.onMapLeftClick);
                    this.listenTo(this.geoController, 'doubleclick:left', this.onMapDoubleClick);
                }
                this.color = options.color || {red: 1, green: 0.6431372549019608, blue: 0.403921568627451, alpha: 1 };
                this.buildBillboard();
            },

            isThisPrimitive : function(event){
                // could wrap this in one huge if statement, but this seems more readable
                if(_.has(event,'object')){
                    if(event.object === this.billboard){
                        return true;
                    }
                    if(_.contains(this.lines, event.object)){
                        return true;
                    }
                }
                return false;
            },

            buildBillboard: function () {
                var point = this.model.get('geometry').getPoint();
                this.billboard = new ol.Feature({
                    geometry: new ol.geom.Point(ol.proj.transform([point.longitude, point.latitude], 'EPSG:4326',
                        'EPSG:3857'))
                });

                var iconStyle = new ol.style.Style({
                    image: new ol.style.Circle({
                        radius: 4,
                        fill: new ol.style.Fill({color: '#FFA466'}),
                        stroke: new ol.style.Stroke({color: '#914500', width: 1})
                    })
                });
                this.billboard.setStyle(iconStyle);

                var vectorSource = new ol.source.Vector({
                    features: [this.billboard]
                });

                var vectorLayer = new ol.layer.Vector({
                    source: vectorSource
                });

                this.vectorLayer = vectorLayer;
                this.geoController.mapViewer.addLayer(vectorLayer);
            },

            toggleSelection: function () {

                if (this.model.get('context')) {
                    this.billboard.scale = 0.5;
                } else {
                    this.billboard.scale = 0.41;
                }

            },
            onMapLeftClick: function (event) {
                // find out if this click is on us
                if (event === this.billboard) {
                    wreqr.vent.trigger('metacard:selected', dir.none, this.model);
                }
            },
            onMapDoubleClick: function (event) {
                // find out if this click is on us
                if (_.has(event, 'object') && event.object === this.billboard) {
                    this.geoController.flyToLocation(this.model);

                }
            },

            onClose: function () {
                // If there is already a billboard for this view, remove it
                if (!_.isUndefined(this.billboard)) {
                    this.geoController.mapViewer.removeLayer(this.vectorLayer);

                }
                this.stopListening();
            }

        });

        Views.MultiPointView = Views.PointView.extend({
            initialize: function (options) {
                Views.PointView.prototype.initialize.call(this, options);
            },

            buildBillboard: function () {
                var points = this.model.get('geometry').getMultiPoint();
                var coords = [];
                _.each(points, function(point) {
                    coords.push(ol.proj.transform([point.longitude, point.latitude], 'EPSG:4326', 'EPSG:3857'));
                });
                this.billboard = new ol.Feature({
                    geometry: new ol.geom.MultiPoint(coords)
                });

                var iconStyle = new ol.style.Style({
                    image: new ol.style.Circle({
                        radius: 4,
                        fill: new ol.style.Fill({color: '#FFA466'}),
                        stroke: new ol.style.Stroke({color: '#914500', width: 1})
                    })
                });
                this.billboard.setStyle(iconStyle);

                var vectorSource = new ol.source.Vector({
                    features: [this.billboard]
                });

                var vectorLayer = new ol.layer.Vector({
                    source: vectorSource
                });

                this.vectorLayer = vectorLayer;
                this.geoController.mapViewer.addLayer(vectorLayer);
            }
        });

        Views.LineView = Views.PointView.extend({
            initialize: function (options) {
                Views.PointView.prototype.initialize.call(this, options);
            },

            buildBillboard: function () {
                var points = this.model.get('geometry').getLineString();
                var coords = [];
                _.each(points, function(point) {
                    coords.push(ol.proj.transform([point.longitude, point.latitude], 'EPSG:4326', 'EPSG:3857'));
                });
                this.billboard = new ol.Feature({
                    geometry: new ol.geom.LineString(coords)
                });

                var iconStyle = new ol.style.Style({
                    stroke: new ol.style.Stroke({color: '#914500', width: 1})
                });
                this.billboard.setStyle(iconStyle);

                var vectorSource = new ol.source.Vector({
                    features: [this.billboard]
                });

                var vectorLayer = new ol.layer.Vector({
                    source: vectorSource
                });

                this.vectorLayer = vectorLayer;
                this.geoController.mapViewer.addLayer(vectorLayer);
            }
        });

        Views.MultiLineView = Views.LineView.extend({
            buildBillboard: function () {
                var points = this.model.get('geometry').getMultiLineString();
                var coords = [];
                var mls = new ol.geom.MultiLineString(coords);
                _.each(points, function(line) {
                    coords = [];
                    _.each(line, function(point) {
                        coords.push(ol.proj.transform([point.longitude, point.latitude], 'EPSG:4326', 'EPSG:3857'));
                    });
                    mls.appendLineString(new ol.geom.LineString(coords));
                });
                this.billboard = new ol.Feature({
                    geometry: mls
                });

                var iconStyle = new ol.style.Style({
                    stroke: new ol.style.Stroke({color: '#914500', width: 1})
                });
                this.billboard.setStyle(iconStyle);

                var vectorSource = new ol.source.Vector({
                    features: [this.billboard]
                });

                var vectorLayer = new ol.layer.Vector({
                    source: vectorSource
                });

                this.vectorLayer = vectorLayer;
                this.geoController.mapViewer.addLayer(vectorLayer);
            }
        });

        Views.RegionView = Views.PointView.extend({
            initialize: function (options) {
                Views.PointView.prototype.initialize.call(this, options);
            },
            buildBillboard: function () {
                var points = this.model.get('geometry').getPolygon();
                var coords = [];
                _.each(points, function(point) {
                    coords.push(ol.proj.transform([point.longitude, point.latitude], 'EPSG:4326', 'EPSG:3857'));
                });
                this.billboard = new ol.Feature({
                    geometry: new ol.geom.LineString(coords)
                });

                var iconStyle = new ol.style.Style({
                    stroke: new ol.style.Stroke({color: '#914500', width: 1})
                });
                this.billboard.setStyle(iconStyle);

                var vectorSource = new ol.source.Vector({
                    features: [this.billboard]
                });

                var vectorLayer = new ol.layer.Vector({
                    source: vectorSource
                });

                this.vectorLayer = vectorLayer;
                this.geoController.mapViewer.addLayer(vectorLayer);
            }
        });

        Views.MultiRegionView = Views.RegionView.extend({
            buildBillboard: function () {
                var points = this.model.get('geometry').getMultiPolygon();
                var coords = [];
                var mpg = new ol.geom.MultiLineString(coords);
                _.each(points, function(polygon) {
                    coords = [];
                    _.each(polygon, function (point) {
                        coords.push(ol.proj.transform([point.longitude, point.latitude], 'EPSG:4326', 'EPSG:3857'));
                    });
                    mpg.appendLineString(new ol.geom.LineString(coords));
                });
                this.billboard = new ol.Feature({
                    geometry: mpg
                });

                var iconStyle = new ol.style.Style({
                    stroke: new ol.style.Stroke({color: '#914500', width: 1})
                });
                this.billboard.setStyle(iconStyle);

                var vectorSource = new ol.source.Vector({
                    features: [this.billboard]
                });

                var vectorLayer = new ol.layer.Vector({
                    source: vectorSource
                });

                this.vectorLayer = vectorLayer;
                this.geoController.mapViewer.addLayer(vectorLayer);
            }
        });

        Views.GeometryCollectionView = Views.PointView.extend({
            initialize: function (options) {
                options.color = options.color || {red: 1, green: 1, blue: 0.403921568627451, alpha: 1 };
                options.polygonColor = options.polygonColor || {red: 1, green: 1, blue: 0.404, alpha: 0.2 };

                this.buildGeometryCollection(options);
                Views.PointView.prototype.initialize.call(this, options);
            },

            buildGeometryCollection: function (options) {
                var collection = this.model.get('geometry');

                this.geometries = _.map(collection.getGeometryCollection(), function(geo) {

                    var subOptions = _.clone(options);
                    var subModel = _.clone(options.model);
                    subOptions.ignoreEvents = true;
                    subModel.set('geometry', geo);
                    subOptions.model = subModel;
                    if (geo.isPoint()) {
                        return new Views.PointView(subOptions);
                    } else if (geo.isMultiPoint()) {
                        return new Views.MultiPointView(subOptions);
                    } else if (geo.isPolygon()) {
                        return new Views.RegionView(subOptions);
                    } else if (geo.isMultiPolygon()) {
                        return new Views.MultiRegionView(subOptions);
                    }  else if (geo.isLineString()) {
                        return new Views.LineView(subOptions);
                    } else if (geo.isMultiLineString()) {
                        return new Views.MultiLineView(subOptions);
                    } else if (geo.isGeometryCollection()) {
                        return new Views.GeometryCollectionView(subOptions);
                    } else {
                        throw new Error("No view for this geometry");
                    }
                });
                this.model.set('geometry', collection);
            },

            buildBillboard: function () {
            },

            toggleSelection: function () {
                var view = this;

                _.each(view.geometries, function(geometry) {
                    geometry.toggleSelection();
                });
            },

            onMapLeftClick: function (event) {
                var view = this;

                _.each(view.geometries, function(geometry) {
                    geometry.onMapLeftClick(event);
                });
            },

            onMapDoubleClick: function (event) {
                var view = this;
                _.each(view.geometries, function(geometry) {
                    geometry.onMapDoubleClick(event);
                });
            },

            onClose: function () {
                var view = this;

                _.each(view.geometries, function(geometry) {
                    geometry.onClose();
                });

                this.stopListening();
            }
        });


        Views.ResultsView = Marionette.CollectionView.extend({
            itemView: Backbone.View,
            initialize: function (options) {
                this.geoController = options.geoController;
            },

            // get the child view by item it holds, and remove it
            removeItemView: function (item) {
                var view = this.children.findByModel(item.get('metacard'));
                this.removeChildView(view);
                this.checkEmpty();
            },

            buildItemView: function (item, ItemViewType, itemViewOptions) {
                var metacard = item.get('metacard'),
                    geometry = metacard.get('geometry'),
                    ItemView;
                if (!geometry) {
                    var opts = _.extend({model: metacard}, itemViewOptions);
                    return new ItemViewType(opts);
                }
                // build the final list of options for the item view type.
                var options = _.extend({model: metacard, geoController: this.geoController}, itemViewOptions);

                if (geometry.isPoint()) {
                    ItemView = Views.PointView;
                } else if (geometry.isMultiPoint()) {
                    ItemView = Views.MultiPointView;
                } else if (geometry.isPolygon()) {
                    ItemView = Views.RegionView;
                } else if (geometry.isMultiPolygon()) {
                    ItemView = Views.MultiRegionView;
                }  else if (geometry.isLineString()) {
                    ItemView = Views.LineView;
                } else if (geometry.isMultiLineString()) {
                    ItemView = Views.MultiLineView;
                } else if (geometry.isGeometryCollection()) {
                    ItemView = Views.GeometryCollectionView;
                } else {
                    throw new Error("No view for this geometry");
                }

                // create the item view instance
                var view = new ItemView(options);
                // return it
                return view;
            }
        });
        return Views;
    });
