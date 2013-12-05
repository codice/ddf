/*global define*/

define(function (require) {
    "use strict";
    var Backbone = require('backbone'),
        _ = require('underscore'),
        Cesium = require('cesium'),
        SceneMode = require('cesium').SceneMode,
        CesiumViewer = require('cesium').Viewer,

        MapView = Backbone.View.extend({
            initialize: function () {
                _.bindAll(this, "render", "createResultsOnMap", "addAdditionalLayers", "flyToLocation");
            },
            render: function () {
                this.mapViewer = this.createMap('cesiumContainer');
                this.createPicker(this.mapViewer.scene, this.mapViewer.centralBody.getEllipsoid());
                this.addAdditionalLayers(this.mapViewer.centralBody.getImageryLayers());
                return this;
            },
            createResultsOnMap: function (options) {
                var startAt, finishAt, i, metacardResult, metacard, jsonDataSource;
                if (options) {
                    this.model = options;
                    this.listenTo(this.model, 'change', this.createResultsOnMap);
                }
                startAt = this.model.get("startIndex") - 1;
                finishAt = startAt + this.model.get("count") - 1;
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
                        jsonDataSource = new Cesium.GeoJsonDataSource();

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
                    imageryProvider: new Cesium.OpenStreetMapImageryProvider({
                        url: 'http://tile.openstreetmap.org/'
                    })
                });
                return viewer;
            },
            createPicker: function (scene, ellipsoid) {
                var labels, label, handler, cartesian, cartographic;
                labels = new Cesium.LabelCollection();
                label = labels.add();
                scene.getPrimitives().add(labels);

                // Mouse over the globe to see the cartographic position
                handler = new Cesium.ScreenSpaceEventHandler(scene.getCanvas());
                handler.setInputAction(function (movement) {
                    cartesian = scene.getCamera().controller.pickEllipsoid(movement.endPosition, ellipsoid);
                    if (cartesian) {
                        cartographic = ellipsoid.cartesianToCartographic(cartesian);
                        label.setShow(true);
                        label.setText('(' + Cesium.Math.toDegrees(cartographic.longitude).toFixed(2) + ', ' + Cesium.Math.toDegrees(cartographic.latitude).toFixed(2) + ')');
                        label.setPosition(cartesian);
                    } else {
                        label.setText('');
                    }
                }, Cesium.ScreenSpaceEventType.MOUSE_MOVE);

            },
            addAdditionalLayers: function () {
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
                var destination, flight, extent;

                //polygon
                if (geometry.get("coordinates").length === 1 && geometry.get("coordinates")[0].length > 1) {

                    var cartArray = _.map(geometry.get("coordinates")[0], function (coordinate) {
                        return Cesium.Cartographic.fromDegrees(coordinate[0], coordinate[1], 15000.0);
                    });

                    extent = Cesium.Extent.fromCartographicArray(cartArray);
                    flight = Cesium.CameraFlightPath.createAnimationExtent(this.mapViewer.scene, {
                        destination: extent
                    });
                }
                else {
                    destination = Cesium.Cartographic.fromDegrees(geometry.get("coordinates")[0], geometry.get("coordinates")[1], 15000.0);
                    flight = Cesium.CameraFlightPath.createAnimationCartographic(this.mapViewer.scene, {
                        destination: destination
                    });
                }


                this.mapViewer.scene.getAnimations().add(flight);
            }
        });
    return MapView;
});