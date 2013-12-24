/*global define*/

define(function (require) {
    "use strict";
    var Marionette = require('marionette'),
        Backbone = require('backbone'),
        Cesium = require('cesium'),
        _ = require('underscore'),
        ddf = require('ddf'),
        DrawExtent = require('./draw.extent'),
        webgl = require('webglcheck'),
        DrawCircle = ddf.module();

    DrawCircle.CircleModel = Backbone.Model.extend({
        defaults: {
            lat: undefined,
            lon: undefined,
            radius: undefined
        }
    });
    var defaultAttrs = ['lat','lon','radius'];
    DrawCircle.Views.CircleView = Backbone.View.extend({
        initialize: function (options) {
            _.bindAll(this);
            this.canvas = options.scene.getCanvas();
            this.scene = options.scene;
            this.ellipsoid = options.scene.getPrimitives().getCentralBody().getEllipsoid();
            this.mouseHandler = new Cesium.ScreenSpaceEventHandler(this.canvas);
            var modelProp = _.defaults(this.model.toJSON(), {lat: 0, lon: 0, radius: 1});
            this.primitive = new Cesium.Polygon({
                positions: Cesium.Shapes.computeCircleBoundary(
                    this.ellipsoid,
                    this.ellipsoid.cartographicToCartesian(Cesium.Cartographic.fromDegrees(modelProp.lon, modelProp.lat)),
                    modelProp.radius),
                material: new Cesium.Material({
                    fabric: {
                        type: 'Color',
                        uniforms: {
                            // translucent yellow
                            color: new Cesium.Color(1.0, 1.0, 0.0, 0.2)
                        }
                    }
                })
            });

            this.primitive.asynchronous = false;


            this.scene.getPrimitives().add(this.primitive);


            this.listenTo(this.model, 'change:lat change:lon change:radius', this.updatePrimitive);
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
                radius = Math.abs(Cesium.Cartesian3.distance(startCartographic, stopCart));

            var modelProp = {
                lat: (mn.latitude * 180 / Math.PI).toFixed(4),
                lon: (mn.longitude * 180 / Math.PI).toFixed(4),
                radius: radius

            };

            this.model.set(modelProp);

        },

        isModelReset : function(modelProp){
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
                this.scene.getPrimitives().remove(this.primitive);
                this.stopListening();
                return;
            }
            if(modelProp.radius === 0 || isNaN(modelProp.radius)){
                modelProp.radius = 1;
            }

            this.primitive.setPositions(Cesium.Shapes.computeCircleBoundary(
                this.ellipsoid,
                this.ellipsoid.cartographicToCartesian(Cesium.Cartographic.fromDegrees(modelProp.lon, modelProp.lat)),
                modelProp.radius
            ));
        },

        addBorderedCircle : function(model){
            // if model has been reset

            var modelProp = model.toJSON();
            if (this.isModelReset(modelProp)){
                return;
            }
            // first destroy old one
            if(this.primitive && !this.primitive.isDestroyed()){
                this.scene.getPrimitives().remove(this.primitive);
            }


            var circleOutlineGeometry = new Cesium.CircleOutlineGeometry({
                center : this.ellipsoid.cartographicToCartesian(Cesium.Cartographic.fromDegrees(modelProp.lon, modelProp.lat)),
                radius : modelProp.radius
            });

            var circleOutlineInstance = new Cesium.GeometryInstance({
                geometry : circleOutlineGeometry,
                attributes : {
                    color : Cesium.ColorGeometryInstanceAttribute.fromColor(Cesium.Color.KHAKI)
                }
            });

            this.primitive= new Cesium.Primitive({
                geometryInstances : [circleOutlineInstance],
                appearance : new Cesium.PerInstanceColorAppearance({
                    flat : true,
                    renderState : {
                        depthTest : {
                            enabled : true
                        },
                        lineWidth : Math.min(4.0, this.scene.getContext().getMaximumAliasedLineWidth())
                    }
                })
            });
            this.scene.getPrimitives().add(this.primitive);

        },
        handleRegionStop: function () {
            this.enableInput();
            if (!this.mouseHandler.isDestroyed()) {
                this.mouseHandler.destroy();
            }
            this.addBorderedCircle(this.model);
            this.stopListening(this.model, 'change:lat change:lon change:radius', this.updatePrimitive);
            this.listenTo(this.model, 'change:lat change:lon change:radius', this.addBorderedCircle);
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

        destroyPrimitive: function(){
            if (!this.mouseHandler.isDestroyed()) {
                this.mouseHandler.destroy();
            }
            if(this.primitive && !this.primitive.isDestroyed()){
                this.scene.getPrimitives().remove(this.primitive);
            }
        }


    });

    DrawCircle.Controller = Marionette.Controller.extend({
        enabled: webgl.isAvailable(),
        initialize: function (options) {
            this.scene = options.scene;
            this.notificationEl = options.notificationEl;
        },

        draw: function (model) {
            if (this.enabled) {
                var circleModel = model || new DrawCircle.CircleModel(),
                    view = new DrawCircle.Views.CircleView({
                        scene: this.scene,
                        model: circleModel
                    });

                if(this.view){
                    this.view.destroyPrimitive();
                    this.view.stop();

                }

                view.start();
                this.view = view;

                this.notificationView = new DrawExtent.Views.NotificationView({
                    el: this.notificationEl
                }).render();
                this.listenToOnce(circleModel, 'EndExtent', function () {
                    this.notificationView.close();
                });

                return circleModel;
            }
        },
        stopDrawing: function() {
            if (this.enabled && this.view) {
                this.view.stop();
                this.view.handleRegionStop();
                this.notificationView.close();
            }
        },
        stop: function () {
            if (this.enabled && this.view) {
                this.view.stop();
                this.view.destroyPrimitive();
                this.view = undefined;
                this.notificationView.close();
            }
        }

    });


    return DrawCircle;
});