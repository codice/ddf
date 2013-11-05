var MapView = Backbone.View.extend({
    initialize: function(options) {
        _.bindAll(this, "render", "createResultsOnMap", "addAdditionalLayers", "flyToLocation");
    },
    render: function() {
        this.mapViewer = this.createMap("cesiumContainer");
        this.createPicker(this.mapViewer.scene, this.mapViewer.centralBody.getEllipsoid());
        this.addAdditionalLayers(this.mapViewer.centralBody.getImageryLayers());
    },
    createResultsOnMap: function(options) {
        var startAt, finishAt, i, metacardResult, jsonDataSource;
        if(options && options.results)
        {
            this.model = new SearchResult(options);
        }
        if(options && options.startAt)
        {
            startAt = options.startAt;
        }
        else
        {
            startAt = 0;
        }
        finishAt = startAt + this.model.results.length - 1;
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
            metacardResult = this.model.results.at(i);
            if (metacardResult) {
                jsonDataSource = new Cesium.GeoJsonDataSource();

                // jsonDataSource.load(testGeoJson, 'Test JSON');
                jsonDataSource.load(metacardResult.get("metacard"), metacardResult.get("metacard").properties.title);

                this.mapViewer.dataSources.add(jsonDataSource);
            }
        }
    },
    createMap: function(mapDivId) {
        var viewer;
        viewer = new Cesium.Viewer(mapDivId, {
            // Start in Columbus Viewer
            // sceneMode : Cesium.SceneMode.COLUMBUS_VIEW,
            sceneMode: Cesium.SceneMode.SCENE3D,
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
    createPicker: function(scene, ellipsoid) {
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
    flyToLocation: function(longitude, latitude) {
        var destination, flight;
        destination = Cesium.Cartographic.fromDegrees(longitude, latitude, 15000.0);

        flight = Cesium.CameraFlightPath.createAnimationCartographic(this.mapViewer.scene, {
            destination : destination
        });
        this.mapViewer.scene.getAnimations().add(flight);
    }
});