/*global define*/
/*jshint newcap:false */
define(function (require) {
    "use strict";
    var Marionette = require('marionette'),
        Cesium = require('cesium'),
        Q = require('q'),
        properties = require('properties'),
        _ = require('underscore'),
        webgl = require('webglcheck');

    var Controller = Marionette.Controller.extend({
        enabled: webgl.isAvailable(),
        initialize: function () {
            if (this.enabled) {
                this.mapViewer = this.createMap('cesiumContainer');
                this.scene = this.mapViewer.scene;
                this.ellipsoid = this.mapViewer.centralBody.getEllipsoid();
                this.handler = new Cesium.ScreenSpaceEventHandler(this.scene.getCanvas());
                this.setupEvents();
                this.preloadBillboards();
            }
        },
        createMap: function (mapDivId) {
            var viewer, options;
            options = {
                // Start in Columbus Viewer
                // sceneMode : Cesium.SceneMode.COLUMBUS_VIEW,
                sceneMode: Cesium.SceneMode.SCENE3D,
                animation: false,
                fullscreenButton: false,
                timeline: false,
                geocoder: false,
                homeButton: true,
                sceneModePicker: true,

                // Hide the base layer picker for OpenStreetMaps
                baseLayerPicker: false
                // Use OpenStreetMaps

            };

            if(properties.wmsServer && properties.wmsServer !== "") {
                options.imageryProvider = new Cesium.WebMapServiceImageryProvider({
                    url: '/search/standard/proxy',
                    layers : properties.layers,
                    parameters : {
                        format : properties.format
                    }
                });
            }
            else {
                options.imageryProvider = new Cesium.BingMapsImageryProvider({
                                              url : 'http://dev.virtualearth.net',
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
            // cesium loads the images asynchronously, so we need to use promises
            this.billboardPromise = Q.all(_.map(this.billboards, function (billboard) {
                    return Q(Cesium.loadImage(billboard));
                }))
                .then(function (images) {
                    controller.billboardCollection = new Cesium.BillboardCollection();
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
            extent.east = extent.east + Math.abs(scalingFactor * widthGap);
            extent.north = extent.north + Math.abs(scalingFactor * heightGap);
            extent.south = extent.south - Math.abs(scalingFactor * heightGap);
            extent.west = extent.west - Math.abs(scalingFactor * widthGap);

            return extent;
        },

        flyToLocation: function (model) {
            if (this.enabled) {
                console.log('flying to model dest:  ', model.toJSON());
                var destination, flight, extent;

                //polygon
                var geometry = model.get('geometry');
                if (geometry.isPolygon()) {

                    var cartArray = _.map(geometry.get("coordinates")[0], function (coordinate) {
                        return Cesium.Cartographic.fromDegrees(coordinate[0], coordinate[1], properties.defaultFlytoHeight);
                    });

                    extent = Cesium.Extent.fromCartographicArray(cartArray);
                    flight = Cesium.CameraFlightPath.createAnimationExtent(this.mapViewer.scene, {
                        destination: this.expandExtent(extent)
                    });
                }
                else {
                    destination = Cesium.Cartographic.fromDegrees(geometry.get("coordinates")[0], geometry.get("coordinates")[1], geometry.get("coordinates")[2] ? geometry.get("coordinates")[2] : properties.defaultFlytoHeight);
                    flight = Cesium.CameraFlightPath.createAnimationCartographic(this.mapViewer.scene, {
                        destination: destination
                    });
                }


                this.mapViewer.scene.getAnimations().add(flight);
            }
        },

        flyToExtent: function (extent) {
            if (this.enabled) {
                var flight;
                if(extent.north === extent.south && extent.east === extent.west) {
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
            }
        }

    });

    return Controller;

});