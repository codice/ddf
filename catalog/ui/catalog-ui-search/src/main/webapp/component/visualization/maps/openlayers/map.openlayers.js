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

var DrawBBox = require('js/widgets/openlayers.bbox');
var DrawCircle = require('js/widgets/openlayers.circle');
var DrawPolygon = require('js/widgets/openlayers.polygon');
var DrawLine = require('js/widgets/openlayers.line');

var properties = require('properties');
var Openlayers = require('openlayers');
var geocoder = require('js/view/openlayers.geocoder');
var LayerCollectionController = require('js/controllers/ol.layerCollection.controller');
var user = require('component/singletons/user-instance');
var User = require('js/model/User');
var wreqr = require('wreqr');

var billboardMarker = require('../billboardMarker.hbs');
var clusterMarker = require('../clusterMarker.hbs');

var defaultColor = '#3c6dd5';

var OpenLayerCollectionController = LayerCollectionController.extend({
    initialize: function() {
        // there is no automatic chaining of initialize.
        LayerCollectionController.prototype.initialize.apply(this, arguments);
    }
});

function createMap(insertionElement) {
    var layerPrefs = user.get('user>preferences>mapLayers');
    User.updateMapLayers(layerPrefs, [], 0);
    var layerCollectionController = new OpenLayerCollectionController({ collection: layerPrefs });
    var map = layerCollectionController.makeMap({
        zoom: 3,
        element: insertionElement
    });

    if (properties.gazetteer) {
        geoCoder = new geocoder.View({ el: $(insertionElement).siblings('#mapTools') });
        geoCoder.render();
    }
    return map;
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

    var rectangle = {};
    var minLon = Number.MAX_VALUE;
    var maxLon = -Number.MAX_VALUE;
    var minLat = Number.MAX_VALUE;
    var maxLat = -Number.MAX_VALUE;

    for (var i = 0, len = regionPoints.length; i < len; i++) {
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
}

function determineIdFromPosition(position, map) {
    var features = [];
    map.forEachFeatureAtPixel(position, function(feature) {
        features.push(feature);
    });
    if (features.length > 0) {
        return features[0].getId();
    }
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

function convertPointCoordinate(point) {
    var coords = [point[0], point[1]];
    return Openlayers.proj.transform(coords, 'EPSG:4326', properties.projection);
}

function convertExtent(extent) {
    return Openlayers.proj.transform(extent, 'EPSG:4326', properties.projection);
}

function unconvertPointCoordinate(point) {
    return Openlayers.proj.transform(point, properties.projection, 'EPSG:4326');
}

module.exports = function OpenlayersMap(insertionElement, selectionInterface, notificationEl) {
    var overlays = {};
    var shapes = [];
    var map = createMap(insertionElement);
    listenToResize();
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
            notificationEl: notificationEl
        });
        new DrawLine.Controller({
            map: map,
            notificationEl: notificationEl
        });
    }

    function resizeMap() {
        map.updateSize();
    }

    function listenToResize() {
        wreqr.vent.on('resize', resizeMap)
    }

    function unlistenToResize() {
        wreqr.vent.off('resize', resizeMap);
    }

    var exposedMethods = _.extend({}, Map, {
        onLeftClick: function(callback) {
            $(map.getTargetElement()).on('click', function(e) {
                var boundingRect = map.getTargetElement().getBoundingClientRect();
                callback(e, {
                    mapTarget: determineIdFromPosition([
                        e.clientX - boundingRect.left,
                        e.clientY - boundingRect.top
                    ], map)
                });
            });
        },
        onRightClick: function(callback) {
            $(map.getTargetElement()).on('contextmenu', function(e) {
                var boundingRect = map.getTargetElement().getBoundingClientRect();
                callback(e, {
                    mapTarget: determineIdFromPosition([
                        e.clientX - boundingRect.left,
                        e.clientY - boundingRect.top
                    ], map)
                });
            });
        },
        onMouseMove: function(callback) {
            $(map.getTargetElement()).on('mousemove', function(e) {
                var boundingRect = map.getTargetElement().getBoundingClientRect();
                callback(e, {
                    mapTarget: determineIdFromPosition([
                        e.clientX - boundingRect.left,
                        e.clientY - boundingRect.top
                    ], map)
                });
            });
        },
        onCameraMoveStart: function(callback) {
            map.on('movestart', callback);
        },
        onCameraMoveEnd: function(callback) {
            map.on('moveend', callback);
        },
        zoomToSelected: function() {
            if (selectionInterface.getSelectedResults().length === 1) {
                this.panToResults(selectionInterface.getSelectedResults());
            }
        },
        panToResults: function(results) {
            var coordinates = _.flatten(results.map(function(result) {
                var geometry = result.get('metacard').get('geometry');
                if (geometry) {
                    return geometry.getAllPoints();
                } else {
                    return [];
                }
            }), true);
            this.panToExtent(coordinates);
        },
        panToExtent: function(coords) {
            if (coords.constructor === Array && coords.length > 0) {
                var zoom = Openlayers.animation.zoom({
                    duration: 250,
                    resolution: map.getView().getResolution()
                });
                var pan1 = Openlayers.animation.pan({
                    duration: 250,
                    source: map.getView().getCenter()
                });

                var lineObject = coords.map(function(coordinate) {
                    return convertPointCoordinate(coordinate);
                });

                var extent = Openlayers.extent.boundingExtent(lineObject)

                map.beforeRender(pan1, zoom);
                map.getView().fit(extent, map.getSize(), {
                    maxZoom: map.getView().getZoom()
                });
            }
        },
        panToRectangle: function(rectangle) {
            var zoom = Openlayers.animation.zoom({
                duration: 250,
                resolution: map.getView().getResolution()
            });
            var northWest = Openlayers.proj.transform([rectangle.west, rectangle.north], 'EPSG:4326', properties.projection);
            var southEast = Openlayers.proj.transform([rectangle.east, rectangle.south], 'EPSG:4326', properties.projection);
            var coords = [];
            coords.push(northWest[0]);
            coords.push(southEast[1]);
            coords.push(southEast[0]);
            coords.push(northWest[1]);
            var pan1 = Openlayers.animation.pan({
                duration: 250,
                source: map.getView().getCenter()
            });
            map.beforeRender(pan1);
            map.getView().setCenter(coords);
        },
        zoomToExtent: function(coords) {
            var zoom = Openlayers.animation.zoom({
                duration: 250,
                resolution: map.getView().getResolution()
            });
            var pan1 = Openlayers.animation.pan({
                duration: 250,
                source: map.getView().getCenter()
            });

            var lineObject = coords.map(function(coordinate) {
                return convertPointCoordinate(coordinate);
            });

            var extent = Openlayers.extent.boundingExtent(lineObject)

            map.beforeRender(pan1, zoom);
            map.getView().fit(extent, map.getSize());
        },
        overlayImage: function(model) {
            var metacardId = model.get('properties').get('id');
            this.removeOverlay(metacardId);

            var coords = model.get('geometry').getPolygon();
            var array = _.map(coords, function(coord) {
                return Openlayers.proj.transform([coord.longitude, coord.latitude], 'EPSG:4326', properties.projection);
            });

            var polygon = new Openlayers.geom.Polygon([array]);
            var extent = polygon.getExtent();
            var projection = Openlayers.proj.get(properties.projection);

            var overlayLayer = new Openlayers.layer.Image({
                source: new Openlayers.source.ImageStatic({
                    url: model.get('currentOverlayUrl'),
                    projection: projection,
                    imageExtent: extent
                })
            });

            map.addLayer(overlayLayer);
            overlays[metacardId] = overlayLayer;
        },
        removeOverlay: function(metacardId) {
            if (overlays[metacardId]) {
                map.removeLayer(overlays[metacardId]);
                delete overlays[metacardId];
            }
        },
        removeAllOverlays: function() {
            for (var overlay in overlays) {
                if (overlays.hasOwnProperty(overlay)) {
                    map.removeLayer(overlays[overlay]);
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
            return results.map(function(result) {
                var openlayersCenterOfGeometry = utility.calculateOpenlayersCenterOfGeometry(result.get('metacard').get('geometry'));
                var center = map.getPixelFromCoordinate(openlayersCenterOfGeometry);
                if (center) {
                    return center;
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
            var feature = new Openlayers.Feature({
                geometry: new Openlayers.geom.Point(pointObject)
            });
            feature.setId(options.id);

            feature.setStyle(new Openlayers.style.Style({
                image: new Openlayers.style.Icon({
                    src: getSVGImageForCluster(options.color, options.id.length)
                })
            }));

            var vectorSource = new Openlayers.source.Vector({
                features: [feature]
            });

            var vectorLayer = new Openlayers.layer.Vector({
                source: vectorSource,
                zIndex: 1
            });

            map.addLayer(vectorLayer);

            return vectorLayer;
        },
        /*
          Adds a billboard point utilizing the passed in point and options.
          Options are a view to relate to, and an id, and a color.
        */
        addPoint: function(point, options) {
            var pointObject = convertPointCoordinate(point);
            var feature = new Openlayers.Feature({
                geometry: new Openlayers.geom.Point(pointObject)
            });
            feature.setId(options.id);

            feature.setStyle(new Openlayers.style.Style({
                image: new Openlayers.style.Icon({
                    src: getSVGImage(options.color)
                })
            }));

            var vectorSource = new Openlayers.source.Vector({
                features: [feature]
            });

            var vectorLayer = new Openlayers.layer.Vector({
                source: vectorSource,
                zIndex: 1
            });

            map.addLayer(vectorLayer);

            return vectorLayer;
        },
        /*
          Adds a polyline utilizing the passed in line and options.
          Options are a view to relate to, and an id, and a color.
        */
        addLine: function(line, options) {
            var lineObject = line.map(function(coordinate) {
                return convertPointCoordinate(coordinate);
            });

            var feature = new Openlayers.Feature({
                geometry: new Openlayers.geom.LineString(lineObject)
            });
            feature.setId(options.id);

            var styles = [
                new Openlayers.style.Style({
                    stroke: new Openlayers.style.Stroke({
                        color: 'white',
                        width: 8
                    })
                }),
                new Openlayers.style.Style({
                    stroke: new Openlayers.style.Stroke({
                        color: options.color || defaultColor,
                        width: 4
                    })
                })
            ];

            feature.setStyle(styles);

            var vectorSource = new Openlayers.source.Vector({
                features: [feature]
            });

            var vectorLayer = new Openlayers.layer.Vector({
                source: vectorSource
            });

            map.addLayer(vectorLayer);

            return vectorLayer;
        },
        /*
          Adds a polygon fill utilizing the passed in polygon and options.
          Options are a view to relate to, and an id.
        */
        addPolygon: function(polygon, options) {},
        /*
         Updates a passed in geometry to reflect whether or not it is selected.
         Options passed in are color and isSelected.
         */
        updateCluster: function(geometry, options) {
            if (geometry.constructor === Array) {
                geometry.forEach(function(innerGeometry) {
                    this.updateCluster(innerGeometry, options);
                }.bind(this));
            } else {
                var feature = geometry.getSource().getFeatures()[0];
                var geometryInstance = feature.getGeometry();
                if (geometryInstance.constructor === Openlayers.geom.Point) {
                    feature.setStyle(new Openlayers.style.Style({
                        image: new Openlayers.style.Icon({
                            src: getSVGImageForCluster(options.color, options.count, options.outline, options.textFill)
                        })
                    }));
                } else if (geometryInstance.constructor === Openlayers.geom.LineString) {
                    var styles = [
                        new Openlayers.style.Style({
                            stroke: new Openlayers.style.Stroke({
                                color: 'rgba(255,255,255, .1)',
                                width: 8
                            })
                        }),
                        new Openlayers.style.Style({
                            stroke: new Openlayers.style.Stroke({
                                color: 'rgba(0,0,0, .1)',
                                width: 4
                            })
                        })
                    ];
                    feature.setStyle(styles);
                }
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
            } else {
                var feature = geometry.getSource().getFeatures()[0];
                var geometryInstance = feature.getGeometry();
                if (geometryInstance.constructor === Openlayers.geom.Point) {
                    feature.setStyle(new Openlayers.style.Style({
                        image: new Openlayers.style.Icon({
                            src: getSVGImage(options.color, options.isSelected)
                        })
                    }));
                } else if (geometryInstance.constructor === Openlayers.geom.LineString) {
                    var styles = [
                        new Openlayers.style.Style({
                            stroke: new Openlayers.style.Stroke({
                                color: options.isSelected ? 'black' : 'white',
                                width: 8
                            })
                        }),
                        new Openlayers.style.Style({
                            stroke: new Openlayers.style.Stroke({
                                color: options.color || defaultColor,
                                width: 4
                            })
                        })
                    ];
                    feature.setStyle(styles);
                }
            }
        },
        /*
         Updates a passed in geometry to be hidden
         */
        hideGeometry: function(geometry) {
            geometry.setVisible(false);
        },
        /*
         Updates a passed in geometry to be shown
         */
        showGeometry: function(geometry) {
            geometry.setVisible(true);
        },
        removeGeometry: function(geometry) {
            map.removeLayer(geometry);
        },
        showPolygonShape: function(locationModel){
            var polygon = new DrawPolygon.PolygonView({
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
            var line = new DrawLine.LineView({
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
        destroy: function() {
            unlistenToResize();
        }
    });

    return exposedMethods;
};