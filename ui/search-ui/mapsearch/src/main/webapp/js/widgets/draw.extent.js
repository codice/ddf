/*global define*/

define(function (require) {
    "use strict";
    var Marionette = require('marionette'),
        Backbone = require('backbone'),
        Cesium = require('cesium'),
        _ = require('underscore'),
        ddf = require('ddf'),
        Draw = ddf.module();

    Draw.ExentModel = Backbone.Model.extend({
        defaults: {
            north: undefined,
            east: undefined,
            west: undefined,
            south: undefined
        }
    });

    Draw.Views.ExtentView = Backbone.View.extend({
        initialize: function (options) {
            this.canvas = options.scene.getCanvas();
            this.scene = options.scene;
            this.ellipsoid = options.scene.getPrimitives()
                .getCentralBody().getEllipsoid();
            this.mouseHandler = new Cesium.ScreenSpaceEventHandler(
                this.canvas);
            this.extentPrimitive = new Cesium.ExtentPrimitive();
            this.extentPrimitive.material = new Cesium.Material({
                fabric : {
                    type : 'Color',
                    uniforms : {
                        color : new Cesium.Color(1.0, 1.0, 0.0, 0.3)
                    }
                }
            });
            this.extentPrimitive.asynchronous = false;
            this.scene.getPrimitives().add(this.extentPrimitive);
            this.listenTo(this.model,'change', this.updatePrimitive);
        },
        enableInput: function () {
            var controller = this.scene
                .getScreenSpaceCameraController();
            controller.enableTranslate = true;
            controller.enableZoom = true;
            controller.enableRotate = true;
            controller.enableTilt = true;
            controller.enableLook = true;
        },
        disableInput: function () {
            var controller = this.scene
                .getScreenSpaceCameraController();
            controller.enableTranslate = false;
            controller.enableZoom = false;
            controller.enableRotate = false;
            controller.enableTilt = false;
            controller.enableLook = false;
        },
        getExtent: function (mn, mx) {

            var e = new Cesium.Extent(),
                epsilon = Cesium.Math.EPSILON7,
                modelProps;

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
            _.each(modelProps, function (val, key) {
                modelProps[key] = (val * 180 / Math.PI).toFixed(4);
            });
            this.model.set(modelProps);

            return e;
        },
        setPolyPts: function (mn, mx) {
            this.extentPrimitive.extent = this.getExtent(mn, mx);
        },

        updatePrimitive : function(model){
            var toRad = Cesium.Math.toRadians;
            var obj = model.toJSON();
            if(_.every(obj, function(val){
                return _.isUndefined(val);
            }) || _.isEmpty(obj)){
                this.scene.getPrimitives().remove(this.extentPrimitive);
                // remove primitive?
                return;
            }
            _.each(obj, function (val, key) {
                obj[key] = toRad(val);
            });
            var extent = new Cesium.Extent();
            extent.north = obj.north;
            extent.south = obj.south;
            extent.east = obj.east;
            extent.west = obj.west;
            this.extentPrimitive.extent = extent;
        },

        handleRegionStop: function (movement) {
            var cartesian = this.scene.getCamera().controller
                .pickEllipsoid(movement.position, this.ellipsoid);
            if (cartesian) {
                this.click2 = this.ellipsoid
                    .cartesianToCartographic(cartesian);
            }
            this.enableInput();
            this.mouseHandler.destroy();
            this.model.trigger("EndExtent", this.model);
        },
        handleRegionInter: function (movement) {
            var cartesian = this.scene.getCamera().controller
                .pickEllipsoid(movement.endPosition, this.ellipsoid), cartographic;
            if (cartesian) {
                cartographic = this.ellipsoid
                    .cartesianToCartographic(cartesian);
                this.setPolyPts(this.click1, cartographic);
            }
        },
        handleRegionStart: function (movement) {
            var cartesian = this.scene.getCamera().controller
                .pickEllipsoid(movement.position, this.ellipsoid), that = this;
            if (cartesian) {
                // var that = this;
                this.click1 = this.ellipsoid
                    .cartesianToCartographic(cartesian);
                this.mouseHandler.setInputAction(function (movement) {
                    that.handleRegionStop(movement);
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




        stop : function(){
            this.stopListening();
            this.enableInput();
            if(!this.mouseHandler.isDestroyed()){
                this.mouseHandler.destroy();
            }
            if(!this.extentPrimitive.isDestroyed()){
                this.scene.getPrimitives().remove(this.extentPrimitive);
            }

        }

    });

    Draw.Controller = Marionette.Controller.extend({
        initialize: function (options) {
            this.viewer = options.viewer;
            this.notificationEl = options.notificationEl;
        },

        drawExtent: function (model) {

            var bboxModel = model || new Draw.ExtentModel(),
                view = new Draw.Views.ExtentView(
                    {
                        scene: this.viewer.scene,
                        model: bboxModel
                    });
            view.start();
            this.view = view;
            this.notificationView = new Draw.Views.NotificationView({
                el : this.notificationEl
            }).render();
            this.listenToOnce(bboxModel, 'EndExtent', function(){
                this.notificationView.close();
            });
            return bboxModel;
        },

        stop : function(){
            if(this.view){
                this.view.stop();
                this.view = undefined;
                this.notificationView.close();
            }

        }
    });

    Draw.Views.NotificationView = Backbone.View.extend({
        render : function(){
            if(this.rendered){
                this.$el.hide('fast');
            }
            this.$el.empty();
            // if it gets any more complicated than this, then we should move to templates
            this.$el.append('<span>You are in Drawing Mode!</span>');
            this.$el.animate({
                height: 'show'
            },425);
            this.rendered = true;
            return this;
        },
        close : function(){
            this.$el.animate({
                height: 'hide'
            },425);
        }
    });

    return Draw;
});