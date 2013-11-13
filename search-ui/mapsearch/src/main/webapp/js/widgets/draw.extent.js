/*global define*/

define(function(require) {
    "use strict";
    var Marionette = require('marionette'), Backbone = require('backbone'), Cesium = require('cesium'), _ = require('underscore'), ddf = require('ddf'), Draw = ddf
            .module();

    Draw.ExentModel = Backbone.Model.extend({
        defaults : {
            north : undefined,
            east : undefined,
            west : undefined,
            south : undefined
        }
    });

    Draw.Views.ExtentView = Backbone.View
            .extend({
                initialize : function(options) {
                    this.canvas = options.scene.getCanvas();
                    this.scene = options.scene;
                    this.ellipsoid = options.scene.getPrimitives()
                            .getCentralBody().getEllipsoid();
                    this.mouseHandler = new Cesium.ScreenSpaceEventHandler(
                            this.canvas);
                    this.extentPrimitive = new Cesium.ExtentPrimitive();
                    this.extentPrimitive.asynchronous = false;
                    this.scene.getPrimitives().add(this.extentPrimitive);
                },
                enableInput : function() {
                    var controller = this.scene
                            .getScreenSpaceCameraController();
                    controller.enableTranslate = true;
                    controller.enableZoom = true;
                    controller.enableRotate = true;
                    controller.enableTilt = true;
                    controller.enableLook = true;
                },
                disableInput : function() {
                    var controller = this.scene
                            .getScreenSpaceCameraController();
                    controller.enableTranslate = false;
                    controller.enableZoom = false;
                    controller.enableRotate = false;
                    controller.enableTilt = false;
                    controller.enableLook = false;
                },
                getExtent : function(mn, mx) {

                    var e = new Cesium.Extent(), epsilon = Cesium.Math.EPSILON7, modelProps;

                    // Re-order so west < east and south < north
                    e.west = Math.min(mn.longitude, mx.longitude);
                    e.east = Math.max(mn.longitude, mx.longitude);
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
                    this.model.set(modelProps);

                    return e;
                },
                setPolyPts : function(mn, mx) {
                    this.extentPrimitive.extent = this.getExtent(mn, mx);
                },
                setToDegrees : function(w, s, e, n) {
                    var toRad = Cesium.Math.toRadians, mn = new Cesium.Cartographic(
                            toRad(w), toRad(s)), mx = new Cesium.Cartographic(
                            toRad(e), toRad(n));
                    this.setPolyPts(mn, mx);
                },
                handleRegionStop : function(movement) {
                    this.enableInput();
                    var cartesian = this.scene.getCamera().controller
                            .pickEllipsoid(movement.position, this.ellipsoid);
                    if (cartesian) {
                        this.click2 = this.ellipsoid
                                .cartesianToCartographic(cartesian);
                    }
                    this.mouseHandler.destroy();

                    this.trigger("EndExtent", this.model);

                },
                handleRegionInter : function(movement) {
                    var cartesian = this.scene.getCamera().controller
                            .pickEllipsoid(movement.endPosition, this.ellipsoid), cartographic;
                    if (cartesian) {
                        cartographic = this.ellipsoid
                                .cartesianToCartographic(cartesian);
                        this.setPolyPts(this.click1, cartographic);
                    }
                },
                handleRegionStart : function(movement) {
                    var cartesian = this.scene.getCamera().controller
                            .pickEllipsoid(movement.position, this.ellipsoid), that = this;
                    if (cartesian) {
                        // var that = this;
                        this.click1 = this.ellipsoid
                                .cartesianToCartographic(cartesian);
                        this.mouseHandler.setInputAction(function(movement) {
                            that.handleRegionStop(movement);
                        }, Cesium.ScreenSpaceEventType.LEFT_UP);
                        this.mouseHandler.setInputAction(function(movement) {
                            that.handleRegionInter(movement);
                        }, Cesium.ScreenSpaceEventType.MOUSE_MOVE);
                    }
                },
                start : function() {
                    this.disableInput();

                    var that = this;

                    // Now wait for start
                    this.mouseHandler.setInputAction(function(movement) {
                        that.handleRegionStart(movement);
                    }, Cesium.ScreenSpaceEventType.LEFT_DOWN);
                }

            });

    Draw.Controller = Marionette.Controller
            .extend({
                initialize : function(options) {
                    this.viewer = options.viewer;
                },

                drawExtent : function() {
                    var model = new Draw.ExtentModel(), view = new Draw.Views.ExtentView(
                            {
                                scene : this.viewer.scene,
                                model : model
                            });
                    view.start();
                    return model;
                }
            });

    Draw.Views.ButtonView = Backbone.View.extend({

    });

    return Draw;
});