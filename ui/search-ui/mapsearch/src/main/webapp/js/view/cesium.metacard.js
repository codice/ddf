/*global define*/

define(function (require) {
    "use strict";
    var Backbone = require('backbone'),
        Marionette = require('marionette'),
        _ = require('underscore'),
        Cesium = require('cesium'),
        Views = {};



    Views.PointView = Marionette.ItemView.extend({
        initialize: function (options) {
            this.geoController = options.geoController;
            this.buildBillboard();
            this.listenTo(this.model, 'change:context', this.toggleSelection);
            this.listenTo(this.geoController, 'click:left', this.onMapLeftClick);
            this.listenTo(this.geoController, 'doubleclick:left', this.onMapDoubleClick);
            this.color = options.color || {red : 1,green :0.6431372549019608, blue:0.403921568627451, alpha : 1 };
            this.imageIndex = options.imageIndex || 0;
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
            }).fail(function(error){
                    console.log('error:  ', error.stack ? error.stack: error);
                });
        },

        toggleSelection : function(){
            var view = this;

            if(view.billboard.getEyeOffset().z < 0){
                view.billboard.setEyeOffset(new Cesium.Cartesian3(0,0,0));
            } else{
                view.billboard.setEyeOffset(new Cesium.Cartesian3(0,0,-10));
            }

            if (view.model.get('context')) {
                view.billboard.setScale(0.5);
                view.billboard.setImageIndex(view.imageIndex+1);
            } else {
                view.billboard.setScale(0.41);
                view.billboard.setImageIndex(view.imageIndex);
            }

        },
        onMapLeftClick : function(event){
            var view = this;
            // find out if this click is on us
            if (_.has(event, 'object') && event.object === view.billboard) {
                view.model.set('context', true);
            }
        },
        onMapDoubleClick : function(event){
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
            this.stopListening();
        }

    });

    Views.RegionView = Marionette.ItemView.extend({
        initialize: function (options) {
            this.map = options.map;
        },
        render: function () {

        },

        close : function(){

        }
    });

    Views.ResultsView =  Marionette.CollectionView.extend({
        itemView : Backbone.View,
        initialize : function(options){
            this.geoController = options.geoController;
        },

        buildItemView : function(item, ItemViewType, itemViewOptions){
            var metacard = item.get('metacard'),
                geometry = metacard.get('geometry'),
                ItemView;
            if(!geometry){
                return new ItemViewType();
            }
            // build the final list of options for the item view type.
            var options = _.extend({model: metacard, geoController : this.geoController}, itemViewOptions);

            if(geometry.isPoint()){
                ItemView = Views.PointView;
            }
            else if(geometry.isPolygon()){
                ItemView = Views.RegionView;
            }
            else {
                throw new Error("No view for this geometry");
            }

            // create the item view instance
            var view = new ItemView(options);
            // return it
            return view;
        }
    });
    return Views;
});