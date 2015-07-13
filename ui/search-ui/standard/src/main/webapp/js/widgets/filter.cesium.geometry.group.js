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
define([
    'marionette',
    'backbone',
    'cesium',
    'underscore',
    'wreqr'
],
function (Marionette, Backbone, Cesium, _, wreqr) {
    "use strict";

    var FilterGeometryGroup = {};

    var FilterGeometryItem = Backbone.View.extend({
        initialize: function(options){
            this.options = options;
            this.listenTo(this.model, 'change', this.updatePrimitive);
        },
        // this will manipulate the map.
        render: function(){
            this.updatePrimitive();
            return this;
        },
        remove: function(){
            this.cleanUpPrimitive();
            Backbone.View.prototype.remove.apply(this, arguments);
        },
        cleanUpPrimitive: function() {
            if(this.primitive){
                this.options.geoController.scene.primitives.remove(this.primitive);
                this.primitive = null;
            }
        },
        updatePrimitive: function(){
            this.cleanUpPrimitive();
            var geoType = this.model.get('geoType');
            if(geoType === 'polygon'){
                this.primitive = this.createPolygonPrimitive();
            } else if(geoType === 'circle') {
                this.primitive = this.createCirclePrimitive();
            } else if(geoType === 'bbox'){
                this.primitive = this.createBboxPrimitive();
            }

            if(this.primitive) {
                this.options.geoController.scene.primitives.add(this.primitive);
            }
        },

        createBboxPrimitive: function(){
            var rectangle = this.modelToRectangle();
            if (!rectangle) {
                // handles case where model changes to empty vars and we don't want to draw anymore
                return;
            }

            return new Cesium.Primitive({
                asynchronous: false,
                geometryInstances: [new Cesium.GeometryInstance({
                    geometry: new Cesium.RectangleOutlineGeometry({
                        rectangle: rectangle
                    }),
                    attributes: {
                        color: Cesium.ColorGeometryInstanceAttribute.fromColor(Cesium.Color.KHAKI)
                    }
                })],
                appearance: new Cesium.PerInstanceColorAppearance({
                    flat: true,
                    renderState: {
                        depthTest: {
                            enabled: true
                        },
                        lineWidth: Math.min(4.0, this.options.geoController.scene.context.maximumAliasedLineWidth)
                    }
                })
            });
        },

        modelToRectangle: function () {
            var toRad = Cesium.Math.toRadians;
            var obj = this.model.toJSON();
            _.each(obj, function (val, key) {
                obj[key] = toRad(val);
            });
            var rectangle = new Cesium.Rectangle();
            if (!obj.north || isNaN(obj.north) || !obj.south || isNaN(obj.south) || !obj.east || isNaN(obj.east) || !obj.west || isNaN(obj.west)) {
                return null;
            }

            rectangle.north = obj.north;
            rectangle.south = obj.south;
            rectangle.east = obj.east;
            rectangle.west = obj.west;
            return rectangle;
        },

        createCirclePrimitive: function(){
            // if model has been reset
            var modelProp = this.model.toJSON();
            if (modelProp.lon === undefined || modelProp.lat === undefined || modelProp.radius === undefined) {
                return;
            }

            return new Cesium.Primitive({
                asynchronous: false,
                geometryInstances: [new Cesium.GeometryInstance({
                    geometry: new Cesium.CircleOutlineGeometry({
                        center: this.options.geoController.ellipsoid.cartographicToCartesian(Cesium.Cartographic.fromDegrees(modelProp.lon, modelProp.lat)),
                        radius: modelProp.radius
                    }),
                    attributes: {
                        color: Cesium.ColorGeometryInstanceAttribute.fromColor(Cesium.Color.KHAKI)
                    }
                })],
                appearance: new Cesium.PerInstanceColorAppearance({
                    flat: true,
                    renderState: {
                        depthTest: {
                            enabled: true
                        },
                        lineWidth: Math.min(4.0, this.options.geoController.scene.context.maximumAliasedLineWidth)
                    }
                })
            });
        },

        createPolygonPrimitive: function(){
            var polygonPoints = this.model.toJSON().polygon;
            if(!polygonPoints){
                return null;
            }

            return new Cesium.Primitive({
                asynchronous: false,
                geometryInstances: [
                    new Cesium.GeometryInstance({
                        geometry: new Cesium.PolygonOutlineGeometry({
                            polygonHierarchy: {
                                positions: Cesium.Cartesian3.fromDegreesArray(_.flatten(polygonPoints))
                            }
                        }),
                        attributes: {
                            color: Cesium.ColorGeometryInstanceAttribute.fromColor(Cesium.Color.KHAKI)
                        }
                    })
                ],
                appearance: new Cesium.PerInstanceColorAppearance({
                    flat: true,
                    renderState: {
                        depthTest: {
                            enabled: true
                        },
                        lineWidth: Math.min(4.0, this.options.geoController.scene.context.maximumAliasedLineWidth)
                    }
                })
            });
        }

    });

    var FilterGeometryCollection = Marionette.CollectionView.extend({
        // this will control the collection via crud events through the model.
        childView: FilterGeometryItem,
        childViewOptions: function(){
            return {
                geoController: this.options.geoController
            };
        }
    });

    FilterGeometryGroup.Controller = Marionette.Controller.extend({
        initialize: function(options){
            this.options = options;
            this.listenTo(wreqr.vent, 'mapfilter:showFilters', this.showFilters);
            this.listenTo(wreqr.vent, 'map:clear', this.clear);
        },
        showFilters: function(filters){
            this.clear();

            this.view = new FilterGeometryCollection({
                geoController: this.options.geoController,
                collection: filters
            });
            this.view.render();
        },
        clear: function () {
            if (this.view){
                this.view.destroy();
            }
        }
    });

    return FilterGeometryGroup;
});