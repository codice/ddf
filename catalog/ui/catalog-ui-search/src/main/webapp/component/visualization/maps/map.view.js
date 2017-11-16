/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global require, setTimeout*/
var wreqr = require('wreqr');
var template = require('./map.hbs');
var Marionette = require('marionette');
var CustomElements = require('js/CustomElements');
var LoadingCompanionView = require('component/loading-companion/loading-companion.view');
var store = require('js/store');
var GeometryCollectionView = require('./geometry.collection.view');
var ClusterCollectionView = require('./cluster.collection.view');
var ClusterCollection = require('./cluster.collection');
var CQLUtils = require('js/CQLUtils');
var LocationModel = require('component/location-old/location-old');
var user = require('component/singletons/user-instance');
var LayersDropdown = require('component/dropdown/layers/dropdown.layers.view');
var DropdownModel = require('component/dropdown/dropdown');
var MapContextMenuDropdown = require('component/dropdown/map-context-menu/dropdown.map-context-menu.view');
var MapModel = require('./map.model');
var MapInfoView = require('component/map-info/map-info.view');
var MapSettingsDropdown = require('component/dropdown/map-settings/dropdown.map-settings.view');
var properties = require('properties');

function wrapNum(x, range) {
    var max = range[1],
        min = range[0],
        d = max - min;
    return ((x - min) % d + d) % d + min;
}

function findExtreme({objArray, property, comparator}) {
    if (objArray.length === 0) {
        return undefined;
    }
    return objArray.reduce((extreme, coordinateObj) => extreme = comparator(extreme, coordinateObj[property]), objArray[0][property]);
}

function getHomeCoordinates() {
    if (properties.mapHome !== "") {
        const separateCoordinates = properties.mapHome.replace(/\s/g, "").split(",");
        if (separateCoordinates.length % 2 === 0){
            return separateCoordinates.reduce((coordinates, coordinate, index) => {
                if (index % 2 === 0) {
                    coordinates.push({
                        lon: coordinate,
                        lat: separateCoordinates[index + 1]
                    });
                }
                return coordinates;
            }, []).map((coordinateObj) => {
                let lon = parseFloat(coordinateObj.lon);
                let lat = parseFloat(coordinateObj.lat);
                if (isNaN(lon) || isNaN(lat)) {
                    return undefined;
                } 
                lon = wrapNum(lon, [-180, 180]);
                lat = wrapNum(lat, [-90, 90]);
                return {
                    lon: lon,
                    lat: lat
                };
            }).filter((coordinateObj) => {
                return coordinateObj !== undefined;  
            });
        }
    } else {
        return [];
    }
}

function getBoundingBox(coordinates) {
    const north = findExtreme({
        objArray: coordinates,
        property: 'lat',
        comparator: Math.max
    });
    const south = findExtreme({
        objArray: coordinates,
        property: 'lat',
        comparator: Math.min
    });
    const east = findExtreme({
        objArray: coordinates,
        property: 'lon',
        comparator: Math.max
    });
    const west = findExtreme({
        objArray: coordinates,
        property: 'lon',
        comparator: Math.min
    });
    if (north === undefined || south === undefined || east === undefined || west === undefined) {
        return undefined;
    }
    return {
        north: north,
        east: east,
        south: south,
        west: west
    };
}

const homeBoundingBox = getBoundingBox(getHomeCoordinates());
const defaultHomeBoundingBox = {
    west: -128,
    south: 24,
    east: -63,
    north: 52
};

module.exports = Marionette.LayoutView.extend({
    tagName: CustomElements.register('map'),
    template: template,
    regions: {
        mapDrawingPopup: '#mapDrawingPopup',
        mapContextMenu: '.map-context-menu',
        mapInfo: '.mapInfo'
    },
    events: {
        'click .cluster-button': 'toggleClustering',
        'click .zoomToHome': 'zoomToHome'
    },
    clusterCollection: undefined,
    clusterCollectionView: undefined,
    geometryCollectionView: undefined,
    map: undefined,
    mapModel: undefined,
    initialize: function(options) {
        if (!options.selectionInterface) {
            throw 'Selection interface has not been provided';
        }
        this.mapModel = new MapModel();
        this.listenTo(store.get('content'), 'change:drawing', this.handleDrawing);
        this.handleDrawing();
    },
    setupCollections: function() {
        if (!this.map) {
            throw 'Map has not been set.'
        }
        this.clusterCollection = new ClusterCollection();
        this.geometryCollectionView = new GeometryCollectionView({
            collection: this.options.selectionInterface.getActiveSearchResults(),
            map: this.map,
            selectionInterface: this.options.selectionInterface,
            clusterCollection: this.clusterCollection
        });
        this.clusterCollectionView = new ClusterCollectionView({
            collection: this.clusterCollection,
            map: this.map,
            selectionInterface: this.options.selectionInterface
        });
    },
    setupListeners: function() {
        this.listenTo(wreqr.vent, 'metacard:overlay', this.map.overlayImage.bind(this.map));
        this.listenTo(wreqr.vent, 'metacard:overlay:remove', this.map.removeOverlay.bind(this.map));
        this.listenTo(wreqr.vent, 'search:maprectanglefly', this.map.zoomToExtent.bind(this.map));
        this.listenTo(this.options.selectionInterface, 'reset:activeSearchResults', this.map.removeAllOverlays.bind(this.map));

        this.listenTo(this.options.selectionInterface.getSelectedResults(), 'update', this.map.zoomToSelected.bind(this.map));
        this.listenTo(this.options.selectionInterface.getSelectedResults(), 'add', this.map.zoomToSelected.bind(this.map));
        this.listenTo(this.options.selectionInterface.getSelectedResults(), 'remove', this.map.zoomToSelected.bind(this.map));

        this.listenTo(user.get('user').get('preferences'), 'change:resultFilter', this.handleCurrentQuery);
        this.listenTo(this.options.selectionInterface, 'change:currentQuery', this.handleCurrentQuery);
        this.handleCurrentQuery();

        if (this.options.selectionInterface.getSelectedResults().length > 0) {
            this.map.zoomToSelected(this.options.selectionInterface.getSelectedResults());
        } else {
            this.zoomToHome();
        }
        this.map.onMouseMove(this.onMapHover.bind(this));
        this.map.onRightClick(this.onRightClick.bind(this));
        this.setupRightClickMenu();
        this.setupMapInfo();
    },
    zoomToHome: function() {
        this.map.zoomToBoundingBox(homeBoundingBox !== undefined ? homeBoundingBox : defaultHomeBoundingBox);
    },
    addHome: function() {
        this.$el.find('.cesium-viewer-toolbar').append('<button class="is-button zoomToHome"><span class="fa fa-home"></span><span> Home</span></div>');
    },
    addClustering: function() {
        this.$el.find('.cesium-viewer-toolbar').append('<button class="is-button cluster cluster-button">' +
            '<span class="fa fa-cubes is-not-clustering"></span>' +
            '<span class="fa fa-cube is-clustering"></span>' +
            '<span> Cluster </span>' +
            '<span class="fa fa-toggle-on is-clustering"></span>' +
            '</div>');
    },
    addSettings: function(){
        this.$el.find('.cesium-viewer-toolbar').append('<div class="toolbar-settings is-button"></div>');
        this.addRegion('toolbarSettings', '.toolbar-settings');
        this.toolbarSettings.show(new MapSettingsDropdown({
            model: new DropdownModel()
        }));
    },
    onMapHover: function(event, mapEvent) {
        var metacard = this.options.selectionInterface
            .getCompleteActiveSearchResults()
            .get(mapEvent.mapTarget);
        this.updateTarget(metacard);
        this.$el.toggleClass('is-hovering', Boolean(mapEvent.mapTarget && mapEvent.mapTarget !== ('userDrawing')));
    },
    updateMouseCoordinates: function(coordinates){
        this.mapModel.set({
            mouseLat: Number(coordinates.lat.toFixed(6)), // wrap in Number to chop off trailing zero
            mouseLon: Number(wrapNum(coordinates.lon, [-180, 180]).toFixed(6))
        });
    },
    updateTarget: function(metacard){
        var target;
        var targetMetacard;
        if (metacard) {
            target = metacard.get('metacard').get('properties').get('title');
            targetMetacard = metacard;
        }
        this.mapModel.set({
            target: target,
            targetMetacard: targetMetacard
        });
    },
    onRightClick: function(event, mapEvent) {
        event.preventDefault();
        this.$el.find('.map-context-menu').css('left', event.offsetX)
            .css('top', event.offsetY);
        this.mapModel.updateClickCoordinates();
        this.mapContextMenu.currentView.model.open();
    },
    setupRightClickMenu: function(){
        this.mapContextMenu.show(new MapContextMenuDropdown({
            model: new DropdownModel(),
            mapModel: this.mapModel,
            selectionInterface: this.options.selectionInterface
        }));
    },
    setupMapInfo: function(){
        this.mapInfo.show(new MapInfoView({
            model: this.mapModel
        }));
    },
    /*
        Map creation is deferred to this method, so that all resources pertaining to the map can be loaded lazily and
        not be included in the initial page payload.
        Because of this, make sure to return a deferred that will resolve when your respective map implementation
        is finished loading / starting up.
        Also, make sure you resolve that deferred by passing the reference to the map implementation.
    */
    loadMap: function() {
        throw 'Map not implemented';
    },
    createMap: function(Map){
        this.map = Map(this.el.querySelector('#mapContainer'),
                this.options.selectionInterface, this.mapDrawingPopup.el, this.el, this);
        this.setupCollections();
        this.setupListeners();
        this.addHome();
        this.addClustering();
        this.addLayers();
        this.addSettings();
        this.endLoading();
    },
    addLayers: function(){
        this.$el.find('.cesium-viewer-toolbar').append('<div class="toolbar-layers is-button"></div>');
        this.addRegion('toolbarLayers', '.toolbar-layers');
        this.toolbarLayers.show(new LayersDropdown({
            model: new DropdownModel()
        }));
    },
    initializeMap: function(){
        this.loadMap().then(function(Map) {
            this.createMap(Map);
        }.bind(this));
    },
    startLoading: function() {
        LoadingCompanionView.beginLoading(this);
    },
    endLoading: function() {
        LoadingCompanionView.endLoading(this);
    },
    onRender: function() {
        this.startLoading();
        setTimeout(function() {
            this.initializeMap();
        }.bind(this), 1000);
    },
    toggleClustering: function() {
        this.$el.toggleClass('is-clustering');
        this.clusterCollectionView.toggleActive();
    },
    handleDrawing: function() {
        this.$el.toggleClass('is-drawing', store.get('content').get('drawing'));
    },
    handleCurrentQuery: function() {
        this.removePreviousLocations();
        var currentQuery = this.options.selectionInterface.get('currentQuery');
        if (currentQuery) {
            this.handleFilter(CQLUtils.transformCQLToFilter(currentQuery.get('cql')), currentQuery.get('color'));
        }
        var resultFilter = user.get('user').get('preferences').get('resultFilter');
        if (resultFilter){
            this.handleFilter(CQLUtils.transformCQLToFilter(resultFilter), '#c89600');
        }
    },
    handleFilter: function(filter, color){
        if (filter.filters) {
            filter.filters.forEach(function(subfilter){
                this.handleFilter(subfilter, color);
            }.bind(this));
        } else {
            var pointText;
            var locationModel;
            switch (filter.type) {
                case 'DWITHIN':
                    if (CQLUtils.isPointRadiusFilter(filter)) {
                        pointText = filter.value.value.substring(6);
                        pointText = pointText.substring(0, pointText.length - 1);
                        var latLon = pointText.split(' ');
                        locationModel = new LocationModel({
                            lat: latLon[1],
                            lon: latLon[0],
                            radius: filter.distance,
                            color: color
                        });
                        this.map.showCircleShape(locationModel);
                    } else {
                        pointText = filter.value.value.substring(11);
                        pointText = pointText.substring(0, pointText.length - 1);
                        locationModel = new LocationModel({
                            lineWidth: filter.distance,
                            line: pointText.split(',').map(function(coordinate) {
                                return coordinate.split(' ').map(function(value) {
                                    return Number(value)
                                });
                            }),
                            color: color
                        });
                        this.map.showLineShape(locationModel);
                    }
                    break;
                case 'INTERSECTS':
                    var filterValue = typeof(filter.value) === 'string' ? filter.value : filter.value.value;
                    locationModel = new LocationModel({
                        polygon: CQLUtils.arrayFromCQLGeometry(filterValue),
                        color: color
                    });
                    this.map.showPolygonShape(locationModel);
                    break;
            }
        }
    },
    removePreviousLocations: function(){
        this.map.destroyShapes();
    },
    onDestroy: function() {
        if (this.geometryCollectionView){
            this.geometryCollectionView.destroy();
        }
        if (this.clusterCollectionView){
            this.clusterCollectionView.destroy();
        }
        if (this.clusterCollection){
            this.clusterCollection.reset();
        }
        if (this.map) {
            this.map.destroy();
        }
    }
});