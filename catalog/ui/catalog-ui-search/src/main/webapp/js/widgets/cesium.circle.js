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
    function(Marionette, Backbone, Cesium, _, wreqr, DrawBbox, maptype, NotificationView) {
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
        DrawCircle.CircleView = Marionette.View.extend({
            initialize: function() {
                this.mouseHandler = new Cesium.ScreenSpaceEventHandler(this.options.map.scene.canvas);

                this.listenTo(this.model, 'change:lat change:lon change:radius', this.updatePrimitive);
                this.updatePrimitive(this.model);
            },
            enableInput: function() {
                var controller = this.options.map.scene.screenSpaceCameraController;
                controller.enableTranslate = true;
                controller.enableZoom = true;
                controller.enableRotate = true;
                controller.enableTilt = true;
                controller.enableLook = true;
            },
            disableInput: function() {
                var controller = this.options.map.scene.screenSpaceCameraController;
                controller.enableTranslate = false;
                controller.enableZoom = false;
                controller.enableRotate = false;
                controller.enableTilt = false;
                controller.enableLook = false;
            },

            setCircleRadius: function(mn, mx) {
                var startCartographic = this.options.map.scene.globe.ellipsoid.cartographicToCartesian(mn),
                    stopCart = this.options.map.scene.globe.ellipsoid.cartographicToCartesian(mx),
                    radius = Math.abs(Cesium.Cartesian3.distance(startCartographic, stopCart));

                var modelProp = {
                    lat: (mn.latitude * 180 / Math.PI).toFixed(14),
                    lon: (mn.longitude * 180 / Math.PI).toFixed(14),
                    radius: radius

                };

                this.model.set(modelProp);

            },

            isModelReset: function(modelProp) {
                if (_.every(defaultAttrs, function(val) {
                        return _.isUndefined(modelProp[val]);
                    }) || _.isEmpty(modelProp)) {
                    return true;
                }
                return false;
            },

            updatePrimitive: function(model) {

                var modelProp = model.toJSON();
                if (this.isModelReset(modelProp)) {
                    this.options.map.scene.primitives.remove(this.primitive);
                    this.stopListening();
                    return;
                }

                if (modelProp.radius === 0 || isNaN(modelProp.radius)) {
                    modelProp.radius = 1;
                }

                this.drawBorderedCircle(model);
            },
            drawBorderedCircle: function(model) {
                // if model has been reset
                var modelProp = model.toJSON();
                if (this.isModelReset(modelProp)) {
                    return;
                }

                // first destroy old one
                if (this.primitive && !this.primitive.isDestroyed()) {
                    this.options.map.scene.primitives.remove(this.primitive);
                }

                var color = this.model.get('color');

                this.primitive = new Cesium.Primitive({
                    asynchronous: false,
                    geometryInstances: [new Cesium.GeometryInstance({
                        geometry: new Cesium.CircleOutlineGeometry({
                            center: this.options.map.scene.globe.ellipsoid.cartographicToCartesian(Cesium.Cartographic.fromDegrees(modelProp.lon, modelProp.lat)),
                            radius: modelProp.radius
                        }),
                        attributes: {
                            color: color ? Cesium.ColorGeometryInstanceAttribute.fromColor(Cesium.Color.fromCssColorString(this.model.get('color'))) : Cesium.ColorGeometryInstanceAttribute.fromColor(Cesium.Color.KHAKI)
                        }
                    })],
                    appearance: new Cesium.PerInstanceColorAppearance({
                        flat: true,
                        renderState: {
                            depthTest: {
                                enabled: true
                            },
                            lineWidth: Math.min(4.0, this.options.map.scene.maximumAliasedLineWidth)
                        }
                    })
                });

                this.options.map.scene.primitives.add(this.primitive);
            },
            handleRegionStop: function() {
                this.enableInput();
                if (!this.mouseHandler.isDestroyed()) {
                    this.mouseHandler.destroy();
                }
                this.drawBorderedCircle(this.model);
                this.stopListening(this.model, 'change:lat change:lon change:radius', this.updatePrimitive);
                this.listenTo(this.model, 'change:lat change:lon change:radius', this.drawBorderedCircle);
                this.model.trigger("EndExtent", this.model);
            },
            handleRegionInter: function(movement) {
                var cartesian = this.options.map.scene.camera.pickEllipsoid(movement.endPosition, this.options.map.scene.globe.ellipsoid),
                    cartographic;
                if (cartesian) {
                    cartographic = this.options.map.scene.globe.ellipsoid.cartesianToCartographic(cartesian);
                    this.setCircleRadius(this.click1, cartographic);
                }
            },
            handleRegionStart: function(movement) {
                var cartesian = this.options.map.scene.camera.pickEllipsoid(movement.position, this.options.map.scene.globe.ellipsoid),
                    that = this;
                if (cartesian) {
                    this.click1 = this.options.map.scene.globe.ellipsoid.cartesianToCartographic(cartesian);
                    this.mouseHandler.setInputAction(function() {
                        that.handleRegionStop();
                    }, Cesium.ScreenSpaceEventType.LEFT_UP);
                    this.mouseHandler.setInputAction(function(movement) {
                        that.handleRegionInter(movement);
                    }, Cesium.ScreenSpaceEventType.MOUSE_MOVE);
                }
            },
            start: function() {
                this.disableInput();

                var that = this;

                // Now wait for start
                this.mouseHandler.setInputAction(function(movement) {
                    that.handleRegionStart(movement);
                }, Cesium.ScreenSpaceEventType.LEFT_DOWN);
            },
            stop: function() {
                this.stopListening();
                this.enableInput();
            },

            destroyPrimitive: function() {
                if (!this.mouseHandler.isDestroyed()) {
                    this.mouseHandler.destroy();
                }
                if (this.primitive && !this.primitive.isDestroyed()) {
                    this.options.map.scene.primitives.remove(this.primitive);
                }
            },
            destroy: function(){
                this.destroyPrimitive();
                this.remove(); // backbone cleanup.
            }


        });

        DrawCircle.Controller = Marionette.Controller.extend({
            enabled: maptype.is3d(),
            initialize: function() {
                this.listenTo(wreqr.vent, 'search:circledisplay', function(model) {
                    if (this.isVisible()) {
                        this.showCircle(model);
                    }
                });
                this.listenTo(wreqr.vent, 'search:drawcircle', function(model) {
                    if (this.isVisible()) {
                        this.draw(model);
                    }
                });
                this.listenTo(wreqr.vent, 'search:drawstop', function(model) {
                    this.stop(model);
                });
                this.listenTo(wreqr.vent, 'search:drawend', function(model) {
                    this.destroy(model);
                });
                this.listenTo(wreqr.vent, 'search:destroyAllDraw', function(model) {
                    this.destroyAll(model);
                });
            },
            views: [],
            isVisible: function() {
                return this.options.map.scene.canvas.width !== 0;
            },
            destroyAll: function() {
                for (var i = this.views.length - 1; i >= 0; i -= 1) {
                    this.destroyView(this.views[i]);
                }
            },
            getViewForModel: function(model) {
                return this.views.filter(function(view) {
                    return view.model === model;
                })[0];
            },
            removeViewForModel: function(model) {
                var view = this.getViewForModel(model);
                if (view) {
                    this.views.splice(this.views.indexOf(view), 1);
                }
            },
            removeView: function(view) {
                this.views.splice(this.views.indexOf(view), 1);
            },
            addView: function(view) {
                this.views.push(view);
            },
            showCircle: function(model) {
                if (this.enabled) {
                    var circleModel = model || new DrawCircle.CircleModel();
                    /*     view = new DrawCircle.CircleView({
                             scene: this.scene,
                             model: circleModel
                         });*/


                    var existingView = this.getViewForModel(model);
                    if (existingView) {
                        existingView.stop();
                        existingView.destroyPrimitive();
                        existingView.updatePrimitive(model);
                    } else {
                        var view = new DrawCircle.CircleView({
                            map: this.options.map,
                            model: circleModel
                        });
                        view.updatePrimitive(model);
                        this.addView(view);
                    }

                    return circleModel;
                }
            },
            draw: function(model) {
                if (this.enabled) {
                    var circleModel = model || new DrawCircle.CircleModel();
                    var view = new DrawCircle.CircleView({
                        map: this.options.map,
                        model: circleModel
                    });

                    var existingView = this.getViewForModel(model);
                    if (existingView) {
                        existingView.stop();
                        existingView.destroyPrimitive();
                        this.removeView(existingView);
                    }
                    view.start();
                    this.addView(view);
                    this.notificationView = new NotificationView({
                        el: this.options.notificationEl
                    }).render();
                    this.listenToOnce(circleModel, 'EndExtent', function() {
                        this.notificationView.destroy();
                    });

                    return circleModel;
                }
            },
            stop: function(model) {
                var view = this.getViewForModel(model);
                if (view) {
                    view.stop();
                    view.handleRegionStop();
                }
                if (this.notificationView) {
                    this.notificationView.destroy();
                }
            },
            destroyView: function(view) {
                view.stop();
                view.destroyPrimitive();
                this.removeView(view);
            },
            destroy: function(model) {
                this.stop(model);
                var view = this.getViewForModel(model);
                if (view) {
                    view.stop();
                    view.destroyPrimitive();
                    this.removeView(view);
                    if (this.notificationView) {
                        this.notificationView.destroy();
                    }
                }
            }

        });


        return DrawCircle;
    });