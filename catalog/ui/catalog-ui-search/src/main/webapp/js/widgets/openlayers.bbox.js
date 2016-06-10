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

        Draw.BboxModel = Backbone.Model.extend({
            defaults: {
                north: undefined,
                east: undefined,
                west: undefined,
                south: undefined
            }
        });
        Draw.BboxView = Backbone.View.extend({
            initialize: function (options) {
                this.map = options.map;
            },
            setModelFromGeometry: function (geometry) {

                var extent = geometry.getExtent();

                var northWest = ol.proj.transform([extent[0], extent[3]], properties.projection, 'EPSG:4326');
                var southEast = ol.proj.transform([extent[2], extent[1]], properties.projection, 'EPSG:4326');

                this.model.set({
                    north: northWest[1],
                    south: southEast[1],
                    west: northWest[0],
                    east: southEast[0]
                });
            },

            modelToRectangle: function (model) {

                //ensure that the values are numeric
                //so that the openlayer projections
                //do not fail
                var north  = parseFloat(model.get('mapNorth'));
                var south = parseFloat(model.get('mapSouth'));
                var east = parseFloat(model.get('mapEast'));
                var west = parseFloat(model.get('mapWest'));

                var northWest = ol.proj.transform([west,north], 'EPSG:4326', properties.projection);
                var northEast = ol.proj.transform([east,north], 'EPSG:4326', properties.projection);
                var southWest = ol.proj.transform([west,south], 'EPSG:4326', properties.projection);
                var southEast = ol.proj.transform([east,south], 'EPSG:4326', properties.projection);

                var coords = [];
                coords.push(northWest);
                coords.push(northEast);
                coords.push(southEast);
                coords.push(southWest);
                coords.push(northWest);
                var rectangle = new ol.geom.LineString(coords);
                return rectangle;
            },

            updatePrimitive: function (model) {
                var rectangle = this.modelToRectangle(model);
                // make sure the current model has width and height before drawing
                if (rectangle && !_.isUndefined(rectangle) && (model.get('north') !== model.get('south') && model.get('east') !== model.get('west'))) {
                    this.drawBorderedRectangle(rectangle);
                    //only call this if the mouse button isn't pressed, if we try to draw the border while someone is dragging
                    //the filled in shape won't show up
                    if (!this.buttonPressed) {
                        this.drawBorderedRectangle(rectangle);
                    }
                }
            },

            updateGeometry: function (model) {
                var rectangle = this.modelToRectangle(model);
                if (rectangle) {
                    this.drawBorderedRectangle(rectangle);
                }
            },

            drawBorderedRectangle: function (rectangle) {

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

            handleRegionStop: function () {
                this.setModelFromGeometry(this.primitive.getGeometry());
                this.updateGeometry(this.model);
                this.listenTo(this.model, 'change:mapNorth change:mapSouth change:mapEast change:mapWest', this.updateGeometry);

                this.model.trigger("EndExtent", this.model);
            },
            start: function () {
                var that = this;
                this.primitive = new ol.interaction.DragBox({
                    condition: ol.events.condition.always,
                    style: new ol.style.Style({
                        stroke: new ol.style.Stroke({
                            color: [0,0,255,1]
                        })
                    })
                });

                this.map.addInteraction(this.primitive);
                this.primitive.on('boxend', function(){
                    that.handleRegionStop();
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

                this.listenTo(wreqr.vent, 'search:bboxdisplay', this.showBox);
                this.listenTo(wreqr.vent, 'search:drawbbox', this.draw);
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
                console.log(this.views);
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
                    var bboxModel = model || new Draw.BboxModel();
                        /*view = new Draw.BboxView(
                            {
                                map: this.map,
                                model: bboxModel
                            });*/

                    var existingView = this.getViewForModel(model);
                    if (existingView) {
                        existingView.stop();
                        existingView.destroyPrimitive();
                    }
                    existingView.updatePrimitive(model);

                    return bboxModel;
                }
            },
            draw: function (model) {
                if (this.enabled) {
                    var bboxModel = model || new Draw.BboxModel();
                    var view = new Draw.BboxView(
                            {
                                map: this.map,
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
                        el: this.notificationEl
                    }).render();
                    bboxModel.trigger('BeginExtent');
                    this.listenToOnce(bboxModel, 'EndExtent', function () {
                        this.notificationView.destroy();
                    });

                    return bboxModel;
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