/*global define*/

define(function (require) {
    "use strict";
    var Marionette = require('marionette'),
        Backbone = require('backbone'),
        Cesium = require('cesium'),
        _ = require('underscore'),
        ddf = require('ddf'),
        webgl = require('webglcheck'),
        Draw = ddf.module();

    Draw.ExentModel = Backbone.Model.extend({
        defaults: {
            north: undefined,
            east: undefined,
            west: undefined,
            south: undefined
        }
    });
    var defaultAttrs = ['north','east','west','south'];
    Draw.Views.ExtentView = Backbone.View.extend({
        initialize: function (options) {
            this.canvas = options.scene.getCanvas();
            this.scene = options.scene;
            this.ellipsoid = options.scene.getPrimitives()
                .getCentralBody().getEllipsoid();
            this.mouseHandler = new Cesium.ScreenSpaceEventHandler(
                this.canvas);
            this.primitive = new Cesium.ExtentPrimitive();
            this.primitive.material = new Cesium.Material({
                fabric: {
                    type: 'Color',
                    uniforms: {
                        color: new Cesium.Color(1.0, 1.0, 0.0, 0.2)
                    }
                }
            });
            this.primitive.asynchronous = false;
            this.scene.getPrimitives().add(this.primitive);
            this.listenTo(this.model, 'change:north change:south change:east change:west', this.updatePrimitive);
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
        setModelFromClicks: function (mn, mx) {

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

        modelToExtent : function(model){
            var toRad = Cesium.Math.toRadians;
            var obj = model.toJSON();
            if (_.every(defaultAttrs, function (val) {
                return _.isUndefined(obj[val]);
            }) || _.isEmpty(obj)) {
                this.scene.getPrimitives().remove(this.primitive);
                this.stopListening();
                return;
            }
            _.each(obj, function (val, key) {
                obj[key] = toRad(val);
            });
            var extent = new Cesium.Extent();
            if(!obj.north || isNaN(obj.north) ||
                !obj.south || isNaN(obj.south) ||
                !obj.east || isNaN(obj.east) ||
                !obj.west || isNaN(obj.west)) {
                return null;
            }

            extent.north = obj.north;
            extent.south = obj.south;
            extent.east = obj.east;
            extent.west = obj.west;
            return extent;
        },

        updatePrimitive: function (model) {
            var extent = this.modelToExtent(model);
            // make sure the current model has width and height before drawing
            if (extent && !_.isUndefined(extent) && (extent.north !== extent.south && extent.east !== extent.west)) {
                this.primitive.extent = extent;
                //only call this if the mouse button isn't pressed, if we try to draw the border while someone is dragging
                //the filled in shape won't show up
                if(!this.buttonPressed) {
                    this.addBorderedExtent(extent);
                }
            }
        },

        updateGeometry : function(model){
            var extent = this.modelToExtent(model);
            if (extent && !_.isUndefined(extent) && (extent.north !== extent.south && extent.east !== extent.west)) {
                this.addBorderedExtent(extent);
            }
        },

        addBorderedExtent : function(extent){

            if(!extent){
                // handles case where model changes to empty vars and we don't want to draw anymore
                return;
            }

            // first destroy old one
            if(this.primitive && !this.primitive.isDestroyed()){
                this.scene.getPrimitives().remove(this.primitive);
            }

            this.primitive = new Cesium.Primitive({
                geometryInstances: new Cesium.GeometryInstance({
                    geometry: new Cesium.ExtentOutlineGeometry({
                        extent: extent
                    }),
                    attributes: {
                        color: Cesium.ColorGeometryInstanceAttribute.fromColor(Cesium.Color.KHAKI)
                    }
                }),
                appearance: new Cesium.PerInstanceColorAppearance({
                    flat: true,
                    renderState: {
                        depthTest: {
                            enabled: true
                        },
                        lineWidth: Math.min(4.0, this.scene.getContext().getMaximumAliasedLineWidth())
                    }
                })
            });
            this.scene.getPrimitives().add(this.primitive);
        },

        handleRegionStop: function () {
            this.enableInput();
            this.mouseHandler.destroy();
            this.addBorderedExtent(this.primitive.extent);
            this.stopListening(this.model, 'change:north change:south change:east change:west', this.updatePrimitive);
            this.listenTo(this.model, 'change:north change:south change:east change:west', this.updateGeometry);

            this.model.trigger("EndExtent", this.model);
        },
        handleRegionInter: function (movement) {
            var cartesian = this.scene.getCamera().controller
                .pickEllipsoid(movement.endPosition, this.ellipsoid), cartographic;
            if (cartesian) {
                cartographic = this.ellipsoid
                    .cartesianToCartographic(cartesian);
                this.setModelFromClicks(this.click1, cartographic);
            }
        },
        handleRegionStart: function (movement) {
            var cartesian = this.scene.getCamera().controller
                .pickEllipsoid(movement.position, this.ellipsoid), that = this;
            if (cartesian) {
                // var that = this;
                this.click1 = this.ellipsoid
                    .cartesianToCartographic(cartesian);
                this.mouseHandler.setInputAction(function () {
                    that.buttonPressed = false;
                    that.handleRegionStop();
                }, Cesium.ScreenSpaceEventType.LEFT_UP);
                this.mouseHandler.setInputAction(function (movement) {
                    that.buttonPressed = true;
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

    Draw.Controller = Marionette.Controller.extend({
        enabled: webgl.isAvailable(),
        initialize: function (options) {
            this.scene = options.scene;
            this.notificationEl = options.notificationEl;
        },
        draw: function (model) {
            if (this.enabled) {
                var bboxModel = model || new Draw.ExtentModel(),
                    view = new Draw.Views.ExtentView(
                        {
                            scene: this.scene,
                            model: bboxModel
                        });

                if(this.view){
                    this.view.destroyPrimitive();
                    this.view.stop();

                }
                view.start();
                this.view = view;
                this.notificationView = new Draw.Views.NotificationView({
                    el: this.notificationEl
                }).render();
                this.listenToOnce(bboxModel, 'EndExtent', function () {
                    this.notificationView.close();
                });

                return bboxModel;
            }
        },
        stopDrawing: function() {
            if (this.enabled && this.view) {
                this.view.stop();
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

    Draw.Views.NotificationView = Backbone.View.extend({
        render: function () {
            if (this.rendered) {
                this.$el.hide('fast');
            }
            this.$el.empty();
            // if it gets any more complicated than this, then we should move to templates
            this.$el.append('<span>You are in Drawing Mode!</span>');
            this.$el.animate({
                height: 'show'
            }, 425);
            this.rendered = true;
            return this;
        },
        close: function () {
            this.$el.animate({
                height: 'hide'
            }, 425);
        }
    });

    return Draw;
});