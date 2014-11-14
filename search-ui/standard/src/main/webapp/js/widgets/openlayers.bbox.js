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
        'wreqr',
        'maptype',
        './notification.view'
    ],
    function (Marionette, Backbone, ol, _, wreqr, maptype, NotificationView) {
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

                var northWest = ol.proj.transform([extent[0], extent[3]], 'EPSG:3857', 'EPSG:4326');
                var southEast = ol.proj.transform([extent[2], extent[1]], 'EPSG:3857', 'EPSG:4326');

                this.model.set({
                    north: northWest[1],
                    south: southEast[1],
                    west: northWest[0],
                    east: southEast[0]
                });
            },

            modelToRectangle: function (model) {
                var northWest = ol.proj.transform([model.get('west'), model.get('north')], 'EPSG:4326', 'EPSG:3857');
                var northEast = ol.proj.transform([model.get('east'), model.get('north')], 'EPSG:4326', 'EPSG:3857');
                var southWest = ol.proj.transform([model.get('west'), model.get('south')], 'EPSG:4326', 'EPSG:3857');
                var southEast = ol.proj.transform([model.get('east'), model.get('south')], 'EPSG:4326', 'EPSG:3857');
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

                var iconStyle = new ol.style.Style({
                    stroke: new ol.style.Stroke({color: '#914500', width: 3})
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
                this.listenTo(this.model, 'change:north change:south change:east change:west', this.updateGeometry);

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
            },
            showBox: function(model) {
                if (this.enabled) {
                    var bboxModel = model || new Draw.BboxModel(),
                        view = new Draw.BboxView(
                            {
                                map: this.map,
                                model: bboxModel
                            });

                    if (this.view) {
                        this.view.destroyPrimitive();
                        this.view.stop();

                    }
                    view.updatePrimitive(model);
                    this.view = view;

                    return bboxModel;
                }
            },
            draw: function (model) {
                if (this.enabled) {
                    var bboxModel = model || new Draw.BboxModel(),
                        view = new Draw.BboxView(
                            {
                                map: this.map,
                                model: bboxModel
                            });

                    if (this.view) {
                        this.view.destroyPrimitive();
                        this.view.stop();

                    }
                    view.start();
                    this.view = view;
                    this.notificationView = new NotificationView({
                        el: this.notificationEl
                    }).render();
                    bboxModel.trigger('BeginExtent');
                    this.listenToOnce(bboxModel, 'EndExtent', function () {
                        this.notificationView.close();
                    });

                    return bboxModel;
                }
            },
            stop: function () {
                if (this.enabled && this.view) {
                    this.view.stop();
                    if(this.notificationView) {
                        this.notificationView.close();
                    }
                }
            },
            destroy: function () {
                if (this.enabled && this.view) {
                    this.view.stop();
                    this.view.destroyPrimitive();
                    this.view = undefined;
                    if(this.notificationView) {
                        this.notificationView.close();
                    }
                }
            }
        });

        return Draw;
    });