/*global define*/

define(function (require) {
    "use strict";
    var Backbone = require('backbone'),
        Marionette = require('marionette'),
        _ = require('underscore'),
        Cesium = require('cesium'),
        dir = require('direction'),
        Views = {};


    Views.PointView = Marionette.ItemView.extend({
        initialize: function (options) {
            this.geoController = options.geoController;
            this.listenTo(this.model, 'change:context', this.toggleSelection);
            this.listenTo(this.geoController, 'click:left', this.onMapLeftClick);
            this.listenTo(this.geoController, 'doubleclick:left', this.onMapDoubleClick);
            this.color = options.color || {red: 1, green: 0.6431372549019608, blue: 0.403921568627451, alpha: 1 };
            this.imageIndex = options.imageIndex || 0;
            this.buildBillboard();

        },

        buildBillboard: function () {
            var view = this;
            this.geoController.billboardPromise.then(function () {
                var point = view.model.get('geometry').getPoint();
                view.billboard = view.geoController.billboardCollection.add({
                    imageIndex: view.imageIndex,
                    position: view.geoController.ellipsoid.cartographicToCartesian(
                        Cesium.Cartographic.fromDegrees(
                            point.longitude,
                            point.latitude,
                            point.altitude
                        )
                    ),
                    horizontalOrigin: Cesium.HorizontalOrigin.CENTER,
                    verticalOrigin: Cesium.VerticalOrigin.BOTTOM,
                    scaleByDistance: new Cesium.NearFarScalar(1.0, 1.0, 1.5e7, 0.5)
                });
                view.billboard.setColor(view.color);
                view.billboard.setScale(0.41);
                view.billboard.hasScale = true;
            }).fail(function (error) {
                    console.log('error:  ', error.stack ? error.stack : error);
                });
        },

        toggleSelection: function () {
            var view = this;

            if (view.billboard.getEyeOffset().z < 0) {
                view.billboard.setEyeOffset(new Cesium.Cartesian3(0, 0, 0));
            } else {
                view.billboard.setEyeOffset(new Cesium.Cartesian3(0, 0, -10));
            }

            if (view.model.get('context')) {
                view.billboard.setScale(0.5);
                view.billboard.setImageIndex(view.imageIndex + 1);
            } else {
                view.billboard.setScale(0.41);
                view.billboard.setImageIndex(view.imageIndex);
            }

        },
        onMapLeftClick: function (event) {
            var view = this;
            // find out if this click is on us
            if (_.has(event, 'object') && event.object === view.billboard) {
                view.model.set('direction', dir.none);
                view.model.set('context', true);
            }
        },
        onMapDoubleClick: function (event) {
            var view = this;
            // find out if this click is on us
            if (_.has(event, 'object') && event.object === view.billboard) {
                view.geoController.flyToLocation(view.model);

            }
        },


        onClose: function () {
            var view = this;

            // If there is already a billboard for this view, remove it
            if (!_.isUndefined(view.billboard)) {
                view.geoController.billboardCollection.remove(view.billboard);

            }
            this.stopListening();
        }

    });

    Views.RegionView = Views.PointView.extend({
        initialize: function (options) {
            this.geoController = options.geoController;
            this.listenTo(this.model, 'change:context', this.toggleSelection);
            this.listenTo(this.geoController, 'click:left', this.onMapLeftClick);
            this.listenTo(this.geoController, 'doubleclick:left', this.onMapDoubleClick);
            this.color = options.color || {red: 1, green: 0.6431372549019608, blue: 0.403921568627451, alpha: 1 };
            // a light blue
            this.polygonColor = options.polygonColor || new Cesium.Color(0.3568627450980392, 0.5764705882352941, 0.8823529411764706, 0.2);
            // a grey matching the outline of the default marker
            this.outlineColor = options.outlineColor || new Cesium.Color(0.707,0.707,0.707,1);//new Cesium.Color(0.607,0.607,0.607,1);
            this.imageIndex = options.imageIndex || 0;
            this.buildBillboard();
            this.buildPolygon();

        },

        isThisPrimitive : function(event){
            var view = this;
            // could wrap this in one huge if statement, but this seems more readable
            if(_.has(event,'object')){
                if(event.object === view.billboard){
                    return true;
                }
                if(_.contains(view.polygons, event.object)){
                    return true;
                }
            }
            return false;
        },
        toggleSelection : function(){
            var view = this;
            // call super for billboard modification
            Views.PointView.prototype.toggleSelection.call(this);
            if (view.model.get('context')) {
                view.setPolygonSelected();
            }else{
                view.setPolygonUnselected();
            }

        },
        onMapLeftClick: function (event) {
            var view = this;
            // find out if this click is on us
            if (view.isThisPrimitive(event)) {
                view.model.set('direction', dir.none);
                view.model.set('context', true);
            }
        },
        onMapDoubleClick: function (event) {
            var view = this;
            // find out if this click is on us
            if (view.isThisPrimitive(event)) {
                view.geoController.flyToLocation(view.model);

            }
        },


        buildPolygon: function () {
            var view = this;
            var points = view.model.get('geometry').getPolygon();
            var cartPoints = _.map(points, function (point) {
                return Cesium.Cartographic.fromDegrees(point.longitude, point.latitude, point.altitude);
            });
            var positions = view.geoController.ellipsoid.cartographicArrayToCartesianArray(cartPoints);
            var outlineGeometry = Cesium.PolygonOutlineGeometry.fromPositions({
                    positions: positions
            });

            // Outline
            var polygonOutlineInstance = new Cesium.GeometryInstance({
                geometry:outlineGeometry ,
                attributes: {
                    color: Cesium.ColorGeometryInstanceAttribute.fromColor(view.outlineColor),
                    show : new Cesium.ShowGeometryInstanceAttribute(true)
                },
                id : 'outline'
            });
            var outlineColorAppearance = new Cesium.PerInstanceColorAppearance({
                flat: true,
                renderState: {
                    depthTest: {
                        enabled: true
                    },
                    lineWidth: Math.min(2.0, view.geoController.scene.getContext().getMaximumAliasedLineWidth())
                }
            });

            // Blue polygon
            var polygonInstance = new Cesium.GeometryInstance({
                geometry: Cesium.PolygonGeometry.fromPositions({
                    positions: positions,
                    vertexFormat: Cesium.PerInstanceColorAppearance.VERTEX_FORMAT
                }),
                attributes: {
                    color: Cesium.ColorGeometryInstanceAttribute.fromColor(view.polygonColor),
                    show : new Cesium.ShowGeometryInstanceAttribute(false)
                },
                id : 'polyfill'
            });

            // Add primitives
            view.polygons = [
                new Cesium.Primitive({
                    geometryInstances: [polygonOutlineInstance],
                    appearance: outlineColorAppearance
                }),
                new Cesium.Primitive({
                    geometryInstances: [polygonInstance],
                    appearance: new Cesium.PerInstanceColorAppearance({
                        closed: true
                    })
                })
            ];

            _.each(view.polygons, function (polygonPrimitive) {
                view.geoController.scene.getPrimitives().add(polygonPrimitive);
            });

        },

        setPolygonSelected : function(){
            var view = this;
            var attributes = view.polygons[0].getGeometryInstanceAttributes('outline');
            attributes.color = Cesium.ColorGeometryInstanceAttribute.toValue(Cesium.Color.BLACK);

            var fillAttributes = view.polygons[1].getGeometryInstanceAttributes('polyfill');
            fillAttributes.show = Cesium.ShowGeometryInstanceAttribute.toValue(true, fillAttributes.show);
        },

        setPolygonUnselected : function(){
            var view = this;
            var attributes = view.polygons[0].getGeometryInstanceAttributes('outline');
            attributes.color = Cesium.ColorGeometryInstanceAttribute.toValue(view.outlineColor);

            var fillAttributes = view.polygons[1].getGeometryInstanceAttributes('polyfill');
            fillAttributes.show = Cesium.ShowGeometryInstanceAttribute.toValue(false, fillAttributes.show);
        },

        onClose: function () {
            var view = this;

            // If there is already a billboard for this view, remove it
            if (!_.isUndefined(view.billboard)) {
                view.geoController.billboardCollection.remove(view.billboard);
            }
            if (!_.isUndefined(view.polygons)) {
                _.each(view.polygons, function (polygonPrimitive) {
                    view.geoController.scene.getPrimitives().remove(polygonPrimitive);
                });
            }

            this.stopListening();
        }
    });

    Views.ResultsView = Marionette.CollectionView.extend({
        itemView: Backbone.View,
        initialize: function (options) {
            this.geoController = options.geoController;
        },

        buildItemView: function (item, ItemViewType, itemViewOptions) {
            var metacard = item.get('metacard'),
                geometry = metacard.get('geometry'),
                ItemView;
            if (!geometry) {
                return new ItemViewType();
            }
            // build the final list of options for the item view type.
            var options = _.extend({model: metacard, geoController: this.geoController}, itemViewOptions);

            if (geometry.isPoint()) {
                ItemView = Views.PointView;
            }
            else if (geometry.isPolygon()) {
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