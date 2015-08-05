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
/*global define,Math*/

define([
        'backbone',
        'marionette',
        'underscore',
        'cesium',
        'direction',
        'wreqr',
        'application'
    ],
    function (Backbone, Marionette, _, Cesium, dir, wreqr, Application) {
        "use strict";
        var Views = {},
            pointScale = 0.02,
            selectedPointScale = 0.035;

        Views.PointView = Marionette.ItemView.extend({
            modelEvents: {
                'change:context': 'toggleSelection'
            },
            billboards: [
                'images/default.png',
                'images/default-selected.png'
                // add extra here if you want to switch
            ],
            initialize: function (options) {
                this.geoController = options.geoController;
                if(! options.ignoreEvents) {
                    this.listenTo(this.geoController, 'click:left', this.onMapLeftClick);
                    this.listenTo(this.geoController, 'doubleclick:left', this.onMapDoubleClick);
                }
                this.color = options.color || Cesium.Color.fromCssColorString(Application.UserModel.get('user>preferences>pointColor'));
                this.buildBillboard();
            },

            isThisPrimitive : function(event){
                // could wrap this in one huge if statement, but this seems more readable
                if(event.object){
                    if(event.object === this.billboard){
                        return true;
                    }
                    if(_.contains(this.lines, event.object)){
                        return true;
                    }
                    if(_.contains(_.map(this.geometries, function (view) {
                            return view.billboard;
                        }), event.object)){
                        return true;
                    }
                }
                return false;
            },

            buildBillboard: function () {
                var view = this;
                var point = this.model.get('geometry').getPoint();
                var cartographic = Cesium.Cartographic.fromDegrees(
                    point.longitude,
                    point.latitude,
                    point.altitude
                );
                this.billboard = this.geoController.billboardCollection.add({
                    image: this.billboards[0],
                    position: this.geoController.ellipsoid.cartographicToCartesian(cartographic),
                    color: this.color,
                    scale: pointScale
                });
                //if there is a terrain provider and no altitude has been specified, sample it from the configured terrain provider
                if (this.geoController.scene.terrainProvider && !point.altitude) {
                    var promise = Cesium.sampleTerrain(this.geoController.scene.terrainProvider, 5, [cartographic]);
                    Cesium.when(promise, function(updatedCartographic) {
                        if (updatedCartographic[0].height && !view.isDestroyed) {
                            view.billboard.position = view.geoController.ellipsoid.cartographicToCartesian(updatedCartographic[0]);
                        }
                    });
                }
            },

            toggleSelection: function () {
                if (this.billboard) {
                    if (this.billboard.eyeOffset.z < 0) {
                        this.billboard.eyeOffset = new Cesium.Cartesian3(0, 0, 0);
                    } else {
                        this.billboard.eyeOffset = new Cesium.Cartesian3(0, 0, -10);
                    }

                    if (this.model.get('context')) {
                        this.billboard.scale = selectedPointScale;
                        this.billboard.image = this.billboards[1];
                    } else {
                        this.billboard.scale = pointScale;
                        this.billboard.image = this.billboards[0];
                    }
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

            onDestroy: function () {
                // If there is already a billboard for this view, remove it
                if (!_.isUndefined(this.billboard)) {
                    this.geoController.billboardCollection.remove(this.billboard);

                }
                this.stopListening();
            }

        });

        Views.MultiPointView = Views.PointView.extend({
            initialize: function (options) {
                options.color = options.color || Cesium.Color.fromCssColorString(Application.UserModel.get('user>preferences>multiPointColor'));
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
                        image: view.billboards[0],
                        position: view.geoController.ellipsoid.cartographicToCartesian(point),
                        color: view.color,
                        scale: pointScale
                    });
                    //if there is a terrain provider and no altitude has been specified, sample it from the configured terrain provider
                    if (view.geoController.scene.terrainProvider && !point.height) {
                        var promise = Cesium.sampleTerrain(view.geoController.scene.terrainProvider, 5, [point]);
                        Cesium.when(promise, function(updatedCartographic) {
                            if (updatedCartographic[0].height && !view.isDestroyed) {
                                billboard.position = view.geoController.ellipsoid.cartographicToCartesian(updatedCartographic[0]);
                            }
                        });
                    }
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
                        point.scale = selectedPointScale;
                        point.image = view.billboards[1];
                    } else {
                        point.scale = pointScale;
                        point.image = view.billboards[0];
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

            onDestroy: function () {
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
                options.color = options.color || Cesium.Color.fromCssColorString(Application.UserModel.get('user>preferences>lineColor'));
                this.color = options.color;
                this.lineMap = {};
                Views.PointView.prototype.initialize.call(this, options);
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

            removeLine: function(line) {
                this.lines.remove(line);
            },

            addLine : function(positions, key) {
                this.lineMap[key] = this.lines.add({
                    positions: positions,
                    width: 2
                });
            },

            buildBillboard: function () {
                this.lines = new Cesium.PolylineCollection();
                var points = this.model.get('geometry').getLineString();
                var lineObj = this.buildLine(points);
                this.addLine(lineObj.line, lineObj.key);
                this.geoController.scene.primitives.add(this.lines);
                Views.PointView.prototype.buildBillboard.call(this);
            },

            buildLine: function (points) {
                var view = this;
                var key = Math.random() * 16;

                var cartPoints = _.map(points, function (point) {
                    return Cesium.Cartographic.fromDegrees(point.longitude, point.latitude, point.altitude);
                });
                //if there is a terrain provider and no altitude has been specified, sample it from the configured terrain provider
                if (view.geoController.scene.terrainProvider) {
                    var promise = Cesium.sampleTerrain(this.geoController.scene.terrainProvider, 5, cartPoints);
                    Cesium.when(promise, function(updatedCartographic) {
                        var positions = view.geoController.ellipsoid.cartographicArrayToCartesianArray(updatedCartographic);
                        if (updatedCartographic[0].height && !view.isDestroyed) {
                            view.removeLine(view.lineMap[key]);
                            view.addLine(positions, key);
                        }
                    });
                }
                var cartesian = view.geoController.ellipsoid.cartographicArrayToCartesianArray(cartPoints);

                return {
                    line: cartesian,
                    key: key
                };
            },

            onDestroy: function () {
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
            initialize: function (options) {
                options.color = options.color || Cesium.Color.fromCssColorString(Application.UserModel.get('user>preferences>multiLineColor'));
                this.lineMap = {};
                Views.PointView.prototype.initialize.call(this, options);
            },
            buildBillboard: function () {
                var view = this;
                var lineList = view.model.get('geometry').getMultiLineString();
                view.lines = new Cesium.PolylineCollection();
                _.each(lineList, function(points) {
                    var lineObj = view.buildLine(points);
                    view.addLine(lineObj.line, lineObj.key);
                });
                view.geoController.scene.primitives.add(view.lines);
                Views.PointView.prototype.buildBillboard.call(this);
            }
        });

        Views.RegionView = Views.PointView.extend({
            initialize: function (options) {
                this.color = options.color || Cesium.Color.fromCssColorString(Application.UserModel.get('user>preferences>polygonColor'));
                options.color = this.color;

                this.polygonColor = options.polygonColor || Cesium.Color.fromCssColorString(Application.UserModel.get('user>preferences>polygonColor'));

                this.polygonColor = Cesium.Color.fromRgba(this.polygonColor.toRgba());
                if (this.polygonColor.withAlpha) {
                    this.polygonColor = this.polygonColor.withAlpha(0.2);
                } else {
                    this.polygonColor.alpha = 0.2;
                }

                this.outlineColor = options.outlineColor || new Cesium.Color(255/255, 255/255, 255/255, 1);

                this.polygonMap = {};
                this.polygons = [];
                Views.PointView.prototype.initialize.call(this, options);
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

            removePolygon: function(positions) {
                var idx = -1;
                _.each(this.polygons, function(polygon, index) {
                    if (_.difference(polygon.positions, positions).length === 0) {
                        idx = index;
                    }
                });

                if (idx >= 0) {
                    this.geoController.scene.primitives.remove(this.polygons[idx].outline);
                    this.geoController.scene.primitives.remove(this.polygons[idx].fill);
                    this.polygons.splice(idx, 1);
                }
            },

            addPolygon: function(positions) {
                if(! this.validatePolygon(positions)) {
                    return false;
                }
                var polygon = {
                    positions: positions,
                    outline: new Cesium.Primitive({
                        geometryInstances: [this.getPolygonOutline(positions)],
                        appearance: this.getOutlineColor()
                    }),
                    fill: new Cesium.Primitive({
                        geometryInstances: [this.getPolygonFill(positions)],
                        appearance: new Cesium.PerInstanceColorAppearance({
                            closed: true
                        })
                    })
                };
                // Add primitives
                this.polygons.push(polygon);
                if (!this.isDestroyed) {
                    this.geoController.scene.primitives.add(polygon.outline);
                    this.geoController.scene.primitives.add(polygon.fill);
                }
            },

            buildBillboard: function () {
                this.lines = new Cesium.PolylineCollection();
                var points = this.model.get('geometry').getPolygon();
                var polygon = this.buildPolygon(points);
                this.addPolygon(polygon);
                Views.PointView.prototype.buildBillboard.call(this);
            },

            buildPolygon: function (points) {
                var view = this;
                var key = Math.random() * 16;
                var cartPoints = _.map(points, function (point) {
                    return Cesium.Cartographic.fromDegrees(point.longitude, point.latitude, point.altitude);
                });
                //if there is a terrain provider and no altitude has been specified, sample it from the configured terrain provider
                if (view.geoController.scene.terrainProvider) {
                    var promise = Cesium.sampleTerrain(this.geoController.scene.terrainProvider, 5, cartPoints);
                    Cesium.when(promise, function(updatedCartographic) {
                        var positions = view.geoController.ellipsoid.cartographicArrayToCartesianArray(updatedCartographic);
                        if (updatedCartographic[0].height && !view.isDestroyed) {
                            view.removePolygon(view.polygonMap[key]);
                            view.addPolygon(positions);
                            view.polygonMap[key] = positions;
                        }
                    });
                }
                var positions = view.geoController.ellipsoid.cartographicArrayToCartesianArray(cartPoints);

                this.polygonMap[key] = positions;

                return positions;
            },

            setPolygonSelected : function(){
                var view = this;
                var attributes = view.polygons[0].outline.getGeometryInstanceAttributes('outline');
                attributes.color = Cesium.ColorGeometryInstanceAttribute.toValue(Cesium.Color.BLACK);

                var fillAttributes = view.polygons[0].fill.getGeometryInstanceAttributes('polyfill');
                fillAttributes.show = Cesium.ShowGeometryInstanceAttribute.toValue(true, fillAttributes.show);
            },

            setPolygonUnselected : function(){
                var view = this;
                var attributes = view.polygons[0].outline.getGeometryInstanceAttributes('outline');
                attributes.color = Cesium.ColorGeometryInstanceAttribute.toValue(view.outlineColor);

                var fillAttributes = view.polygons[0].fill.getGeometryInstanceAttributes('polyfill');
                fillAttributes.show = Cesium.ShowGeometryInstanceAttribute.toValue(false, fillAttributes.show);
            },

            onDestroy: function () {
                var view = this;

                // If there is already a billboard for this view, remove it
                if (!_.isUndefined(view.billboard)) {
                    view.geoController.billboardCollection.remove(view.billboard);
                }
                if (!_.isUndefined(view.polygons)) {
                    _.each(view.polygons, function (polygonPrimitive) {
                        view.geoController.scene.primitives.remove(polygonPrimitive.outline);
                        view.geoController.scene.primitives.remove(polygonPrimitive.fill);
                    });
                }

                this.stopListening();
            }
        });

        Views.MultiRegionView = Views.RegionView.extend({
            initialize: function (options) {
                this.color = options.color || Cesium.Color.fromCssColorString(Application.UserModel.get('user>preferences>multiPolygonColor'));
                options.color = this.color;

                this.polygonColor = options.polygonColor || Cesium.Color.fromCssColorString(Application.UserModel.get('user>preferences>multiPolygonColor'));
                this.polygonColor = Cesium.Color.fromRgba(this.polygonColor.toRgba());
                if (this.polygonColor.withAlpha) {
                    this.polygonColor = this.polygonColor.withAlpha(0.2);
                } else {
                    this.polygonColor.alpha = 0.2;
                }

                this.outlineColor = options.outlineColor || new Cesium.Color(255/255, 255/255, 255/255, 1);

                this.polygonMap = {};
                this.polygons = [];
                Views.PointView.prototype.initialize.call(this, options);
            },

            buildBillboard: function () {
                var view = this;
                this.lines = new Cesium.PolylineCollection();
                var polyList = this.model.get('geometry').getMultiPolygon();
                _.each(polyList, function(points){
                    var polygon = view.buildPolygon(points);
                    view.addPolygon(polygon);
                });
                Views.PointView.prototype.buildBillboard.call(this);
            },

            setPolygonSelected : function(){
                var view = this;
                var attributes, fillAttributes;

                for(var i = 0; i < view.polygons.length; i++) {
                    attributes = view.polygons[i].outline.getGeometryInstanceAttributes('outline');
                    attributes.color = Cesium.ColorGeometryInstanceAttribute.toValue(Cesium.Color.BLACK);

                    fillAttributes = view.polygons[i].fill.getGeometryInstanceAttributes('polyfill');
                    fillAttributes.show = Cesium.ShowGeometryInstanceAttribute.toValue(true, fillAttributes.show);
                }
            },

            setPolygonUnselected : function(){
                var view = this;
                var attributes, fillAttributes;

                for(var i = 0; i < view.polygons.length; i++) {
                    attributes = view.polygons[i].outline.getGeometryInstanceAttributes('outline');
                    attributes.color = Cesium.ColorGeometryInstanceAttribute.toValue(view.outlineColor);

                    fillAttributes = view.polygons[i].fill.getGeometryInstanceAttributes('polyfill');
                    fillAttributes.show = Cesium.ShowGeometryInstanceAttribute.toValue(false, fillAttributes.show);
                }
            }
        });

        Views.GeometryCollectionView = Views.PointView.extend({
            initialize: function (options) {
                options.color = options.color || Cesium.Color.fromCssColorString(Application.UserModel.get('user>preferences>geometryCollectionColor'));
                options.polygonColor = options.polygonColor || Cesium.Color.fromCssColorString(Application.UserModel.get('user>preferences>geometryCollectionColor'));
                options.polygonColor = Cesium.Color.fromRgba(options.polygonColor.toRgba());
                if (options.polygonColor.withAlpha) {
                    options.polygonColor = options.polygonColor.withAlpha(0.2);
                } else {
                    options.polygonColor.alpha = 0.2;
                }

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

            onMapLeftClick: function (event) {
                var view = this;

                if(this.isThisPrimitive(event)) {
                    if (this.model) {
                        wreqr.vent.trigger('metacard:selected', dir.none, this.model);
                    }
                    _.each(view.geometries, function (geometry) {
                        geometry.onMapLeftClick(event);
                        geometry.toggleSelection();
                    });
                }
            },

            onMapDoubleClick: function (event) {
                var view = this;
                if(this.isThisPrimitive(event)) {
                    if (this.model) {
                        wreqr.vent.trigger('metacard:selected', dir.none, this.model);
                    }
                    _.each(view.geometries, function (geometry) {
                        geometry.onMapDoubleClick(event);
                        geometry.toggleSelection();
                    });
                }
            },

            onDestroy: function () {
                var view = this;

                _.each(view.geometries, function(geometry) {
                    geometry.destroy();
                });

                this.stopListening();
            }
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
