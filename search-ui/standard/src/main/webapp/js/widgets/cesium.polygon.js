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
        './notification.view'
    ],
    function (Marionette, Backbone, Cesium, _, wreqr, maptype, NotificationView) {
        "use strict";
        var Draw = {};

        Draw.PolygonRenderView = Backbone.View.extend({
            initialize: function(options){
                this.scene = options.scene;
                this.updatePrimitive(this.model);
            },
            modelEvents: {
                'changed': 'updatePrimitive'
            },
            updatePrimitive: function(){
                this.drawPolygon(this.model);
            },
            drawPolygon: function (model) {
                var polygonPoints = model.toJSON().polygon;
                if(!polygonPoints){
                    return;
                }

                // first destroy old one
                if (this.primitive && !this.primitive.isDestroyed()) {
                    this.scene.primitives.remove(this.primitive);
                }

                this.primitive = new Cesium.Primitive({
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
                            lineWidth: Math.min(4.0, this.scene.context.maximumAliasedLineWidth)
                        }
                    })
                });

                this.scene.primitives.add(this.primitive);
            },
            close: function(){
                if(this.primitive){
                    this.scene.primitives.remove(this.primitive);
                }
                this.remove();  // backbone cleanup.
            }
        });

        Draw.Controller = Marionette.Controller.extend({
            enabled: maptype.is3d(),
            initialize: function (options) {
                this.scene = options.scene;
                this.notificationEl = options.notificationEl;
                this.drawHelper = options.drawHelper;
                this.geoController = options.geoController;

                this.listenTo(wreqr.vent, 'search:polydisplay', this.showPolygon);
                this.listenTo(wreqr.vent, 'search:drawpoly', this.draw);
                this.listenTo(wreqr.vent, 'search:drawstop', this.stop);
                this.listenTo(wreqr.vent, 'search:drawend', this.destroy);

            },
            showPolygon: function(model) {
                if (this.enabled) {
                    this.drawHelper.stopDrawing();
                    // remove old polygon
                    if(this.view){
                        this.view.close();
                    }
                    this.view = new Draw.PolygonRenderView({model: model, scene: this.scene});
                }
            },
            draw: function (model) {
                var controller = this;
                var toDeg = Cesium.Math.toDegrees;
                if (this.enabled) {
                    // start polygon draw.
                    this.notificationView = new NotificationView({
                        el: this.notificationEl
                    }).render();
                    this.drawHelper.startDrawingPolygon({
                        callback: function(positions) {

                            if(controller.notificationView) {
                                controller.notificationView.close();
                            }
                            var latLonRadPoints =_.map(positions, function(cartPos){
                                var latLon = controller.geoController.ellipsoid.cartesianToCartographic(cartPos);
                                return [ toDeg(latLon.longitude),toDeg(latLon.latitude)];
                            });

                            // get rid of the points drawhelper added when the user double clicks.
                            // this addresses the known issue of https://github.com/leforthomas/cesium-drawhelper/issues/7
                            latLonRadPoints.pop();
                            latLonRadPoints.pop();

                            model.set('polygon', latLonRadPoints);

                            // doing this out of formality since bbox/circle call this after drawing has ended.
                            model.trigger('EndExtent', model);

                            // lets go ahead and show our new shiny polygon.
                            wreqr.vent.trigger('search:polydisplay', model);
                        }
                    });
                }
            },
            stop: function () {
                if (this.enabled) {
                    // stop drawing
                    this.drawHelper.stopDrawing();
                    if(this.notificationView) {
                        this.notificationView.close();
                    }
                }
            },
            destroy: function () {
                if (this.enabled) {
                    // I don't think we need this method.
                    if(this.notificationView) {
                        this.notificationView.close();
                    }
                    if(this.view){
                        this.view.close();
                    }
                }
            }
        });

        return Draw;
    });