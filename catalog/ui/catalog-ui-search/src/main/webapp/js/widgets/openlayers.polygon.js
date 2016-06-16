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
        'openlayers',
        'underscore',
        'properties',
        'wreqr',
        'maptype',
        './notification.view',
        'js/store'
    ],
    function (Marionette, Backbone, ol, _, properties, wreqr, maptype, NotificationView, store) {
        "use strict";

        var Draw = {};

        Draw.PolygonModel = Backbone.Model.extend({
            defaults: {
                north: undefined,
                east: undefined,
                west: undefined,
                south: undefined
            }
        });
        Draw.PolygonView = Backbone.View.extend({
            initialize: function (options) {
                this.map = options.map;
            },
            setModelFromGeometry: function (geometry) {

                var coords = geometry.getCoordinates();

                var coordinates = [];

                _.each(coords, function (item) {
                    _.each(item, function (point) {
                        coordinates.push(ol.proj.transform([point[0], point[1]], properties.projection, 'EPSG:4326'));
                    });
                });

                this.model.set({
                    polygon: coordinates
                });
            },

            modelToPolygon: function (model) {
                var polygon = model.get('polygon');
                var coords = [];
                var setArr = _.uniq(polygon);
                if(setArr.length < 3){
                    return;
                }
                _.each(setArr, function (item) {
                    coords.push(ol.proj.transform([item[0], item[1]], 'EPSG:4326', properties.projection));
                });

                var rectangle = new ol.geom.LineString(coords);
                return rectangle;
            },

            updatePrimitive: function (model) {
                var polygon = this.modelToPolygon(model);
                // make sure the current model has width and height before drawing
                if (polygon && !_.isUndefined(polygon)) {
                    this.drawBorderedPolygon(polygon);
                }
            },

            updateGeometry: function (model) {
                var rectangle = this.modelToPolygon(model);
                if (rectangle) {
                    this.drawBorderedPolygon(rectangle);
                }
            },

            drawBorderedPolygon: function (rectangle) {

                if (!rectangle) {
                    // handles case where model changes to empty vars and we don't want to draw anymore
                    return;
                }

                if(this.vectorLayer) {
                    this.map.removeLayer(this.vectorLayer);
                }

                this.billboard = new ol.Feature({
                    geometry: rectangle
                });

                var color = this.model.get('color');

                var iconStyle = new ol.style.Style({
                    stroke: new ol.style.Stroke({color: color ? color : '#914500', width: 3})
                });
                this.billboard.setStyle(iconStyle);

                var vectorSource = new ol.source.Vector({
                    features: [this.billboard]
                });

                var vectorLayer = new ol.layer.Vector({
                    source: vectorSource
                });

                this.vectorLayer = vectorLayer;
                this.map.addLayer(vectorLayer);
            },

            handleRegionStop: function (sketchFeature) {
                this.setModelFromGeometry(sketchFeature.feature.getGeometry());
                this.drawBorderedPolygon(sketchFeature.feature.getGeometry());
                this.listenTo(this.model, 'change:polygon', this.updateGeometry);

                this.model.trigger("EndExtent", this.model);
            },
            start: function () {
                var that = this;

                this.primitive = new ol.interaction.Draw({
                    type: 'Polygon',
                    style: new ol.style.Style({
                        stroke: new ol.style.Stroke({
                            color: [0,0,255,1]
                        })
                    })
                });

                this.map.addInteraction(this.primitive);
                this.primitive.on('drawend', function(sketchFeature){
                    that.handleRegionStop(sketchFeature);
                    that.map.removeInteraction(that.primitive);
                });
            },


            stop: function () {
                this.stopListening();
            },


            destroyPrimitive: function () {
                if (this.primitive) {
                    this.map.removeInteraction(this.primitive);
                }
                if (this.vectorLayer) {
                    this.map.removeLayer(this.vectorLayer);
                }
            }

        });

        Draw.Controller = Marionette.Controller.extend({
            enabled: maptype.is2d(),
            initialize: function (options) {
                this.map = options.map;
                this.notificationEl = options.notificationEl;

                this.listenTo(wreqr.vent, 'search:polydisplay', this.showBox);
                this.listenTo(wreqr.vent, 'search:drawpoly', this.draw);
                this.listenTo(wreqr.vent, 'search:drawstop', this.stop);
                this.listenTo(wreqr.vent, 'search:drawend', this.destroy);
                this.listenTo(wreqr.vent, 'search:destroyAllDraw', this.destroyAll);
                this.listenTo(store.get('content'), 'change:query', this.destroyAll);
            },
            views: [],
            destroyAll: function(){
                for (var i = this.views.length - 1; i>=0 ; i-=1){
                    this.destroyView(this.views[i]);
                }
            },
            getViewForModel: function(model){
                return this.views.filter(function(view){
                    return view.model === model;
                })[0];
            },
            removeViewForModel: function(model){
                var view = this.getViewForModel(model);
                if (view){
                    this.views.splice(this.views.indexOf(view), 1);
                }
            },
            removeView: function(view){
                this.views.splice(this.views.indexOf(view), 1);
            },
            addView: function(view){
                this.views.push(view);
            },
            showBox: function(model) {
                if (this.enabled) {
                    var polygonModel = model || new Draw.PolygonModel();
                        /*view = new Draw.PolygonView(
                            {
                                map: this.map,
                                model: polygonModel
                            });*/

                    var existingView = this.getViewForModel(model);
                    if (existingView) {
                        existingView.stop();
                        existingView.destroyPrimitive();
                        existingView.updatePrimitive(model);
                    } else {
                        var view = new Draw.PolygonView(
                            {
                                map: this.map,
                                model: polygonModel
                            });
                        view.updatePrimitive(model);
                        this.addView(view);
                    }

                    return polygonModel;
                }
            },
            draw: function (model) {
                if (this.enabled) {
                    var polygonModel = model || new Draw.PolygonModel();
                    var view = new Draw.PolygonView(
                            {
                                map: this.map,
                                model: polygonModel
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
                        el: this.notificationEl
                    }).render();
                    polygonModel.trigger('BeginExtent');
                    this.listenToOnce(polygonModel, 'EndExtent', function () {
                        this.notificationView.destroy();
                    });

                    return polygonModel;
                }
            },
            stop: function (model) {
                var view = this.getViewForModel(model);
                if (view) {
                    view.stop();
                    if(this.notificationView) {
                        this.notificationView.destroy();
                    }
                }
            },
            destroyView: function(view){
                view.stop();
                view.destroyPrimitive();
                this.removeView(view);
            },
            destroy: function (model) {
                var view = this.getViewForModel(model);
                if (view) {
                    view.stop();
                    view.destroyPrimitive();
                    this.removeView(view);
                    if(this.notificationView) {
                        this.notificationView.destroy();
                    }
                }
            }
        });

        return Draw;
    });