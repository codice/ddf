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
        'cesium',
        'q',
        'wreqr',
        'properties',
        'js/view/cesium.metacard'
    ], function (_, Marionette, Cesium, Q, wreqr, properties, CesiumMetacard) {
        "use strict";

        var Controller = Marionette.Controller.extend({
            initialize: function () {
                this.mapViewer = this.createMap('cesiumContainer');
                this.scene = this.mapViewer.scene;
                this.ellipsoid = this.mapViewer.centralBody.getEllipsoid();
                this.handler = new Cesium.ScreenSpaceEventHandler(this.scene.getCanvas());
                this.setupEvents();
                this.preloadBillboards();

                wreqr.vent.on('map:show', _.bind(this.flyToLocation, this));
                wreqr.vent.on('search:start', _.bind(this.clearResults, this));
                wreqr.vent.on('search:results', _.bind(this.newResults, this));
                wreqr.vent.on('search:clear', _.bind(this.clear, this));

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

                if(properties.wmsServer) {
                    options.imageryProvider = new Cesium.WebMapServiceImageryProvider({
                        url: properties.targetUrl,
                        layers : properties.layers,
                        parameters : {
                            format : properties.format
                        }
                    });
                }
                else {
                    options.imageryProvider = new Cesium.BingMapsImageryProvider({
                                                  url : 'https://dev.virtualearth.net',
                                                  mapStyle : Cesium.BingMapsStyle.AERIAL_WITH_LABELS
                                              });
                }

                viewer = new Cesium.Viewer(mapDivId, options);

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
                        controller.billboardCollection.setTextureAtlas(
                            controller.scene.getContext().createTextureAtlas({
                                images: images
                            })
                        );
                        controller.scene.getPrimitives().add(controller.billboardCollection);
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

            expandExtent: function (extent) {
                var scalingFactor = 0.25;

                var widthGap = Math.abs(extent.east) - Math.abs(extent.west);
                var heightGap = Math.abs(extent.north) - Math.abs(extent.south);
                
                //ensure extent has some size
                if(widthGap === 0) {
                        widthGap = 1;
                }
                if(heightGap === 0) {
                        heightGap = 1;
                }
                
                extent.east = extent.east + Math.abs(scalingFactor * widthGap);
                extent.north = extent.north + Math.abs(scalingFactor * heightGap);
                extent.south = extent.south - Math.abs(scalingFactor * heightGap);
                extent.west = extent.west - Math.abs(scalingFactor * widthGap);

                return extent;
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

                var extent = Cesium.Extent.fromCartographicArray(cartPoints);
                return extent;
            },
            flyToLocation: function (model) {
                var geometry = model.get('geometry');
                this.flyToGeometry(geometry);
            },
            
            flyToGeometry: function (geometry) {
                var flight, extent, cartArray;

                cartArray = _.map(geometry.getAllPoints(), function (coordinate) {
                    return Cesium.Cartographic.fromDegrees(coordinate[0], coordinate[1], properties.defaultFlytoHeight);
                });

                extent = Cesium.Extent.fromCartographicArray(cartArray);
                flight = Cesium.CameraFlightPath.createAnimationExtent(this.mapViewer.scene, {
                    destination: this.expandExtent(extent)
                });

                this.mapViewer.scene.getAnimations().add(flight);
            },

            flyToExtent: function (extent) {
                var flight;
                if (extent.north === extent.south && extent.east === extent.west) {
                    var destination = {height: properties.defaultFlytoHeight, latitude: extent.north, longitude: extent.west};
                    flight = Cesium.CameraFlightPath.createAnimationCartographic(this.mapViewer.scene, {
                        destination: destination
                    });
                }
                else {
                    flight = Cesium.CameraFlightPath.createAnimationExtent(this.mapViewer.scene, {
                        destination: this.expandExtent(extent)
                    });
                }

                this.mapViewer.scene.getAnimations().add(flight);
            },

            flyToCenterPoint: function (results) {
                var extent = this.getResultCenterPoint(results);
                if (extent) {
                    this.flyToExtent(extent);
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