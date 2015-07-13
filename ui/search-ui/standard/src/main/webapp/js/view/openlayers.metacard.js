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
        'properties',
        'wreqr',
        'application'
    ],
    function (Backbone, Marionette, _, ol, dir, properties, wreqr, Application) {
        "use strict";
        var Views = {};

        var transformPoint = function (point) {
            var coords = [point.longitude, point.latitude];
            return ol.proj.transform(coords, 'EPSG:4326', properties.projection);
        };

        var getPoint = function (model) {
            return new ol.geom.Point(transformPoint(model.getPoint()));
        };

        var addCenterPoint = function (model, geom) {
            return new ol.geom.GeometryCollection([getPoint(model), geom]);
        };

        var getMultiPoint = function (model) {
            var points = model.getMultiPoint();

            var coords = [];
            _.each(points, function(point) {
                coords.push(transformPoint(point));
            });

            return new ol.geom.MultiPoint(coords);
        };

        var getLineString = function (model) {
            var points = model.getLineString();
            var coords = [];
            _.each(points, function(point) {
                coords.push(transformPoint(point));
            });
            return new ol.geom.LineString(coords);
        };

        var getMultiLineString = function (model) {
            var points = model.getMultiLineString();
            var coords = [];
            var mls = new ol.geom.MultiLineString(coords);
            _.each(points, function (line) {
                coords = [];
                _.each(line, function (point) {
                    coords.push(transformPoint(point));
                });
                mls.appendLineString(new ol.geom.LineString(coords));
            });
            return mls;
        };

        var getPolygon = function (model) {
            var points = model.getPolygon();
            var coords = [];
            _.each(points, function(point) {
                coords.push(transformPoint(point));
            });

            return new ol.geom.LineString(coords);
        };

        var getMultiPolygon = function (model) {
            var points = model.getMultiPolygon();
            var coords = [];
            var mpg = new ol.geom.MultiLineString(coords);
            _.each(points, function (polygon) {
                coords = [];
                _.each(polygon, function (point) {
                    coords.push(transformPoint(point));
                });
                mpg.appendLineString(new ol.geom.LineString(coords));
            });
            return mpg;
        };

        var getGeometryCollection = function (model) {
            var geometries = _.map(model.getGeometryCollection(), function(geo) {
                if (geo.isPoint()) {
                    return getPoint(geo);
                } else if (geo.isMultiPoint()) {
                    return getMultiPoint(geo);
                }  else if (geo.isLineString()) {
                    return getLineString(geo);
                } else if (geo.isMultiLineString()) {
                    return getMultiLineString(geo);
                } else if (geo.isPolygon()) {
                    return getPolygon(geo);
                } else if (geo.isMultiPolygon()) {
                    return getMultiPolygon(geo);
                } else if (geo.isGeometryCollection()) {
                    return getGeometryCollection(geo);
                } else {
                    throw new Error("No method for this geometry");
                }
            });

            return new ol.geom.GeometryCollection(geometries);
        };

        Views.PointView = Marionette.ItemView.extend({
            modelEvents: {
                'change:context': 'toggleSelection'
            },

            getGeometry: getPoint,
            pointStrokeColor: 'rgba(0,0,0,1)',
            lineStrokeColor: 'rgba(255,255,255,1)',

            initialize: function (options) {
                this.pointFillColor = options.pointFillColor || Application.UserModel.get('user>preferences>pointColor');
                this.geoController = options.geoController;
                if (!options.ignoreEvents) {
                    this.listenTo(this.geoController, 'click:left', this.onMapLeftClick);
                    this.listenTo(this.geoController, 'doubleclick:left', this.onMapDoubleClick);
                }
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
                var model = this.model.get('geometry');
                this.billboard = new ol.Feature({
                    geometry: addCenterPoint(model, this.getGeometry(model))
                });

                this.billboard.setStyle(new ol.style.Style({
                    image: new ol.style.Circle({
                        radius: 4,
                        fill: new ol.style.Fill({color: this.pointFillColor}),
                        stroke: new ol.style.Stroke({color: this.pointStrokeColor, width: 1})
                    }),
                    stroke: new ol.style.Stroke({color: this.lineStrokeColor, width: 1})
                }));

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
                var radius, strokeColor;
                if (this.model.get('context')) {
                    radius = 7;
                    strokeColor = '#000000';
                } else {
                    radius = 4;
                    strokeColor = this.pointStrokeColor;
                }
                this.billboard.setStyle(new ol.style.Style({
                    image: new ol.style.Circle({
                        radius: radius,
                        fill: new ol.style.Fill({color: this.pointFillColor}),
                        stroke: new ol.style.Stroke({color: strokeColor, width: 1})
                    }),
                    stroke: new ol.style.Stroke({color: this.lineStrokeColor, width: 1})
                }));
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

            onDestroy: function () {
                // If there is already a billboard for this view, remove it
                if (!_.isUndefined(this.billboard)) {
                    this.geoController.mapViewer.removeLayer(this.vectorLayer);
                }
                this.stopListening();
            }

        });

        Views.MultiPointView = Views.PointView.extend({
            getGeometry: getMultiPoint
        });

        Views.LineView = Views.PointView.extend({
            getGeometry: getLineString
        });

        Views.MultiLineView = Views.LineView.extend({
            getGeometry: getMultiLineString
        });

        Views.RegionView = Views.PointView.extend({
            getGeometry: getPolygon
        });

        Views.MultiRegionView = Views.RegionView.extend({
            getGeometry: getMultiPolygon
        });

        Views.GeometryCollectionView = Views.PointView.extend({
            getGeometry: getGeometryCollection
        });

        Views.ResultsView = Marionette.CollectionView.extend({
            childView: Backbone.View,
            initialize: function (options) {
                this.geoController = options.geoController;
            },

            // get the child view by item it holds, and remove it
            removeItemView: function (item) {
                var view = this.children.findByModel(item.get('metacard'));
                this.removeChildView(view);
                this.checkEmpty();
            },

            buildChildView: function (item, ItemViewType, childViewOptions) {
                var metacard = item.get('metacard'),
                    geometry = metacard.get('geometry'),
                    ItemView;
                if (!geometry) {
                    var opts = _.extend({model: metacard, template: false}, childViewOptions);
                    return new ItemViewType(opts);
                }
                // build the final list of options for the item view type.
                var options = _.extend({
                    model: metacard,
                    geoController: this.geoController,
                    template: false
                }, childViewOptions);

                if (geometry.isPoint()) {
                    ItemView = Views.PointView;
                } else if (geometry.isMultiPoint()) {
                    options.pointFillColor = Application.UserModel.get('user>preferences>multiPointColor');
                    ItemView = Views.MultiPointView;
                } else if (geometry.isPolygon()) {
                    options.pointFillColor = Application.UserModel.get('user>preferences>polygonColor');
                    ItemView = Views.RegionView;
                } else if (geometry.isMultiPolygon()) {
                    options.pointFillColor = Application.UserModel.get('user>preferences>multiPolygonColor');
                    ItemView = Views.MultiRegionView;
                }  else if (geometry.isLineString()) {
                    options.pointFillColor = Application.UserModel.get('user>preferences>lineColor');
                    ItemView = Views.LineView;
                } else if (geometry.isMultiLineString()) {
                    options.pointFillColor = Application.UserModel.get('user>preferences>multiLineColor');
                    ItemView = Views.MultiLineView;
                } else if (geometry.isGeometryCollection()) {
                    options.pointFillColor = Application.UserModel.get('user>preferences>geometryCollectionColor');
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
