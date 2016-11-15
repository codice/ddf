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
/*global require, window*/

var $ = require('jquery');
var _ = require('underscore');
var Map = require('../map');
var utility = require('./utility');

var DrawBBox = require('js/widgets/cesium.bbox');
var DrawCircle = require('js/widgets/cesium.circle');
var DrawPolygon = require('js/widgets/cesium.polygon');
var DrawLine = require('js/widgets/cesium.line');

var properties = require('properties');
var Cesium = require('cesium');
var DrawHelper = require('imports?Cesium=cesium!exports?DrawHelper!drawHelper');
var LayerCollectionController = require('js/controllers/cesium.layerCollection.controller');
var user = require('component/singletons/user-instance');
var User = require('js/model/User');
var wreqr = require('wreqr');

var billboardMarker = require('../billboardMarker.hbs');
var clusterMarker = require('../clusterMarker.hbs');

var defaultColor = '#3c6dd5';
var eyeOffset = new Cesium.Cartesian3(0, 0, -1000);

Cesium.BingMapsApi.defaultKey = properties.bingKey || 0;
var imageryProviderTypes = LayerCollectionController.imageryProviderTypes;
var CesiumLayerCollectionController = LayerCollectionController.extend({
    initialize: function() {
        // there is no automatic chaining of initialize.
        LayerCollectionController.prototype.initialize.apply(this, arguments);
    }
});

function createMap(insertionElement) {
    var layerPrefs = user.get('user>preferences>mapLayers');
    User.updateMapLayers(layerPrefs, [], 0);
    var layerCollectionController = new CesiumLayerCollectionController({
        collection: layerPrefs
    });

    var viewer = layerCollectionController.makeMap({
        element: insertionElement,
        cesiumOptions: {
            sceneMode: Cesium.SceneMode.SCENE3D,
            animation: false,
            fullscreenButton: false,
            timeline: false,
            geocoder: properties.gazetteer,
            homeButton: true,
            sceneModePicker: true,
            selectionIndicator: false,
            infoBox: false,
            //skyBox: false,
            //skyAtmosphere: false,
            baseLayerPicker: false, // Hide the base layer picker,
            mapMode2D: 0
        }
    });

    if (properties.terrainProvider && properties.terrainProvider.type) {
        var type = imageryProviderTypes[properties.terrainProvider.type];
        var initObj = _.omit(properties.terrainProvider, 'type');
        viewer.scene.terrainProvider = new type(initObj);
    }

    if (properties.gazetteer) {
        var container = $('div.cesium-viewer-geocoderContainer');
        container.html("");
        viewer._geocoder = new Cesium.Geocoder({
            container: container[0],
            url: '/services/',
            scene: viewer.scene
        });
    }
    $(insertionElement).find('.cesium-viewer-toolbar')
        .append("<button class='cesium-button cesium-toolbar-button cluster-button' " +
            "data-help='Toggles whether or not results on the map are clustered.'>" +
            "<span class='fa fa-cubes'></span>" +
            "</span><span class='fa fa-cube'></span>" +
            "</button>");

    return viewer;
}

function determineIdFromPosition(position, map) {
    var id;
    var pickedObject = map.scene.pick(position);
    if (pickedObject) {
        id = pickedObject.id;
        if (id && id.constructor === Cesium.Entity) {
            id = id.resultId;
        }
    }
    return id;
}

function expandRectangle(rectangle) {
    var scalingFactor = 0.25;

    var widthGap = Math.abs(rectangle.east) - Math.abs(rectangle.west);
    var heightGap = Math.abs(rectangle.north) - Math.abs(rectangle.south);

    //ensure rectangle has some size
    if (widthGap === 0) {
        widthGap = 1;
    }
    if (heightGap === 0) {
        heightGap = 1;
    }

    rectangle.east = rectangle.east + Math.abs(scalingFactor * widthGap);
    rectangle.north = rectangle.north + Math.abs(scalingFactor * heightGap);
    rectangle.south = rectangle.south - Math.abs(scalingFactor * heightGap);
    rectangle.west = rectangle.west - Math.abs(scalingFactor * widthGap);

    return rectangle;
}

function getResultCenterPoint(result) {
    var regionPoints = [],
        resultQuad,
        quadrantCounts = [{
            quad: 'one',
            count: 0
        }, {
            quad: 'two',
            count: 0
        }, {
            quad: 'three',
            count: 0
        }, {
            quad: 'four',
            count: 0
        }];

    result.each(function(item) {
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

    result.each(function(item) {
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

    var cartPoints = _.map(regionPoints, function(point) {
        return Cesium.Cartographic.fromDegrees(point.longitude, point.latitude, point.altitude);
    });

    var rectangle = Cesium.Rectangle.fromCartographicArray(cartPoints);
    return rectangle;
}

function getDestinationForVisiblePan(rectangle, map) {
    var destinationForZoom = expandRectangle(rectangle);
    if (map.scene.mode === Cesium.SceneMode.SCENE3D){
        destinationForZoom = map.camera.getRectangleCameraCoordinates(destinationForZoom);
    }
    return destinationForZoom;
}

function determineCesiumColor(color) {
    return !_.isUndefined(color) ?
        Cesium.Color.fromCssColorString(color) : Cesium.Color.fromCssColorString(defaultColor);
}

function getSVGImage(color, selected) {
    var svg = billboardMarker({
        fill: color || defaultColor,
        selected: selected
    });
    return 'data:image/svg+xml;base64,' + window.btoa(svg);
}

function getSVGImageForCluster(color, count, outline, textFill) {
    var svg = clusterMarker({
        fill: color || defaultColor,
        count: count,
        outline: outline || 'white',
        textFill: textFill || 'white'
    });
    return 'data:image/svg+xml;base64,' + window.btoa(svg);
}

function convertPointCoordinate(coordinate) {
    return {
        latitude: coordinate[1],
        longitude: coordinate[0],
        altitude: coordinate[2]
    };
}

function isNotVisible(cartesian3CenterOfGeometry, occluder) {
    return !occluder.isPointVisible(cartesian3CenterOfGeometry);
}

module.exports = function CesiumMap(insertionElement, selectionInterface, notificationEl) {
    var overlays = {};
    var shapes = [];
    var map = createMap(insertionElement);
    var drawHelper = new DrawHelper(map);
    var billboardCollection = setupBillboard();
    setupDrawingTools(map);

    function setupDrawingTools(map) {
        new DrawBBox.Controller({
            map: map,
            notificationEl: notificationEl
        });
        new DrawCircle.Controller({
            map: map,
            notificationEl: notificationEl
        });
        new DrawPolygon.Controller({
            map: map,
            notificationEl: notificationEl,
            drawHelper: drawHelper,
        });
        new DrawLine.Controller({
            map: map,
            notificationEl: notificationEl,
            drawHelper: drawHelper,
        });
    }

    function setupBillboard() {
        var billboardCollection = new Cesium.BillboardCollection();
        map.scene.primitives.add(billboardCollection);
        return billboardCollection;
    }

    var exposedMethods = _.extend({}, Map, {
        onLeftClick: function(callback) {
            $(map.scene.canvas).on('click', function(e) {
                var boundingRect = map.scene.canvas.getBoundingClientRect();
                callback(e, {
                    mapTarget: determineIdFromPosition({
                        x: e.clientX - boundingRect.left,
                        y: e.clientY - boundingRect.top
                    }, map)
                });
            });
        },
        onRightClick: function(callback) {
            $(map.scene.canvas).on('contextmenu', function(e) {
                var boundingRect = map.scene.canvas.getBoundingClientRect();
                callback(e, {
                    mapTarget: determineIdFromPosition({
                        x: e.clientX - boundingRect.left,
                        y: e.clientY - boundingRect.top
                    }, map)
                });
            });
        },
        onMouseMove: function(callback) {
            $(map.scene.canvas).on('mousemove', function(e) {
                var boundingRect = map.scene.canvas.getBoundingClientRect();
                callback(e, {
                    mapTarget: determineIdFromPosition({
                        x: e.clientX - boundingRect.left,
                        y: e.clientY - boundingRect.top
                    }, map)
                });
            });
        },
        onCameraMoveStart: function(callback) {
            map.scene.camera.moveStart.addEventListener(callback);
        },
        onCameraMoveEnd: function(callback) {
            map.scene.camera.moveEnd.addEventListener(callback);
        },
        zoomToSelected: function() {
            if (selectionInterface.getSelectedResults().length === 1) {
                this.panToResults(selectionInterface.getSelectedResults());
            }
        },
        panToResults: function(results) {
            var rectangle, cartArray;

            cartArray = _.flatten(results.filter(function(result) {
                return Boolean(result.get('metacard').get('geometry'));
            }).map(function(result) {
                return _.map(result.get('metacard').get('geometry').getAllPoints(), function(coordinate) {
                    return Cesium.Cartographic.fromDegrees(coordinate[0], coordinate[1], map.camera._positionCartographic.height);
                });
            }, true));

            if (cartArray.length > 0) {
                rectangle = Cesium.Rectangle.fromCartographicArray(cartArray);
                this.panToRectangle(rectangle);
            }
        },
        panToExtent: function(coords) {},
        panToRectangle: function(rectangle) {
            map.scene.camera.flyTo({
                duration: 0.50,
                destination: getDestinationForVisiblePan(rectangle, map),
                complete: function() {
                    map.scene.camera.flyTo({
                        duration: 0.25,
                        destination: getDestinationForVisiblePan(rectangle, map)
                    });
                }
            });
        },
        zoomToExtent: function(coords) {},
        overlayImage: function(model) {
            var metacardId = model.get('properties').get('id');
            this.removeOverlay(metacardId);

            var coords = model.get('geometry').getPolygon();
            var cartographics = _.map(coords, function(coord) {
                return Cesium.Cartographic.fromDegrees(coord.longitude, coord.latitude, coord.altitude);
            });

            var rectangle = Cesium.Rectangle.fromCartographicArray(cartographics);

            var overlayLayer = map.scene.imageryLayers.addImageryProvider(new Cesium.SingleTileImageryProvider({
                url: model.get('currentOverlayUrl'),
                rectangle: rectangle
            }));

            overlays[metacardId] = overlayLayer;
        },
        removeOverlay: function(metacardId) {
            if (overlays[metacardId]) {
                map.scene.imageryLayers.remove(overlays[metacardId]);
                delete overlays[metacardId];
            }
        },
        removeAllOverlays: function() {
            for (var overlay in overlays) {
                if (overlays.hasOwnProperty(overlay)) {
                    map.scene.imageryLayers.remove(overlays[overlay]);
                }
            }
            overlays = {};
        },
        getCartographicCenterOfClusterInDegrees: function(cluster) {
            return utility.calculateCartographicCenterOfGeometriesInDegrees(cluster.get('results').map(function(result) {
                return result.get('metacard').get('geometry');
            }));
        },
        getWindowLocationsOfResults: function(results) {
            var occluder;
            if (map.scene.mode === Cesium.SceneMode.SCENE3D) {
                occluder = new Cesium.EllipsoidalOccluder(Cesium.Ellipsoid.WGS84, map.scene.camera.position);
            }
            return results.map(function(result) {
                var cartesian3CenterOfGeometry = utility.calculateCartesian3CenterOfGeometry(result.get('metacard').get('geometry'));
                if (occluder && isNotVisible(cartesian3CenterOfGeometry, occluder)) {
                    return undefined;
                }
                var center = utility.calculateWindowCenterOfGeometry(cartesian3CenterOfGeometry, map);
                if (center) {
                    return [center.x, center.y];
                } else {
                    return undefined;
                }
            });
        },
        /*
            Adds a billboard point utilizing the passed in point and options.
            Options are a view to relate to, and an id, and a color.
        */
        addPointWithText: function(point, options) {
            var pointObject = convertPointCoordinate(point);
            var cartographicPosition = Cesium.Cartographic.fromDegrees(
                pointObject.longitude,
                pointObject.latitude,
                pointObject.altitude
            );
            var cartesianPosition = map.scene.globe.ellipsoid.cartographicToCartesian(cartographicPosition);
            var billboardRef = billboardCollection.add({
                image: getSVGImageForCluster(options.color, options.id.length),
                position: cartesianPosition,
                id: options.id,
                eyeOffset: eyeOffset
            });
            //if there is a terrain provider and no altitude has been specified, sample it from the configured terrain provider
            if (!pointObject.altitude && map.scene.terrainProvider) {
                var promise = Cesium.sampleTerrain(map.scene.terrainProvider, 5, [cartographicPosition]);
                Cesium.when(promise, function(updatedCartographic) {
                    if (updatedCartographic[0].height && !options.view.isDestroyed) {
                        cartesianPosition = map.scene.globe.ellipsoid.cartographicToCartesian(updatedCartographic[0]);
                        billboardRef.position = cartesianPosition;
                    }
                });
            }

            return billboardRef;
        },
        /*
          Adds a billboard point utilizing the passed in point and options.
          Options are a view to relate to, and an id, and a color.
        */
        addPoint: function(point, options) {
            var pointObject = convertPointCoordinate(point);
            var cartographicPosition = Cesium.Cartographic.fromDegrees(
                pointObject.longitude,
                pointObject.latitude,
                pointObject.altitude
            );
            var billboardRef = billboardCollection.add({
                image: getSVGImage(options.color),
                position: map.scene.globe.ellipsoid.cartographicToCartesian(cartographicPosition),
                id: options.id,
                eyeOffset: eyeOffset
            });
            //if there is a terrain provider and no altitude has been specified, sample it from the configured terrain provider
            if (!pointObject.altitude && map.scene.terrainProvider) {
                var promise = Cesium.sampleTerrain(map.scene.terrainProvider, 5, [cartographicPosition]);
                Cesium.when(promise, function(updatedCartographic) {
                    if (updatedCartographic[0].height && !options.view.isDestroyed) {
                        billboardRef.position = map.scene.globe.ellipsoid.cartographicToCartesian(updatedCartographic[0]);
                    }
                });
            }

            return billboardRef;
        },
        /*
          Adds a polyline utilizing the passed in line and options.
          Options are a view to relate to, and an id, and a color.
        */
        addLine: function(line, options) {
            var lineObject = line.map(function(coordinate) {
                return convertPointCoordinate(coordinate);
            });
            var cartPoints = _.map(lineObject, function(point) {
                return Cesium.Cartographic.fromDegrees(point.longitude, point.latitude, point.altitude);
            });
            var cartesian = map.scene.globe.ellipsoid.cartographicArrayToCartesianArray(cartPoints);

            var polylineCollection = new Cesium.PolylineCollection();
            var polyline = polylineCollection.add({
                width: 8,
                material: Cesium.Material.fromType('PolylineOutline', {
                    color: determineCesiumColor(options.color),
                    outlineColor: Cesium.Color.WHITE,
                    outlineWidth: 4
                }),
                id: options.id,
                positions: cartesian
            });

            if (map.scene.terrainProvider) {
                var promise = Cesium.sampleTerrain(map.scene.terrainProvider, 5, cartPoints);
                Cesium.when(promise, function(updatedCartographic) {
                    var positions = map.scene.globe.ellipsoid.cartographicArrayToCartesianArray(updatedCartographic);
                    if (updatedCartographic[0].height && !options.view.isDestroyed) {
                        polyline.positions = positions;
                    }
                });
            }

            map.scene.primitives.add(polylineCollection);
            return polylineCollection;
        },
        /*
          Adds a polygon fill utilizing the passed in polygon and options.
          Options are a view to relate to, and an id.
        */
        addPolygon: function(polygon, options) {
            var polygonObject = polygon.map(function(coordinate) {
                return convertPointCoordinate(coordinate);
            });
            var cartPoints = _.map(polygonObject, function(point) {
                return Cesium.Cartographic.fromDegrees(point.longitude, point.latitude, point.altitude);
            });
            var cartesian = map.scene.globe.ellipsoid.cartographicArrayToCartesianArray(cartPoints);

            var unselectedPolygonRef = map.entities.add({
                polygon: {
                    hierarchy: cartesian,
                    material: new Cesium.GridMaterialProperty({
                        color: Cesium.Color.WHITE,
                        cellAlpha: 0.0,
                        lineCount: new Cesium.Cartesian2(2, 2),
                        lineThickness: new Cesium.Cartesian2(2.0, 2.0),
                        lineOffset: new Cesium.Cartesian2(0.0, 0.0)
                    }),
                    perPositionHeight: true
                },
                show: true,
                resultId: options.id,
                showWhenSelected: false
            });

            var selectedPolygonRef = map.entities.add({
                polygon: {
                    hierarchy: cartesian,
                    material: new Cesium.GridMaterialProperty({
                        color: Cesium.Color.BLACK,
                        cellAlpha: 0.0,
                        lineCount: new Cesium.Cartesian2(2, 2),
                        lineThickness: new Cesium.Cartesian2(2.0, 2.0),
                        lineOffset: new Cesium.Cartesian2(0.0, 0.0)
                    }),
                    perPositionHeight: true
                },
                show: false,
                resultId: options.id,
                showWhenSelected: true
            });

            if (map.scene.terrainProvider) {
                var promise = Cesium.sampleTerrain(map.scene.terrainProvider, 5, cartPoints);
                Cesium.when(promise, function(updatedCartographic) {
                    cartesian = map.scene.globe.ellipsoid.cartographicArrayToCartesianArray(updatedCartographic);
                    if (updatedCartographic[0].height && !options.view.isDestroyed) {
                        unselectedPolygonRef.polygon.hierarchy.setValue(cartesian);
                        selectedPolygonRef.polygon.hierarchy.setValue(cartesian);
                    }
                });
            }

            return [unselectedPolygonRef, selectedPolygonRef];
        },
        /*
         Updates a passed in geometry to reflect whether or not it is selected.
         Options passed in are color and isSelected.
         */
        updateCluster: function(geometry, options) {
            if (geometry.constructor === Array) {
                geometry.forEach(function(innerGeometry) {
                    this.updateCluster(innerGeometry, options);
                }.bind(this));
            }
            if (geometry.constructor === Cesium.Billboard) {
                geometry.image = getSVGImageForCluster(options.color, options.count, options.outline, options.textFill);
            } else if (geometry.constructor === Cesium.PolylineCollection) {
                geometry._polylines.forEach(function(polyline) {
                    polyline.material = Cesium.Material.fromType('PolylineOutline', {
                        color: determineCesiumColor(options.color),
                        outlineColor: options.isSelected ? Cesium.Color.BLACK : Cesium.Color.WHITE,
                        outlineWidth: 4
                    });
                });
            } else if (geometry.showWhenSelected) {
                geometry.show = options.isSelected;
            } else {
                geometry.show = !options.isSelected;
            }
        },
        /*
          Updates a passed in geometry to reflect whether or not it is selected.
          Options passed in are color and isSelected.
        */
        updateGeometry: function(geometry, options) {
            if (geometry.constructor === Array) {
                geometry.forEach(function(innerGeometry) {
                    this.updateGeometry(innerGeometry, options);
                }.bind(this));
            }
            if (geometry.constructor === Cesium.Billboard) {
                geometry.image = getSVGImage(options.color, options.isSelected);
            } else if (geometry.constructor === Cesium.PolylineCollection) {
                geometry._polylines.forEach(function(polyline) {
                    polyline.material = Cesium.Material.fromType('PolylineOutline', {
                        color: determineCesiumColor(options.color),
                        outlineColor: options.isSelected ? Cesium.Color.BLACK : Cesium.Color.WHITE,
                        outlineWidth: 4
                    });
                });
            } else if (geometry.showWhenSelected) {
                geometry.show = options.isSelected;
            } else {
                geometry.show = !options.isSelected;
            }
        },
        /*
         Updates a passed in geometry to be hidden
         */
        hideGeometry: function(geometry) {
            if (geometry.constructor === Cesium.Billboard) {
                geometry.show = false;
            } else if (geometry.constructor === Cesium.PolylineCollection) {
                geometry._polylines.forEach(function(polyline) {
                    polyline.show = false;
                });
            }
        },
        /*
         Updates a passed in geometry to be shown
         */
        showGeometry: function(geometry) {
            if (geometry.constructor === Cesium.Billboard) {
                geometry.show = true;
            } else if (geometry.constructor === Cesium.PolylineCollection) {
                geometry._polylines.forEach(function(polyline) {
                    polyline.show = true;
                });
            }
        },
        removeGeometry: function(geometry) {
            billboardCollection.remove(geometry);
            map.scene.primitives.remove(geometry);
            map.entities.remove(geometry);
        },
        showPolygonShape: function(locationModel){
            var polygon = new DrawPolygon.PolygonRenderView({
                model: locationModel,
                map: map
            });
            shapes.push(polygon);
        },
        showCircleShape: function(locationModel){
            var circle = new DrawCircle.CircleView({
                model: locationModel,
                map: map
            });
            shapes.push(circle);
        },
        showLineShape: function(locationModel){
            var line = new DrawLine.LineRenderView({
                model: locationModel,
                map: map
            });
            shapes.push(line);
        },
        destroyShapes: function(){
            shapes.forEach(function(shape){
                shape.destroy();
            });
            shapes = [];
        },
        destroy: function() {}
    });

    return exposedMethods;
};