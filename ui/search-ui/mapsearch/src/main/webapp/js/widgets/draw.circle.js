/*global define*/

define(function (require) {
    "use strict";
    var Marionette = require('marionette'),
        Backbone = require('backbone'),
        Cesium = require('cesium'),
        _ = require('underscore'),
        ddf = require('ddf'),
        DrawExtent = require('./draw.extent'),
        DrawCircle = ddf.module();

    DrawCircle.CircleModel = Backbone.Model.extend({
        defaults: {
            latitude: undefined,
            longitude: undefined,
            radius: undefined
        }
    });

    DrawCircle.Views.CircleView = Backbone.View.extend({
        initialize: function (options) {
            _.bindAll(this);
            this.canvas = options.scene.getCanvas();
            this.scene = options.scene;
            this.ellipsoid = options.scene.getPrimitives().getCentralBody().getEllipsoid();
            this.mouseHandler = new Cesium.ScreenSpaceEventHandler(this.canvas);
            var modelProp = _.defaults(this.model.toJSON(), {latitude : 0, longitude: 0, radius:1});
            this.primitive = new Cesium.Polygon({
                positions : Cesium.Shapes.computeCircleBoundary(
                    this.ellipsoid,
                    this.ellipsoid.cartographicToCartesian(Cesium.Cartographic.fromDegrees(modelProp.longitude, modelProp.latitude)),
                    modelProp.radius),
                material : new Cesium.Material({
                    fabric : {
                        type : 'Grid',
                        uniforms : {
                            color : Cesium.Color.BLACK
                        }
                    }
                })
            });
            this.primitive.asynchronous = false;


            this.scene.getPrimitives().add(this.primitive);


            this.listenTo(this.model,'change', this.updatePrimitive);
        },
        enableInput: function () {
            var controller = this.scene.getScreenSpaceCameraController();
            controller.enableTranslate = true;
            controller.enableZoom = true;
            controller.enableRotate = true;
            controller.enableTilt = true;
            controller.enableLook = true;
        },
        disableInput: function () {
            var controller = this.scene.getScreenSpaceCameraController();
            controller.enableTranslate = false;
            controller.enableZoom = false;
            controller.enableRotate = false;
            controller.enableTilt = false;
            controller.enableLook = false;
        },

        setCircleRadius: function (mn, mx) {
            var startCartographic = this.ellipsoid.cartographicToCartesian(mn),
                stopCart = this.ellipsoid.cartographicToCartesian(mx),
                radius = Math.abs(Cesium.Cartesian3.distance(startCartographic,stopCart));

            var modelProp = {
                latitude : (mn.latitude * 180 / Math.PI).toFixed(4),
                longitude : (mn.longitude * 180 / Math.PI).toFixed(4),
                radius : radius

            };

            this.model.set(modelProp);

        },

        updatePrimitive : function(model){

            var modelProp = model.toJSON();
            if(_.every(modelProp, function(val){
                return _.isUndefined(val);
            }) || _.isEmpty(modelProp)){
                this.scene.getPrimitives().remove(this.primitive);
                return;
            }

            this.primitive.setPositions(Cesium.Shapes.computeCircleBoundary(
                this.ellipsoid,
                this.ellipsoid.cartographicToCartesian(Cesium.Cartographic.fromDegrees(modelProp.longitude, modelProp.latitude)),
                modelProp.radius,
                100
            ));
        },

        handleRegionStop: function (movement) {
            var cartesian = this.scene.getCamera().controller.pickEllipsoid(movement.position, this.ellipsoid);
            if (cartesian) {
                this.click2 = this.ellipsoid.cartesianToCartographic(cartesian);
            }
            this.enableInput();
            this.mouseHandler.destroy();
            this.model.trigger("EndExtent", this.model);
        },
        handleRegionInter: function (movement) {
            var cartesian = this.scene.getCamera().controller.pickEllipsoid(movement.endPosition, this.ellipsoid),
                cartographic;
            if (cartesian) {
                cartographic = this.ellipsoid.cartesianToCartographic(cartesian);
                this.setCircleRadius(this.click1, cartographic);
            }
        },
        handleRegionStart: function (movement) {
            var cartesian = this.scene.getCamera().controller.pickEllipsoid(movement.position, this.ellipsoid),
                that = this;
            if (cartesian) {
                // var that = this;
                this.click1 = this.ellipsoid.cartesianToCartographic(cartesian);
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

        }


    });

    DrawCircle.Controller = Marionette.Controller.extend({
        initialize: function (options) {
            this.viewer = options.viewer;
            this.notificationEl = options.notificationEl;
        },

        draw: function (model) {
            var circleModel = model || new DrawCircle.CircleModel(),
                view = new DrawCircle.Views.CircleView({
                    scene: this.viewer.scene,
                    model: circleModel
                });
            view.start();
            this.view = view;

            this.notificationView = new DrawExtent.Views.NotificationView({
                el : this.notificationEl
            }).render();
            this.listenToOnce(circleModel, 'EndExtent', function(){
                this.notificationView.close();
            });

            return circleModel;
        },
        stop : function(){
            if(this.view){
                this.view.stop();
                this.view = undefined;
                this.notificationView.close();
            }
        }
    });


    return DrawCircle;
});