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
var DrawingUtility = require('../DrawingUtility');

var DrawBBox = require('js/widgets/openlayers.bbox');
var DrawCircle = require('js/widgets/openlayers.circle');
var DrawPolygon = require('js/widgets/openlayers.polygon');
var DrawLine = require('js/widgets/openlayers.line');

var properties = require('properties');
var Openlayers = require('openlayers');
var Geocoder = require('js/view/openlayers.geocoder');
var LayerCollectionController = require('js/controllers/ol.layerCollection.controller');
var user = require('component/singletons/user-instance');
var User = require('js/model/User');
var wreqr = require('wreqr');

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
        var geocoder = new Geocoder.View({ el: $(insertionElement).siblings('#mapTools') });
        geocoder.render();
    }
    return map;
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
                return result.getPoints();
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

            var coords = model.getPoints('location');
            var array = _.map(coords, function(coord) {
                return convertPointCoordinate(coord);
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
                return result;
            }));
        },
        getWindowLocationsOfResults: function(results) {
            return results.map(function(result) {
                var openlayersCenterOfGeometry = utility.calculateOpenlayersCenterOfGeometry(result);
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
                    img: DrawingUtility.getCircleWithText({
                        fillColor: options.color,
                        text: options.id.length,
                    }),
                    imgSize: [44, 44]
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
                    img: DrawingUtility.getCircle({
                        fillColor: options.color
                    }),
                    imgSize: [22, 22]
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
                            img: DrawingUtility.getCircleWithText({
                                fillColor: options.color,
                                strokeColor: options.outline,
                                text: options.count,
                                textColor: options.textFill
                            }),
                            imgSize: [44, 44]
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
                            img: DrawingUtility.getCircle({
                                fillColor: options.color,
                                strokeColor: options.isSelected ? 'black' : 'white' 
                            }),
                            imgSize: [22, 22]
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