var MapView = Backbone.View.extend({
    initialize: function(options) {
        _.bindAll(this, "render");
        if(options && options.results)
        {
            this.model = new SearchResult(options);
        }
    },
    render: function() {
        //this isn't a standard backbone view, this is only here as a bridge to the map
        createResultsOnMap()
    },
    createResultsOnMap: function(startAt, finishAt) {
        var i, metacard, jsonDataSource, goeJson, viewer, defaultPoint, defaultLine, defaultPolygon, billboard;
        viewer = getViewSwitcher().getMapViewer();
        // TODO: need to do some of this initialization in ViewSwitcher
        viewer.dataSources.removeAll();

        //defaultPoint = jsonDataSource.defaultPoint;
        //defaultLine = jsonDataSource.defaultLine;
        //defaultPolygon = jsonDataSource.defaultPolygon;
        //billboard = new DynamicBillboard();
        //billboard.image = new ConstantProperty('images/Billboard.png');
        //defaultPoint.billboard = billboard;
        //defaultLine.billboard = billboard;
        //defaultPolygon.billboard = billboard;

        for (i = startAt; i <= finishAt; i++) {

            metacard = getMetacard(i);
            if (metacard) {
                jsonDataSource = new Cesium.GeoJsonDataSource();

                // jsonDataSource.load(testGeoJson, 'Test JSON');
                jsonDataSource.load(metacard, metacard.properties.title);

                viewer.dataSources.add(jsonDataSource);


            }
        }
    }
});