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
        'marionette',
        'backbone',
        'cesium',
        'underscore',
        'wreqr',
        'js/widgets/cesium.bbox',
        'maptype',
        './notification.view'
    ],
    function (Marionette, Backbone, Cesium, _, wreqr, DrawBbox, maptype, NotificationView) {
        "use strict";
        var DrawCircle = {};

        DrawCircle.CircleModel = Backbone.Model.extend({
            defaults: {
                lat: undefined,
                lon: undefined,
                radius: undefined
            }
        });
        var defaultAttrs = ['lat', 'lon', 'radius'];
        DrawCircle.CircleView = Backbone.View.extend({
            initialize: function (options) {
                this.canvas = options.scene.canvas;
                this.scene = options.scene;
                this.ellipsoid = options.scene.globe.ellipsoid;
                this.mouseHandler = new Cesium.ScreenSpaceEventHandler(this.canvas);

                this.listenTo(this.model, 'change:lat change:lon change:radius', this.updatePrimitive);
            },
            enableInput: function () {
                var controller = this.scene.screenSpaceCameraController;
                controller.enableTranslate = true;
                controller.enableZoom = true;
                controller.enableRotate = true;
                controller.enableTilt = true;
                controller.enableLook = true;
            },
            disableInput: function () {
                var controller = this.scene.screenSpaceCameraController;
                controller.enableTranslate = false;
                controller.enableZoom = false;
                controller.enableRotate = false;
                controller.enableTilt = false;
                controller.enableLook = false;
            },

            setCircleRadius: function (mn, mx) {
                var startCartographic = this.ellipsoid.cartographicToCartesian(mn),
                    stopCart = this.ellipsoid.cartographicToCartesian(mx),
                    radius = Math.abs(Cesium.Cartesian3.distance(startCartographic, stopCart));

                var modelProp = {
                    lat: (mn.latitude * 180 / Math.PI).toFixed(4),
                    lon: (mn.longitude * 180 / Math.PI).toFixed(4),
                    radius: radius

                };

                this.model.set(modelProp);

            },

            isModelReset: function (modelProp) {
                if (_.every(defaultAttrs, function (val) {
                    return _.isUndefined(modelProp[val]);
                }) || _.isEmpty(modelProp)) {
                    return true;
                }
                return false;
            },

            updatePrimitive: function (model) {

                var modelProp = model.toJSON();
                if (this.isModelReset(modelProp)) {
                    this.scene.primitives.remove(this.primitive);
                    this.stopListening();
                    return;
                }

                if (modelProp.radius === 0 || isNaN(modelProp.radius)) {
                    modelProp.radius = 1;
                }

                this.drawBorderedCircle(model);
            },
            drawBorderedCircle: function (model) {
                // if model has been reset
                var modelProp = model.toJSON();
                if (this.isModelReset(modelProp)) {
                    return;
                }

                // first destroy old one
                if (this.primitive && !this.primitive.isDestroyed()) {
                    this.scene.primitives.remove(this.primitive);
                }

                this.primitive = new Cesium.Primitive({
                    asynchronous: false,
                    geometryInstances: [new Cesium.GeometryInstance({
                        geometry: new Cesium.CircleOutlineGeometry({
                            center: this.ellipsoid.cartographicToCartesian(Cesium.Cartographic.fromDegrees(modelProp.lon, modelProp.lat)),
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
                            lineWidth: Math.min(4.0, this.scene.context.maximumAliasedLineWidth)
                        }
                    })
                });

                this.scene.primitives.add(this.primitive);
            },
            handleRegionStop: function () {
                this.enableInput();
                if (!this.mouseHandler.isDestroyed()) {
                    this.mouseHandler.destroy();
                }
                this.drawBorderedCircle(this.model);
                this.stopListening(this.model, 'change:lat change:lon change:radius', this.updatePrimitive);
                this.listenTo(this.model, 'change:lat change:lon change:radius', this.drawBorderedCircle);
                this.model.trigger("EndExtent", this.model);
            },
            handleRegionInter: function (movement) {
                var cartesian = this.scene.camera.pickEllipsoid(movement.endPosition, this.ellipsoid),
                    cartographic;
                if (cartesian) {
                    cartographic = this.ellipsoid.cartesianToCartographic(cartesian);
                    this.setCircleRadius(this.click1, cartographic);
                }
            },
            handleRegionStart: function (movement) {
                var cartesian = this.scene.camera.pickEllipsoid(movement.position, this.ellipsoid),
                    that = this;
                if (cartesian) {
                    this.click1 = this.ellipsoid.cartesianToCartographic(cartesian);
                    this.mouseHandler.setInputAction(function () {
                        that.handleRegionStop();
                    }, Cesium.ScreenSpaceEventType.LEFT_UP);
                    this.mouseHandler.setInputAction(function (movement) {
                        that.handleRegionInter(movement);
                    }, Cesium.ScreenSpaceEventType.MOUSE_MOVE);
                }
            },
            start: function () {
                this.disableInput();

                var that = this;

                // Now wait for start
                this.mouseHandler.setInputAction(function (movement) {
                    that.handleRegionStart(movement);
                }, Cesium.ScreenSpaceEventType.LEFT_DOWN);
            },
            stop: function () {
                this.stopListening();
                this.enableInput();
            },

            destroyPrimitive: function () {
                if (!this.mouseHandler.isDestroyed()) {
                    this.mouseHandler.destroy();
                }
                if (this.primitive && !this.primitive.isDestroyed()) {
                    this.scene.primitives.remove(this.primitive);
                }
            }


        });

        DrawCircle.Controller = Marionette.Controller.extend({
            enabled: maptype.is3d(),
            initialize: function (options) {
                this.scene = options.scene;
                this.notificationEl = options.notificationEl;

                this.listenTo(wreqr.vent, 'search:circledisplay', this.showCircle);
                this.listenTo(wreqr.vent, 'search:drawcircle', this.draw);
                this.listenTo(wreqr.vent, 'search:drawstop', this.stop);
                this.listenTo(wreqr.vent, 'search:drawend', this.destroy);
            },
            showCircle: function(model) {
                if (this.enabled) {
                    var circleModel = model || new DrawCircle.CircleModel(),
                        view = new DrawCircle.CircleView({
                            scene: this.scene,
                            model: circleModel
                        });

                    if (this.view) {
                        this.view.destroyPrimitive();
                        this.view.stop();

                    }
                    view.updatePrimitive(model);
                    this.view = view;

                    return circleModel;
                }
            },
            draw: function (model) {
                if (this.enabled) {
                    var circleModel = model || new DrawCircle.CircleModel(),
                        view = new DrawCircle.CircleView({
                            scene: this.scene,
                            model: circleModel
                        });

                    if (this.view) {
                        this.view.destroyPrimitive();
                        this.view.stop();

                    }

                    view.start();
                    this.view = view;

                    this.notificationView = new NotificationView({
                        el: this.notificationEl
                    }).render();
                    this.listenToOnce(circleModel, 'EndExtent', function () {
                        this.notificationView.destroy();
                    });

                    return circleModel;
                }
            },
            stop: function () {
                if (this.enabled && this.view) {
                    this.view.stop();
                    this.view.handleRegionStop();
                    if(this.notificationView) {
                        this.notificationView.destroy();
                    }
                }
            },
            destroy: function () {
                if (this.enabled && this.view) {
                    this.view.stop();
                    this.view.destroyPrimitive();
                    this.view = undefined;
                    if(this.notificationView) {
                        this.notificationView.destroy();
                    }
                }
            }

        });


        return DrawCircle;
    });