/*global define*/

define(function (require) {
    "use strict";
    var $ = require('jquery'),
        Backbone = require('backbone'),
        _ = require('underscore'),
        SceneMode = require('cesium/Scene/SceneMode'),
        Cartographic = require('cesium/Core/Cartographic'),
        OpenStreetMapImageryProvider = require('cesium/Scene/OpenStreetMapImageryProvider'),
        LabelCollection  = require('cesium/Scene/LabelCollection'),
        GeoJsonDataSource = require('cesium/DynamicScene/GeoJsonDataSource'),
        ScreenSpaceEventHandler = require('cesium/Core/ScreenSpaceEventHandler'),
        CesiumMath = require('cesium/Core/Math'),
        ScreenSpaceEventType = require('cesium/Core/ScreenSpaceEventType'),
        CameraFlightPath = require('cesium/Scene/CameraFlightPath'),
        Extent = require('cesium/Core/Extent'),

        CesiumViewer = require('cesium/Widgets/Viewer/Viewer');

    var MapView = Backbone.View.extend({
        initialize: function (options) {
            _.bindAll(this, "render", "createResultsOnMap", "addAdditionalLayers", "flyToLocation");
        },
        render: function () {
            this.mapViewer = this.createMap('geospatial-main');
            this.createPicker(this.mapViewer.scene, this.mapViewer.centralBody.getEllipsoid());
            this.addAdditionalLayers(this.mapViewer.centralBody.getImageryLayers());
        },
        createResultsOnMap: function (options) {
            var startAt, finishAt, i, metacardResult, metacard, jsonDataSource;
            this.model = options;
//        if(options && options.result)
//        {
//            this.model = options.result;
//        }
            if (options && options.startIndex) {
                startAt = options.startIndex;
            }
            else {
                startAt = 0;
            }
            finishAt = startAt + this.model.get("itemsPerPage") - 1;
            // TODO: need to do some of this initialization in ViewSwitcher
            this.mapViewer.dataSources.removeAll();

            //defaultPoint = jsonDataSource.defaultPoint;
            //defaultLine = jsonDataSource.defaultLine;
            //defaultPolygon = jsonDataSource.defaultPolygon;
            //billboard = new DynamicBillboard();
            //billboard.image = new ConstantProperty('images/Billboard.png');
            //defaultPoint.billboard = billboard;
            //defaultLine.billboard = billboard;
            //defaultPolygon.billboard = billboard;

            for (i = startAt; i <= finishAt; i++) {

                //this object contains the metacard and the relevance
                metacardResult = this.model.get("results").at(i);
                if (metacardResult) {
                    jsonDataSource = new GeoJsonDataSource();

                    // jsonDataSource.load(testGeoJson, 'Test JSON');
                    metacard = metacardResult.get("metacard");
                    jsonDataSource.load(metacard.toJSON(), metacard.get("properties").get("title"));

                    this.mapViewer.dataSources.add(jsonDataSource);
                }
            }
        },
        createMap: function (mapDivId) {
            var viewer;
            viewer = new CesiumViewer(mapDivId, {
                // Start in Columbus Viewer
                // sceneMode : Cesium.SceneMode.COLUMBUS_VIEW,
                sceneMode: SceneMode.SCENE3D,
                animation: false,
                fullscreenButton: false,
                timeline: false,

                // Hide the base layer picker for OpenStreetMaps
                baseLayerPicker: false,
                // Use OpenStreetMaps
                imageryProvider: new OpenStreetMapImageryProvider({
                    url: 'http://tile.openstreetmap.org/'
                })
            });
            return viewer;
        },
        createPicker: function (scene, ellipsoid) {
            var labels, label, handler, cartesian, cartographic;
            labels = new LabelCollection();
            label = labels.add();
            scene.getPrimitives().add(labels);

            // Mouse over the globe to see the cartographic position
            handler = new ScreenSpaceEventHandler(scene.getCanvas());
            handler.setInputAction(function (movement) {
                cartesian = scene.getCamera().controller.pickEllipsoid(movement.endPosition, ellipsoid);
                if (cartesian) {
                    cartographic = ellipsoid.cartesianToCartographic(cartesian);
                    label.setShow(true);
                    label.setText('(' + CesiumMath.toDegrees(cartographic.longitude).toFixed(2) + ', ' + CesiumMath.toDegrees(cartographic.latitude).toFixed(2) + ')');
                    label.setPosition(cartesian);
                } else {
                    label.setText('');
                }
            }, ScreenSpaceEventType.MOUSE_MOVE);

        },
        addAdditionalLayers: function (imageryLayerCollection) {
//        this.addAddtionalLayer(imageryLayerCollection, new Cesium.TileMapServiceImageryProvider({
//            url: 'http://cesium.agi.com/blackmarble',
//            maximumLevel: 8,
//            credit: 'Black Marble imagery courtesy NASA Earth Observatory'
//        }));

            //dev.virtualearth.net

            /* TODO: install and use proxy for WMS to resolve cross domain restrictions
             this._addAddtionalLayer(imageryLayerCollection, new Cesium.WebMapServiceImageryProvider({
             url: 'https://home2.gvs.dev/OGCOverlay/wms',
             layers : 'gvs',
             parameters : {
             transparent : 'true',
             format : 'image/png'
             }
             }));

             */
        },
        addAddtionalLayer: function (imageryLayerCollection, imageryProvider) {
            var layer;

            layer = imageryLayerCollection.addImageryProvider(imageryProvider);

            layer.alpha = 0.5;
            layer.brightness = 2.0;
        },
        flyToLocation: function (geometry) {
            var i, destination, flight, extent, cartArray = [];

            //polygon
            if (geometry.get("coordinates").length === 1 && geometry.get("coordinates")[0].length > 1) {
                for (i in geometry.get("coordinates")[0]) {
                    cartArray.push(Cartographic.fromDegrees(geometry.get("coordinates")[0][i][0], geometry.get("coordinates")[0][i][1], 15000.0));
                }
                extent = Extent.fromCartographicArray(cartArray);
                flight = CameraFlightPath.createAnimationExtent(this.mapViewer.scene, {
                    destination: extent
                });
            }
            else {
                destination = Cartographic.fromDegrees(geometry.get("coordinates")[0], geometry.get("coordinates")[1], 15000.0);
                flight = CameraFlightPath.createAnimationCartographic(this.mapViewer.scene, {
                    destination: destination
                });
            }


            this.mapViewer.scene.getAnimations().add(flight);
        }
    });
    return MapView;

});