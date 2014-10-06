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
/*global define, parseFloat*/
/*jshint newcap:false */

define(['underscore',
        'marionette',
        'cesium',
        'q',
        'wreqr',
        'properties',
        'js/view/cesium.metacard',
        'jquery'
    ], function (_, Marionette, Cesium, Q, wreqr, properties, CesiumMetacard, $) {
        "use strict";

        var imageryProviderTypes = {
            OSM: Cesium.OpenStreetMapImageryProvider,
            AGM: Cesium.ArcGisMapServerImageryProvider,
            BM: Cesium.BingMapsImageryProvider,
            WMS: Cesium.WebMapServiceImageryProvider,
            WMT: Cesium.WebMapTileServiceImageryProvider,
            TMS: Cesium.TileMapServiceImageryProvider,
            GE: Cesium.GoogleEarthImageryProvider,
            CT: Cesium.CesiumTerrainProvider,
            AGS: Cesium.ArcGisImageServerTerrainProvider,
            VRW: Cesium.VRTheWorldTerrainProvider
        };

        var Controller = Marionette.Controller.extend({
            initialize: function () {
                this.mapViewer = this.createMap('cesiumContainer');
                this.scene = this.mapViewer.scene;
                this.ellipsoid = this.mapViewer.scene.globe.ellipsoid;
                this.handler = new Cesium.ScreenSpaceEventHandler(this.scene.canvas);
                this.setupEvents();
                this.preloadBillboards();

                this.listenTo(wreqr.vent, 'search:mapshow', this.flyToLocation);
                this.listenTo(wreqr.vent, 'search:start', this.clearResults);
                this.listenTo(wreqr.vent, 'map:results', this.newResults);
                this.listenTo(wreqr.vent, 'map:clear', this.clear);

                if (wreqr.reqres.hasHandler('search:results')) {
                    this.newResults(wreqr.reqres.request('search:results'));
                }
            },
            createMap: function (mapDivId) {
                var viewer, options;
                options = {
                    sceneMode: Cesium.SceneMode.SCENE3D,
                    animation: false,
                    fullscreenButton: false,
                    timeline: false,
                    geocoder: false,
                    homeButton: true,
                    sceneModePicker: true,

                    // Hide the base layer picker
                    baseLayerPicker: false
                };

                if(properties.imageryProviders) {
                    _.each(properties.imageryProviders, function(item) {
                        var imageryProvider = $.parseJSON(item);
                        var key = Object.keys(imageryProvider)[0];
                        var type = imageryProviderTypes[key];
                        var initObj = imageryProvider[key];
                        if (!options.imageryProvider) {
                            options.imageryProvider = new type(initObj);
                            viewer = new Cesium.Viewer(mapDivId, options);
                        } else {
                            var layer = viewer.scene.imageryLayers.addImageryProvider(new type(initObj));
                            if(initObj.alpha) {
                                layer.alpha = parseFloat(initObj.alpha, 10);
                            } else {
                                layer.alpha = 0.5;
                            }

                        }
                    });

                    var terrainProvider = $.parseJSON(properties.terrainProvider);
                    var key = Object.keys(terrainProvider)[0];
                    var type = imageryProviderTypes[key];
                    var initObj = terrainProvider[key];
                    if (viewer) {
                        viewer.scene.terrainProvider = new type(initObj);
                    }
                }

                return viewer;
            },

            billboards: [
                'images/default.png',
                'images/default-selected.png'
                // add extra here if you want to switch
            ],
            // since we only need a single global collection of these billboards, we can prepare them here, if it
            // gets more complex, this should be pushed to individual collection views or views.
            preloadBillboards: function () {
                var controller = this;
                controller.billboardCollection = new Cesium.BillboardCollection();
                // cesium loads the images asynchronously, so we need to use promises
                this.billboardPromise = Q.all(_.map(this.billboards, function (billboard) {
                        return Q(Cesium.loadImage(billboard));
                    }))
                    .then(function (images) {
                        controller.billboardCollection.textureAtlas = new Cesium.TextureAtlas({
                            scene: controller.scene,
                            images: images
                        });
                        controller.scene.primitives.add(controller.billboardCollection);
                    });

            },

            setupEvents: function () {
                var controller = this;
                //Left button events
                controller.handler.setInputAction(function (event) {
                    controller.trigger('click:left', controller.pickObject(event));

                }, Cesium.ScreenSpaceEventType.LEFT_CLICK);

                controller.handler.setInputAction(function (event) {
                    controller.trigger('doubleclick:left', controller.pickObject(event));

                }, Cesium.ScreenSpaceEventType.LEFT_DOUBLE_CLICK);
                //Right button events
                controller.handler.setInputAction(function (event) {
                    //Tack on the object if one was clicked
                    controller.trigger('click:right', controller.pickObject(event));

                }, Cesium.ScreenSpaceEventType.RIGHT_CLICK);
            },

            pickObject: function (event) {
                var controller = this,
                    //Add the offset created by the timeline
                    position = new Cesium.Cartesian2(event.position.x, event.position.y),
                    selectedObject = controller.scene.pick(position);

                if (selectedObject) {
                    selectedObject = selectedObject.primitive;
                }

                return {
                    position: event.position,
                    object: selectedObject
                };
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

                var cartPoints = _.map(regionPoints, function (point) {
                    return Cesium.Cartographic.fromDegrees(point.longitude, point.latitude, point.altitude);
                });

                var rectangle = Cesium.Rectangle.fromCartographicArray(cartPoints);
                return rectangle;
            },
            flyToLocation: function (model) {
                var geometry = model.get('geometry');
                this.flyToGeometry(geometry);
            },
            
            flyToGeometry: function (geometry) {
                var rectangle, cartArray;

                cartArray = _.map(geometry.getAllPoints(), function (coordinate) {
                    return Cesium.Cartographic.fromDegrees(coordinate[0], coordinate[1], properties.defaultFlytoHeight);
                });

                rectangle = Cesium.Rectangle.fromCartographicArray(cartArray);
                this.mapViewer.scene.camera.flyToRectangle({
                    destination: this.expandRectangle(rectangle)
                });
            },

            flyToRectangle: function (rectangle) {
                if (rectangle.north === rectangle.south && rectangle.east === rectangle.west) {
                    this.mapViewer.scene.camera.flyTo({
                        destination: Cesium.Cartesian3.fromRadians(rectangle.west, rectangle.north, properties.defaultFlytoHeight)
                    });
                } else {
                    this.mapViewer.scene.camera.flyToRectangle({
                        destination: this.expandRectangle(rectangle)
                    });
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
                this.mapViews = new CesiumMetacard.ResultsView({
                    collection: results,
                    geoController: this
                }).render();
            },

            clear: function () {
                this.clearResults();
                this.billboardCollection.removeAll();
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