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
        'cesium',
        'direction',
        'wreqr'
    ],
    function (Backbone, Marionette, _, Cesium, dir, wreqr) {
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
                this.imageIndex = options.imageIndex || 0;
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
                this.billboard = this.geoController.billboardCollection.add({
                    imageIndex: this.imageIndex,
                    position: this.geoController.ellipsoid.cartographicToCartesian(
                        Cesium.Cartographic.fromDegrees(
                            point.longitude,
                            point.latitude,
                            point.altitude
                        )
                    ),
                    horizontalOrigin: Cesium.HorizontalOrigin.CENTER,
                    verticalOrigin: Cesium.VerticalOrigin.BOTTOM,
                    scaleByDistance: new Cesium.NearFarScalar(1.0, 1.0, 1.5e7, 0.5)
                });
                this.billboard.color = this.color;
                this.billboard.scale = 0.41;
                this.billboard.hasScale = true;
            },

            toggleSelection: function () {
                if (this.billboard.eyeOffset.z < 0) {
                    this.billboard.eyeOffset = new Cesium.Cartesian3(0, 0, 0);
                } else {
                    this.billboard.eyeOffset = new Cesium.Cartesian3(0, 0, -10);
                }

                if (this.model.get('context')) {
                    this.billboard.scale = 0.5;
                    this.billboard.imageIndex = this.imageIndex + 1;
                } else {
                    this.billboard.scale = 0.41;
                    this.billboard.imageIndex = this.imageIndex;
                }

            },
            onMapLeftClick: function (event) {
                // find out if this click is on us
                if (_.has(event, 'object') && event.object === this.billboard) {
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
                    this.geoController.billboardCollection.remove(this.billboard);

                }
                this.stopListening();
            }

        });

        Views.MultiPointView = Views.PointView.extend({
            initialize: function (options) {
                Views.PointView.prototype.initialize.call(this, options);
            },

            buildBillboard: function () {
                var view = this;

                var points = view.model.get('geometry').getMultiPoint();
                var cartPoints = _.map(points, function (point) {
                    return Cesium.Cartographic.fromDegrees(point.longitude, point.latitude, point.altitude);
                });

                view.points = _.map(cartPoints, function(point) {
                    var billboard = view.geoController.billboardCollection.add({
                        imageIndex: view.imageIndex,
                        position: view.geoController.ellipsoid.cartographicToCartesian(point),
                        horizontalOrigin: Cesium.HorizontalOrigin.CENTER,
                        verticalOrigin: Cesium.VerticalOrigin.BOTTOM,
                        scaleByDistance: new Cesium.NearFarScalar(1.0, 1.0, 1.5e7, 0.5)
                    });
                    billboard.color = view.color;
                    billboard.scale = 0.41;
                    billboard.hasScale = true;
                    return billboard;
                });
            },

            toggleSelection: function () {
                var view = this;

                _.each(view.points, function(point) {
                    if (point.eyeOffset.z < 0) {
                        point.eyeOffset = new Cesium.Cartesian3(0, 0, 0);
                    } else {
                        point.eyeOffset = new Cesium.Cartesian3(0, 0, -10);
                    }
                    if (view.model.get('context')) {
                        point.scale = 0.5;
                        point.imageIndex = view.imageIndex + 1;
                    } else {
                        point.scale = 0.41;
                        point.imageIndex = view.imageIndex;
                    }
                });
            },

            onMapLeftClick: function (event) {
                // find out if this click is on us
                if (_.has(event, 'object') && _.contains(this.points, event.object)) {
                    wreqr.vent.trigger('metacard:selected', dir.none, this.model);
                }
            },
            onMapDoubleClick: function (event) {
                // find out if this click is on us
                if (_.has(event, 'object') && _.contains(this.points, event.object)) {
                    this.geoController.flyToLocation(this.model);
                }
            },

            onClose: function () {
                var view = this;

                if (!_.isUndefined(view.points)) {
                    _.each(view.points, function(point) {
                        view.geoController.billboardCollection.remove(point);
                    });
                }

                this.stopListening();
            }
        });

        Views.LineView = Views.PointView.extend({
            initialize: function (options) {
                options.color = options.color || new Cesium.Color(0.3568627450980392, 0.5764705882352941, 0.8823529411764706, 1);
                Views.PointView.prototype.initialize.call(this, options);

                this.buildLine();

            },

            onMapLeftClick: function (event) {
                // find out if this click is on us
                if (this.isThisPrimitive(event)) {
                    wreqr.vent.trigger('metacard:selected', dir.none, this.model);
                }
            },
            onMapDoubleClick: function (event) {
                // find out if this click is on us
                if (this.isThisPrimitive(event)) {
                    this.geoController.flyToLocation(this.model);

                }
            },

            addLine : function(positions) {
                this.lines.add({
                    positions: positions,
                    width: 2
                });
            },

            buildLine: function () {
                var view = this;
                var points = view.model.get('geometry').getLineString();
                var cartPoints = _.map(points, function (point) {
                    return Cesium.Cartographic.fromDegrees(point.longitude, point.latitude, point.altitude);
                });
                var positions = view.geoController.ellipsoid.cartographicArrayToCartesianArray(cartPoints);

                // Add primitives
                view.lines = new Cesium.PolylineCollection();
                view.addLine(positions);

                view.geoController.scene.primitives.add(view.lines);
            },

            onClose: function () {
                // If there is already a billboard for this view, remove it
                if (!_.isUndefined(this.billboard)) {
                    this.geoController.billboardCollection.remove(this.billboard);
                }
                if (!_.isUndefined(this.lines)) {
                    this.geoController.scene.primitives.remove(this.lines);
                }

                this.stopListening();
            }
        });

        Views.MultiLineView = Views.LineView.extend({
            buildLine: function () {
                var view = this;
                var lineList = view.model.get('geometry').getMultiLineString();
                view.lines = new Cesium.PolylineCollection();
                var pointConverter = function (point) {
                    return Cesium.Cartographic.fromDegrees(point.longitude, point.latitude, point.altitude);
                };
                _.each(lineList, function(points) {
                    var cartPoints = _.map(points, pointConverter);
                    var positions = view.geoController.ellipsoid.cartographicArrayToCartesianArray(cartPoints);

                    view.addLine(positions);
                });
                view.geoController.scene.primitives.add(view.lines);
            },
        });

        Views.RegionView = Views.PointView.extend({
            initialize: function (options) {
                this.color = options.color || {red: 1, green: 0.6431372549019608, blue: 0.403921568627451, alpha: 1 };
                // a light blue
                this.polygonColor = options.polygonColor || new Cesium.Color(0.3568627450980392, 0.5764705882352941, 0.8823529411764706, 0.2);
                this.color = options.color || {red: this.polygonColor.red, green: this.polygonColor.green, blue: this.polygonColor.blue, alpha: 1};
                // a grey matching the outline of the default marker
                this.outlineColor = options.outlineColor || new Cesium.Color(0.707, 0.707, 0.707, 1);
                this.imageIndex = options.imageIndex || 0;

                Views.PointView.prototype.initialize.call(this, options);
                this.buildPolygon();

            },

            toggleSelection : function(){
                // call super for billboard modification
                Views.PointView.prototype.toggleSelection.call(this);
                if (this.model.get('context')) {
                    this.setPolygonSelected();
                } else {
                    this.setPolygonUnselected();
                }

            },
            onMapLeftClick: function (event) {
                // find out if this click is on us
                if (this.isThisPrimitive(event)) {
                    wreqr.vent.trigger('metacard:selected', dir.none, this.model);
                }
            },
            onMapDoubleClick: function (event) {
                // find out if this click is on us
                if (this.isThisPrimitive(event)) {
                    this.geoController.flyToLocation(this.model);
                }
            },

            pointsEqual : function (p1, p2) {
                return p1.x === p2.x && p1.y === p2.y && p1.z === p2.z;
            },

            validatePolygon : function (positions) {
                if(positions.length < 4 ||
                        ! this.pointsEqual(positions[0], positions[positions.length - 1])) {
                    return false;
                }
                for(var i = 1; i < positions.length; i++) {
                    if(this.pointsEqual(positions[i - 1], positions[i])) {
                        return false;
                    }
                }
                return true;
            },

            getOutlineColor: function () {
                return new Cesium.PerInstanceColorAppearance({
                    flat: true,
                    renderState: {
                        depthTest: {
                            enabled: true
                        },
                        lineWidth: Math.min(2.0, this.geoController.scene.context.maximumAliasedLineWidth)
                    }
                });

            },

            getPolygonOutline: function(positions) {
                return new Cesium.GeometryInstance({
                    geometry: Cesium.PolygonOutlineGeometry.fromPositions({positions: positions}),
                    attributes: {
                        color: Cesium.ColorGeometryInstanceAttribute.fromColor(this.outlineColor),
                        show : new Cesium.ShowGeometryInstanceAttribute(true)
                    },
                    id : 'outline'
                });
            },

            getPolygonFill: function(positions) {
                return new Cesium.GeometryInstance({
                    geometry: Cesium.PolygonGeometry.fromPositions({
                        positions: positions,
                        vertexFormat: Cesium.PerInstanceColorAppearance.VERTEX_FORMAT
                    }),
                    attributes: {
                        color: Cesium.ColorGeometryInstanceAttribute.fromColor(this.polygonColor),
                        show : new Cesium.ShowGeometryInstanceAttribute(false)
                    },
                    id : 'polyfill'
                });
            },

            buildPolygon: function () {
                var view = this;
                var points = view.model.get('geometry').getPolygon();
                var cartPoints = _.map(points, function (point) {
                    return Cesium.Cartographic.fromDegrees(point.longitude, point.latitude, point.altitude);
                });
                var positions = view.geoController.ellipsoid.cartographicArrayToCartesianArray(cartPoints);

                if(! this.validatePolygon(positions)) {
                    return false;
                }

                // Add primitives
                view.polygons = [
                    new Cesium.Primitive({
                        geometryInstances: [view.getPolygonOutline(positions)],
                        appearance: view.getOutlineColor()
                    }),
                    new Cesium.Primitive({
                        geometryInstances: [view.getPolygonFill(positions)],
                        appearance: new Cesium.PerInstanceColorAppearance({
                            closed: true
                        })
                    })
                ];

                _.each(view.polygons, function (polygonPrimitive) {
                    view.geoController.scene.primitives.add(polygonPrimitive);
                });
            },

            setPolygonSelected : function(){
                var view = this;
                var attributes = view.polygons[0].getGeometryInstanceAttributes('outline');
                attributes.color = Cesium.ColorGeometryInstanceAttribute.toValue(Cesium.Color.BLACK);

                var fillAttributes = view.polygons[1].getGeometryInstanceAttributes('polyfill');
                fillAttributes.show = Cesium.ShowGeometryInstanceAttribute.toValue(true, fillAttributes.show);
            },

            setPolygonUnselected : function(){
                var view = this;
                var attributes = view.polygons[0].getGeometryInstanceAttributes('outline');
                attributes.color = Cesium.ColorGeometryInstanceAttribute.toValue(view.outlineColor);

                var fillAttributes = view.polygons[1].getGeometryInstanceAttributes('polyfill');
                fillAttributes.show = Cesium.ShowGeometryInstanceAttribute.toValue(false, fillAttributes.show);
            },

            onClose: function () {
                var view = this;

                // If there is already a billboard for this view, remove it
                if (!_.isUndefined(view.billboard)) {
                    view.geoController.billboardCollection.remove(view.billboard);
                }
                if (!_.isUndefined(view.polygons)) {
                    _.each(view.polygons, function (polygonPrimitive) {
                        view.geoController.scene.primitives.remove(polygonPrimitive);
                    });
                }

                this.stopListening();
            }
        });

        Views.MultiRegionView = Views.RegionView.extend({
            buildPolygon: function () {
                var view = this;
                var polygonList = view.model.get('geometry').getMultiPolygon();
                view.polygons = [];
                var pointConverter = function (point) {
                    return Cesium.Cartographic.fromDegrees(point.longitude, point.latitude, point.altitude);
                };
                for(var i = 0; i < polygonList.length; i++) {
                    var points = polygonList[i];
                    var cartPoints = _.map(points, pointConverter);
                    var positions = view.geoController.ellipsoid.cartographicArrayToCartesianArray(cartPoints);

                    if(! this.validatePolygon(positions)) {
                        return false;
                    }

                    // Add primitives
                    view.polygons[2*i] = new Cesium.Primitive({
                            geometryInstances: [view.getPolygonOutline(positions)],
                            appearance: view.getOutlineColor()
                        });
                    view.polygons[2*i + 1] = new Cesium.Primitive({
                            geometryInstances: [view.getPolygonFill(positions)],
                            appearance: new Cesium.PerInstanceColorAppearance({
                                closed: true
                            })
                        });
                }
                _.each(view.polygons, function (polygonPrimitive) {
                    view.geoController.scene.primitives.add(polygonPrimitive);
                });
            },

            setPolygonSelected : function(){
                var view = this;
                var attributes, fillAttributes;

                for(var i = 0; i < view.polygons.length; i += 2) {
                    attributes = view.polygons[i].getGeometryInstanceAttributes('outline');
                    attributes.color = Cesium.ColorGeometryInstanceAttribute.toValue(Cesium.Color.BLACK);

                    fillAttributes = view.polygons[i + 1].getGeometryInstanceAttributes('polyfill');
                    fillAttributes.show = Cesium.ShowGeometryInstanceAttribute.toValue(true, fillAttributes.show);
                }
            },

            setPolygonUnselected : function(){
                var view = this;
                var attributes, fillAttributes;

                for(var i = 0; i < view.polygons.length; i += 2) {
                    attributes = view.polygons[i].getGeometryInstanceAttributes('outline');
                    attributes.color = Cesium.ColorGeometryInstanceAttribute.toValue(view.outlineColor);

                    fillAttributes = view.polygons[i + 1].getGeometryInstanceAttributes('polyfill');
                    fillAttributes.show = Cesium.ShowGeometryInstanceAttribute.toValue(false, fillAttributes.show);
                }
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
