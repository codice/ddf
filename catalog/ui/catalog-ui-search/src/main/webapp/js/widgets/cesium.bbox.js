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
        'maptype',
        './notification.view'
    ],
    function(Marionette, Backbone, Cesium, _, wreqr, maptype, NotificationView) {
        "use strict";
        var Draw = {};

        Draw.BboxModel = Backbone.Model.extend({
            defaults: {
                north: undefined,
                east: undefined,
                west: undefined,
                south: undefined
            }
        });
        var defaultAttrs = ['north', 'east', 'west', 'south'];
        Draw.BboxView = Marionette.View.extend({
            initialize: function() {
                this.mouseHandler = new Cesium.ScreenSpaceEventHandler(this.options.map.scene.canvas);
                this.listenTo(this.model, 'change:mapNorth change:mapSouth change:mapEast change:mapWest', this.updatePrimitive);
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
            setModelFromClicks: function(mn, mx) {

                var e = new Cesium.Rectangle(),
                    epsilon = Cesium.Math.EPSILON14,
                    modelProps;

                if (!this.lastLongitude) {
                    this.crossDateLine = false;
                    this.lastLongitude = mx.longitude;
                } else {
                    if (this.lastLongitude > 0 && mx.longitude > 0 && mn.longitude > 0) {
                        //west of the date line
                        this.crossDateLine = false;
                        //track direction of the bbox
                        if (this.lastLongitude > mx.longitude) {
                            if (this.dir === "east") {
                                if (mx.longitude < mn.longitude) {
                                    this.dir = "west";
                                }
                            } else {
                                this.dir = "west";
                            }
                        } else if (this.lastLongitude < mx.longitude) {
                            if (this.dir === "west") {
                                if (mx.longitude > mn.longitude) {
                                    this.dir = "east";
                                }
                            } else {
                                this.dir = "east";
                            }
                        }
                    } else if (this.lastLongitude > 0 && mx.longitude < 0 && mn.longitude > 0) {
                        //crossed date line from west to east
                        this.crossDateLine = !(this.dir && this.dir === "west");
                    } else if (this.lastLongitude < 0 && mx.longitude > 0 && mn.longitude > 0) {
                        //moved back across date line to same quadrant
                        this.crossDateLine = false;
                    } else if (this.lastLongitude < 0 && mx.longitude < 0 && mn.longitude < 0) {
                        //east of the date line
                        this.crossDateLine = false;
                        //track direction of the bbox
                        if (this.lastLongitude < mx.longitude) {
                            if (this.dir === "west") {
                                if (mx.longitude > mn.longitude) {
                                    this.dir = "east";
                                }
                            } else {
                                this.dir = "east";
                            }
                        } else if (this.lastLongitude > mx.longitude) {
                            if (this.dir === "east") {
                                if (mx.longitude < mn.longitude) {
                                    this.dir = "west";
                                }
                            } else {
                                this.dir = "west";
                            }
                        }
                    } else if (this.lastLongitude < 0 && mx.longitude > 0 && mn.longitude < 0) {
                        //crossed date line from east to west
                        this.crossDateLine = !(this.dir && this.dir === "east");
                    } else if (this.lastLongitude > 0 && mx.longitude < 0 && mn.longitude < 0) {
                        //moved back across date line to same quadrant
                        this.crossDateLine = false;
                    }
                    this.lastLongitude = mx.longitude;
                }

                // Re-order so west < east and south < north
                if (this.crossDateLine) {
                    e.east = Math.min(mn.longitude, mx.longitude);
                    e.west = Math.max(mn.longitude, mx.longitude);
                } else {
                    e.east = Math.max(mn.longitude, mx.longitude);
                    e.west = Math.min(mn.longitude, mx.longitude);
                }
                e.south = Math.min(mn.latitude, mx.latitude);
                e.north = Math.max(mn.latitude, mx.latitude);

                // Check for approx equal (shouldn't require abs due to
                // re-order)

                if ((e.east - e.west) < epsilon) {
                    e.east += epsilon * 2.0;
                }

                if ((e.north - e.south) < epsilon) {
                    e.north += epsilon * 2.0;
                }

                modelProps = _.pick(e, 'north', 'east', 'west', 'south');
                _.each(modelProps, function(val, key) {
                    modelProps[key] = (val * 180 / Math.PI).toFixed(14);
                });
                this.model.set(modelProps);

                return e;
            },

            modelToRectangle: function(model) {
                var toRad = Cesium.Math.toRadians;
                var obj = model.toJSON();
                if (_.every(defaultAttrs, function(val) {
                        return _.isUndefined(obj[val]);
                    }) || _.isEmpty(obj)) {
                    if (this.options.map.scene && this.options.map.scene.primitives) {
                        this.options.map.scene.primitives.remove(this.primitive);
                    }
                    this.stopListening();
                    return;
                }
                _.each(obj, function(val, key) {
                    obj[key] = toRad(val);
                });
                var rectangle = new Cesium.Rectangle();
                if (!obj.north || isNaN(obj.north) || !obj.south || isNaN(obj.south) || !obj.east || isNaN(obj.east) || !obj.west || isNaN(obj.west)) {
                    return null;
                }

                rectangle.north = obj.mapNorth;
                rectangle.south = obj.mapSouth;
                rectangle.east = obj.mapEast;
                rectangle.west = obj.mapWest;
                return rectangle;
            },

            updatePrimitive: function(model) {
                var rectangle = this.modelToRectangle(model);
                // make sure the current model has width and height before drawing
                if (rectangle && !_.isUndefined(rectangle) && (rectangle.north !== rectangle.south && rectangle.east !== rectangle.west)) {
                    this.drawBorderedRectangle(rectangle);
                    //only call this if the mouse button isn't pressed, if we try to draw the border while someone is dragging
                    //the filled in shape won't show up
                    if (!this.buttonPressed) {
                        this.drawBorderedRectangle(rectangle);
                    }
                }
            },

            updateGeometry: function(model) {
                var rectangle = this.modelToRectangle(model);
                if (rectangle && !_.isUndefined(rectangle) && (rectangle.north !== rectangle.south && rectangle.east !== rectangle.west)) {
                    this.drawBorderedRectangle(rectangle);
                }
            },

            drawBorderedRectangle: function(rectangle) {

                if (!rectangle) {
                    // handles case where model changes to empty vars and we don't want to draw anymore
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
                        geometry: new Cesium.RectangleOutlineGeometry({
                            rectangle: rectangle
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
                this.mouseHandler.destroy();
                if (this.primitive) {
                    this.drawBorderedRectangle(this.primitive.rectangle);
                }
                this.stopListening(this.model, 'change:mapNorth change:mapSouth change:mapEast change:mapWest', this.updatePrimitive);
                this.listenTo(this.model, 'change:mapNorth change:mapSouth change:mapEast change:mapWest', this.updateGeometry);

                this.model.trigger("EndExtent", this.model);
                this.dir = undefined;
                this.lastLongitude = undefined;
                this.crossDateLine = undefined;
            },
            handleRegionInter: function(movement) {
                var cartesian = this.options.map.scene.camera
                    .pickEllipsoid(movement.endPosition, this.options.map.scene.globe.ellipsoid),
                    cartographic;
                if (cartesian) {
                    cartographic = this.options.map.scene.globe.ellipsoid.cartesianToCartographic(cartesian);
                    this.setModelFromClicks(this.click1, cartographic);
                }
            },
            handleRegionStart: function(movement) {
                var cartesian = this.options.map.scene.camera
                    .pickEllipsoid(movement.position, this.options.map.scene.globe.ellipsoid),
                    that = this;
                if (cartesian) {
                    // var that = this;
                    this.click1 = this.options.map.scene.globe.ellipsoid
                        .cartesianToCartographic(cartesian);
                    this.mouseHandler.setInputAction(function() {
                        that.buttonPressed = false;
                        that.handleRegionStop();
                    }, Cesium.ScreenSpaceEventType.LEFT_UP);
                    this.mouseHandler.setInputAction(function(movement) {
                        that.buttonPressed = true;
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
            }

        });

        Draw.Controller = Marionette.Controller.extend({
            enabled: maptype.is3d(),
            initialize: function() {
                this.listenTo(wreqr.vent, 'search:bboxdisplay', function(model) {
                    if (this.isVisible()) {
                        this.showBox(model);
                    }
                });
                this.listenTo(wreqr.vent, 'search:drawbbox', function(model) {
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
            showBox: function(model) {
                if (this.enabled) {
                    var bboxModel = model || new Draw.BboxModel();

                    var existingView = this.getViewForModel(model);
                    if (existingView) {
                        existingView.stop();
                        existingView.destroyPrimitive();
                    }
                    existingView.updatePrimitive(model);

                    return bboxModel;
                }
            },
            draw: function(model) {
                if (this.enabled) {
                    var bboxModel = model || new Draw.BboxModel();
                    var view = new Draw.BboxView({
                        map: this.options.map,
                        model: bboxModel
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
                    bboxModel.trigger('BeginExtent');
                    this.listenToOnce(bboxModel, 'EndExtent', function() {
                        this.notificationView.destroy();
                    });

                    return bboxModel;
                }
            },
            stop: function(model) {
                var view = this.getViewForModel(model);
                if (view) {
                    view.stop();
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

        return Draw;
    });