/*global define*/

define(function (require) {
    "use strict";
    var $ = require('jquery'),
        Backbone = require('backbone'),
        _ = require('underscore'),
        Cesium = require('cesium'),
        Views = {};



    Views.PointView = Backbone.View.extend({
        initialize: function (options) {
            this.geoController = options.geoController;
            this.buildBillboard();
            this.listenTo(this.model, 'change:context', this.toggleSelection);
            this.listenTo(this.geoController, 'click:left', this.onMapLeftClick);
            this.listenTo(this.geoController, 'doubleclick:left', this.onMapDoubleClick);
            this.color = options.color || {red : 1,green :0.6431372549019608, blue:0.403921568627451, alpha : 1 };
            this.imageIndex = options.imageIndex || 0;
        },

        render: function () {

            return this;
        },

        buildBillboard : function(){
            var view = this;
            this.geoController.billboardPromise.then(function () {
                var point = view.model.get('geometry').getPoint();
                view.billboard = view.geoController.billboardCollection.add({
                    imageIndex : view.imageIndex,
                    position : view.geoController.ellipsoid.cartographicToCartesian(
                        Cesium.Cartographic.fromDegrees(
                            point.longitude,
                            point.latitude,
                            point.altitude
                        )
                    ),
                    horizontalOrigin : Cesium.HorizontalOrigin.CENTER,
                    verticalOrigin : Cesium.VerticalOrigin.BOTTOM,
                    scaleByDistance : new Cesium.NearFarScalar(1.0, 1.0, 1.5e7, 0.5)
                });
                view.billboard.setColor(view.color);
                view.billboard.setScale(0.41);
                view.billboard.hasScale = true;
            });
        },

        toggleSelection : function(){
            console.log('toggling selection');
            var view = this;

            if(view.billboard.getEyeOffset().z < 0){
                view.billboard.setEyeOffset(new Cesium.Cartesian3(0,0,0));
            } else{
                view.billboard.setEyeOffset(new Cesium.Cartesian3(0,0,-10));
            }

            if (view.model.get('context')) {
                view.billboard.setScale(0.5);
            } else {
                view.billboard.setScale(0.41);
            }

        },
        onMapLeftClick : function(){
            var view = this;
            // find out if this click is on us
            if (_.has(event, 'object') && event.object === view.billboard) {
                view.model.set('context', true);
            }
        },
        onMapDoubleClick : function(){
            var view = this;
            // find out if this click is on us
            if (_.has(event, 'object') && event.object === view.billboard) {
                view.geoController.flyToLocation(view.model);

            }
        },


        close : function(){
            var view = this;

            // If there is already a billboard for this view, remove it
            if (!_.isUndefined(view.billboard)) {
               view.geoController.billboardCollection.remove(view.billboard);

            }
        }

    });

    Views.RegionView = Backbone.View.extend({
        initialize: function (options) {
            this.map = options.map;
        },
        render: function () {

        },

        close : function(){

        }
    });
    return Views;
});