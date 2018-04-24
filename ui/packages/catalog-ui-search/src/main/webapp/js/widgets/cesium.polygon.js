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
        'wreqr',
        'maptype',
        './notification.view',
        'js/ShapeUtils',
        './drawing.controller'
    ],
    function(Marionette, Backbone, Cesium, _, wreqr, maptype, NotificationView, ShapeUtils, DrawingController) {
        "use strict";
        var Draw = {};

        Draw.PolygonRenderView = Marionette.View.extend({
            initialize: function() {
                this.listenTo(this.model, 'change:polygon', this.updatePrimitive);
                this.updatePrimitive(this.model);
            },
            modelEvents: {
                'changed': 'updatePrimitive'
            },
            updatePrimitive: function() {
                this.drawPolygon(this.model);
            },
            drawPolygon: function(model) {
                var json = model.toJSON();
                var isMultiPolygon =  ShapeUtils.isArray3D(json.polygon);
                var polygons = isMultiPolygon ? json.polygon : [ json.polygon ];

                var color = this.model.get('color');

                // first destroy old one
                if (this.primitive && !this.primitive.isDestroyed()) {
                    this.options.map.scene.primitives.remove(this.primitive);
                }

                this.primitive = new Cesium.PolylineCollection();

                (polygons || []).forEach(function(polygonPoints){
                    if (!polygonPoints || polygonPoints.length < 3) {
                        return;
                    }
                    if (polygonPoints[0].toString() !== polygonPoints[polygonPoints.length - 1].toString()) {
                        polygonPoints.push(polygonPoints[0]);
                    }
                    this.primitive.add({
                        width: 8,
                        material: Cesium.Material.fromType('PolylineOutline', {
                            color: color ? Cesium.Color.fromCssColorString(color) : Cesium.Color.KHAKI,
                            outlineColor: Cesium.Color.WHITE,
                            outlineWidth: 4
                        }),
                        id: 'userDrawing',
                        positions: Cesium.Cartesian3.fromDegreesArray(_.flatten(polygonPoints))
                    });
                }.bind(this));

                this.options.map.scene.primitives.add(this.primitive);
            },
            destroy: function() {
                if (this.primitive) {
                    this.options.map.scene.primitives.remove(this.primitive);
                }
                this.remove(); // backbone cleanup.
            }
        });

        Draw.Controller = DrawingController.extend({
            drawingType: 'poly',
            show: function(model) {
                if (this.enabled) {
                    this.options.drawHelper.stopDrawing();
                    // remove old polygon
                    var existingView = this.getViewForModel(model);
                    if (existingView) {
                        existingView.destroy();
                        this.removeViewForModel(model);
                    }
                    var view = new Draw.PolygonRenderView({ model: model, map: this.options.map });
                    this.addView(view);
                }
            },
            draw: function(model) {
                var controller = this;
                var toDeg = Cesium.Math.toDegrees;
                if (this.enabled) {
                    // start polygon draw.
                    this.notificationView = new NotificationView({
                        el: this.options.notificationEl
                    }).render();
                    this.options.drawHelper.startDrawingPolygon({
                        callback: function(positions) {

                            if (controller.notificationView) {
                                controller.notificationView.destroy();
                            }
                            var latLonRadPoints = _.map(positions, function(cartPos) {
                                var latLon = controller.options.map.scene.globe.ellipsoid.cartesianToCartographic(cartPos);
                                return [toDeg(latLon.longitude), toDeg(latLon.latitude)];
                            });

                            //this shouldn't ever get hit because the draw library should protect against it, but just in case it does, remove the point
                            if (latLonRadPoints.length > 3 && latLonRadPoints[latLonRadPoints.length - 1][0] === latLonRadPoints[latLonRadPoints.length - 2][0] &&
                                latLonRadPoints[latLonRadPoints.length - 1][1] === latLonRadPoints[latLonRadPoints.length - 2][1]) {
                                latLonRadPoints.pop();
                            }

                            model.set('polygon', latLonRadPoints);

                            // doing this out of formality since bbox/circle call this after drawing has ended.
                            model.trigger('EndExtent', model);

                            // lets go ahead and show our new shiny polygon.
                            wreqr.vent.trigger('search:polydisplay', model);
                        }
                    });
                }
            }
        });

        return Draw;
    });
