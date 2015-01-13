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
/*jshint newcap:false */

define(['underscore',
        'marionette',
        'openlayers',
        'q',
        'wreqr',
        'properties',
        'js/view/openlayers.metacard',
        'js/model/Metacard',
        'jquery',
        'js/view/openlayers.geocoder'
    ], function (_, Marionette, ol, Q, wreqr, properties, OpenlayersMetacard, Metacard, $, geocoder) {
        "use strict";

        var imageryProviderTypes = {
            OSM: ol.source.OSM,
            BM: ol.source.BingMaps,
            WMS: ol.source.TileWMS,
            MQ: ol.source.MapQuest
        };

        var Controller = Marionette.Controller.extend({
            initialize: function () {
                if (properties.gazetteer) {
                    this.geoCoder = new geocoder.View({el: $('#mapTools')});
                }
                this.mapViewer = this.createMap('cesiumContainer');
                this.setupEvents();

                this.listenTo(wreqr.vent, 'search:mapshow', this.flyToLocation);
                this.listenTo(wreqr.vent, 'search:maprectanglefly', this.flyToRectangle);
                this.listenTo(wreqr.vent, 'search:start', this.clearResults);
                this.listenTo(wreqr.vent, 'map:results', this.newResults);
                this.listenTo(wreqr.vent, 'map:clear', this.clear);

                if (wreqr.reqres.hasHandler('search:results')) {
                    this.newResults(wreqr.reqres.request('search:results'));
                }
            },
            createMap: function (mapDivId) {
                var map;
                if (properties.imageryProviders) {
                    var layers = [];
                    _.each(properties.imageryProviders, function (imageryProvider) {
                        if (imageryProvider) {
                            var type = imageryProviderTypes[imageryProvider.type];
                            var initObj = _.omit(imageryProvider, 'type');

                            if (imageryProvider.type === 'OSM') {
                                if (initObj.url && initObj.url.indexOf('/{z}/{x}/{y}') === -1) {
                                    initObj.url = initObj.url + '/{z}/{x}/{y}.png';
                                }
                            } else if (imageryProvider.type === 'BM') {
                                if (!initObj.imagerySet) {
                                    initObj.imagerySet = initObj.url;
                                }
                            } else if (imageryProvider.type === 'WMS') {
                                if (!initObj.params) {
                                    initObj.params = {
                                        LAYERS: initObj.layers
                                    };
                                    _.extend(initObj.params, initObj.parameters);
                                }
                            }
                            layers.push(new ol.layer.Tile({
                                visible: true,
                                preload: Infinity,
                                opacity: initObj.alpha,
                                source: new type(initObj)
                            }));
                        }
                    });

                    map = new ol.Map({
                        layers: layers,
                        target: mapDivId,
                        view: new ol.View({
                            projection: ol.proj.get(properties.projection),
                            center: ol.proj.transform([0, 0], 'EPSG:4326', properties.projection),
                            zoom: 3
                        })
                    });
                }

                if (this.geoCoder) {
                    this.geoCoder.render();
                }

                return map;
            },

            setupEvents: function () {
                var controller = this;
                this.mapViewer.on('click', function(event) {
                    var feature = controller.mapViewer.forEachFeatureAtPixel(event.pixel,
                        function(feature) {
                            return feature;
                        });
                    if (feature) {
                        controller.trigger('click:left', feature);
                    }
                });
            },

            expandRectangle: function (rectangle) {
                var scalingFactor = 0.25;

                var widthGap = Math.abs(rectangle.east) - Math.abs(rectangle.west);
                var heightGap = Math.abs(rectangle.north) - Math.abs(rectangle.south);

                //ensure rectangle has some size
                if(widthGap === 0) {
                    widthGap = 1;
                }
                if(heightGap === 0) {
                    heightGap = 1;
                }

                rectangle.east = rectangle.east + Math.abs(scalingFactor * widthGap);
                rectangle.north = rectangle.north + Math.abs(scalingFactor * heightGap);
                rectangle.south = rectangle.south - Math.abs(scalingFactor * heightGap);
                rectangle.west = rectangle.west - Math.abs(scalingFactor * widthGap);

                return rectangle;
            },

            getResultCenterPoint: function (result) {
                var regionPoints = [],
                    resultQuad,
                    quadrantCounts = [
                        {
                            quad: 'one',
                            count: 0
                        },
                        {
                            quad: 'two',
                            count: 0
                        },
                        {
                            quad: 'three',
                            count: 0
                        },
                        {
                            quad: 'four',
                            count: 0
                        }
                    ];

                result.each(function (item) {
                    if (item.get("metacard").get("geometry")) {
                        var point = item.get("metacard").get("geometry").getPoint();
                        if (point.longitude > 0 && point.latitude > 0) {
                            quadrantCounts[0].count++;
                        } else if (point.longitude < 0 && point.latitude > 0) {
                            quadrantCounts[1].count++;
                        } else if (point.longitude < 0 && point.latitude < 0) {
                            quadrantCounts[2].count++;
                        } else {
                            quadrantCounts[3].count++;
                        }
                    }
                });

                quadrantCounts = _.sortBy(quadrantCounts, 'count');

                quadrantCounts.reverse();
                resultQuad = quadrantCounts[0].quad;

                result.each(function (item) {
                    if (item.get("metacard").get("geometry")) {
                        var newPoint = item.get("metacard").get("geometry").getPoint(),
                            isInRegion = false;

                        if (newPoint.longitude >= 0 && newPoint.latitude >= 0 && resultQuad === "one") {
                            isInRegion = true;
                        } else if (newPoint.longitude <= 0 && newPoint.latitude >= 0 && resultQuad === "two") {
                            isInRegion = true;
                        } else if (newPoint.longitude <= 0 && newPoint.latitude <= 0 && resultQuad === "three") {
                            isInRegion = true;
                        } else if (newPoint.longitude >= 0 && newPoint.latitude <= 0 && resultQuad === "four") {
                            isInRegion = true;
                        }

                        if (isInRegion) {
                            regionPoints.push(newPoint);
                        }
                    }
                });

                if (regionPoints.length === 0) {
                    return null;
                }

                var rectangle = {};
                var minLon = Number.MAX_VALUE;
                var maxLon = -Number.MAX_VALUE;
                var minLat = Number.MAX_VALUE;
                var maxLat = -Number.MAX_VALUE;

                for ( var i = 0, len = regionPoints.length; i < len; i++) {
                    var position = regionPoints[i];
                    minLon = Math.min(minLon, position.longitude);
                    maxLon = Math.max(maxLon, position.longitude);
                    minLat = Math.min(minLat, position.latitude);
                    maxLat = Math.max(maxLat, position.latitude);
                }

                rectangle.west = minLon;
                rectangle.south = minLat;
                rectangle.east = maxLon;
                rectangle.north = maxLat;
                return rectangle;
            },
            flyToLocation: function (model) {
                var geometry = model.get('geometry');
                this.flyToGeometry(geometry);
            },

            flyToGeometry: function (geometry) {
                var point = geometry.getPoint();
                var location = ol.proj.transform([point.longitude, point.latitude], 'EPSG:4326', properties.projection);
                var pan = ol.animation.pan({
                    duration: 2000,
                    source: /** @type {ol.Coordinate} */ (this.mapViewer.getView().getCenter())
                });

                this.mapViewer.beforeRender(pan);
                this.mapViewer.getView().setCenter(location);
            },

            flyToRectangle: function (rectangle) {
                if (rectangle.north === rectangle.south && rectangle.east === rectangle.west) {
                    this.flyToGeometry(new Metacard.Geometry({
                        type: "Point",
                        coordinates: [rectangle.west, rectangle.north]
                    }));
                } else if (rectangle.north && rectangle.south && rectangle.east && rectangle.west) {
                    var northWest = ol.proj.transform([rectangle.west, rectangle.north], 'EPSG:4326', properties.projection);
                    var southEast = ol.proj.transform([rectangle.east, rectangle.south], 'EPSG:4326', properties.projection);
                    var coords = [];
                    coords.push(northWest[0]);
                    coords.push(southEast[1]);
                    coords.push(southEast[0]);
                    coords.push(northWest[1]);
                    var pan1 = ol.animation.pan({
                        duration: 2000,
                        source: /** @type {ol.Coordinate} */ (this.mapViewer.getView().getCenter())
                    });
                    this.mapViewer.beforeRender(pan1);
                    this.mapViewer.getView().fitExtent(coords, this.mapViewer.getSize());
                } else {
                    var pan2 = ol.animation.pan({
                        duration: 2000,
                        source: /** @type {ol.Coordinate} */ (this.mapViewer.getView().getCenter())
                    });
                    this.mapViewer.beforeRender(pan2);
                    var extent = rectangle.getExtent();
                    this.mapViewer.getView().fitExtent(extent);
                }
            },

            flyToCenterPoint: function (results) {
                var rectangle = this.getResultCenterPoint(results);
                if (rectangle) {
                    this.flyToRectangle(rectangle);
                }
            },

            newResults: function (result, zoomOnResults) {
                this.showResults(result.get('results'));
                if (zoomOnResults) {
                    this.flyToCenterPoint(result.get('results'));
                }
            },

            showResults: function (results) {
                if (this.mapViews) {
                    this.mapViews.close();
                }
                this.mapViews = new OpenlayersMetacard.ResultsView({
                    collection: results,
                    geoController: this
                }).render();
            },

            clear: function () {
                this.clearResults();
            },

            clearResults: function () {
                if (this.mapViews) {
                    this.mapViews.close();
                }
            }

        });

        return Controller;
    }
);