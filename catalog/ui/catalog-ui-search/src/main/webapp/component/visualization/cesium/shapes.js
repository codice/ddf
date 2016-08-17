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
/*global window*/

var _ = require('underscore');
var Cesium = require('cesium');
var billboardMarker = require('./billboardMarker.hbs');
var clusterMarker = require('./clusterMarker.hbs');
var defaultColor = '#3c6dd5';


var eyeOffset = new Cesium.Cartesian3(0, 0, -1000);

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

module.exports = {
  /*
    Adds a billboard point utilizing the passed in geocontroller.
    Options are a view to relate to, and an id, and a color.
  */
  addPointWithText: function (point, geocontroller, options) {
    var pointObject = convertPointCoordinate(point);
    var cartographicPosition = Cesium.Cartographic.fromDegrees(
      pointObject.longitude,
      pointObject.latitude,
      pointObject.altitude
    );
    var cartesianPosition = geocontroller.ellipsoid.cartographicToCartesian(cartographicPosition);
    var billboardRef = geocontroller.billboardCollection.add({
      image: getSVGImageForCluster(options.color, options.id.length),
      position: cartesianPosition,
      id: options.id,
      eyeOffset: eyeOffset
    });
    //if there is a terrain provider and no altitude has been specified, sample it from the configured terrain provider
    if (!pointObject.altitude && geocontroller.scene.terrainProvider) {
      var promise = Cesium.sampleTerrain(geocontroller.scene.terrainProvider, 5, [cartographicPosition]);
      Cesium.when(promise, function (updatedCartographic) {
        if (updatedCartographic[0].height && !options.view.isDestroyed) {
          cartesianPosition = geocontroller.ellipsoid.cartographicToCartesian(updatedCartographic[0]);
          billboardRef.position = cartesianPosition;
        }
      });
    }

    return billboardRef;
  },
  /*
    Adds a billboard point utilizing the passed in geocontroller.
    Options are a view to relate to, and an id, and a color.
  */
  addPoint: function (point, geocontroller, options) {
    var pointObject = convertPointCoordinate(point);
    var cartographicPosition = Cesium.Cartographic.fromDegrees(
      pointObject.longitude,
      pointObject.latitude,
      pointObject.altitude
    );
    var billboardRef = geocontroller.billboardCollection.add({
      image: getSVGImage(options.color),
      position: geocontroller.ellipsoid.cartographicToCartesian(cartographicPosition),
      id: options.id,
      eyeOffset: eyeOffset
    });
    //if there is a terrain provider and no altitude has been specified, sample it from the configured terrain provider
    if (!pointObject.altitude && geocontroller.scene.terrainProvider) {
      var promise = Cesium.sampleTerrain(geocontroller.scene.terrainProvider, 5, [cartographicPosition]);
      Cesium.when(promise, function (updatedCartographic) {
        if (updatedCartographic[0].height && !options.view.isDestroyed) {
          billboardRef.position = geocontroller.ellipsoid.cartographicToCartesian(updatedCartographic[0]);
        }
      });
    }

    return billboardRef;
  },
  /*
    Adds a polyline utilizing the passed in geocontroller.
    Options are a view to relate to, and an id, and a color.
  */
  addLine: function (line, geocontroller, options) {
    var lineObject = line.map(function (coordinate) {
      return convertPointCoordinate(coordinate);
    });
    var cartPoints = _.map(lineObject, function (point) {
      return Cesium.Cartographic.fromDegrees(point.longitude, point.latitude, point.altitude);
    });
    var cartesian = geocontroller.ellipsoid.cartographicArrayToCartesianArray(cartPoints);

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

    if (geocontroller.scene.terrainProvider) {
      var promise = Cesium.sampleTerrain(geocontroller.scene.terrainProvider, 5, cartPoints);
      Cesium.when(promise, function (updatedCartographic) {
        var positions = geocontroller.ellipsoid.cartographicArrayToCartesianArray(updatedCartographic);
        if (updatedCartographic[0].height && !options.view.isDestroyed) {
          polyline.positions = positions;
        }
      });
    }

    geocontroller.scene.primitives.add(polylineCollection);
    return polylineCollection;
  },
  /*
    Adds a polygon fill utilizing the passed in geocontroller.
    Options are a view to relate to, and an id.
  */
  addPolygon: function (polygon, geocontroller, options) {
    var polygonObject = polygon.map(function (coordinate) {
      return convertPointCoordinate(coordinate);
    });
    var cartPoints = _.map(polygonObject, function (point) {
      return Cesium.Cartographic.fromDegrees(point.longitude, point.latitude, point.altitude);
    });
    var cartesian = geocontroller.ellipsoid.cartographicArrayToCartesianArray(cartPoints);

    var unselectedPolygonRef = geocontroller.mapViewer.entities.add({
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

    var selectedPolygonRef = geocontroller.mapViewer.entities.add({
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

    if (geocontroller.scene.terrainProvider) {
      var promise = Cesium.sampleTerrain(geocontroller.scene.terrainProvider, 5, cartPoints);
      Cesium.when(promise, function (updatedCartographic) {
        cartesian = geocontroller.ellipsoid.cartographicArrayToCartesianArray(updatedCartographic);
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
  updateCluster: function (geometry, options) {
    if (geometry.constructor === Array){
      geometry.forEach(function(innerGeometry){
        this.updateCluster(innerGeometry, options);
      }.bind(this));
    }
    if (geometry.constructor === Cesium.Billboard) {
      geometry.image = getSVGImageForCluster(options.color, options.count, options.outline, options.textFill);
    } else if (geometry.constructor === Cesium.PolylineCollection) {
      geometry._polylines.forEach(function (polyline) {
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
  updateGeometry: function (geometry, options) {
    if (geometry.constructor === Array){
      geometry.forEach(function(innerGeometry){
        this.updateGeometry(innerGeometry, options);
      }.bind(this));
    }
    if (geometry.constructor === Cesium.Billboard) {
      geometry.image = getSVGImage(options.color, options.isSelected);
    } else if (geometry.constructor === Cesium.PolylineCollection) {
      geometry._polylines.forEach(function (polyline) {
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
  hideGeometry: function (geometry) {
    if (geometry.constructor === Cesium.Billboard) {
      geometry.show = false;
    } else if (geometry.constructor === Cesium.PolylineCollection) {
      geometry._polylines.forEach(function (polyline) {
        polyline.show = false;
      });
    }
  },
  /*
   Updates a passed in geometry to be shown
   */
  showGeometry: function (geometry) {
    if (geometry.constructor === Cesium.Billboard) {
      geometry.show = true;
    } else if (geometry.constructor === Cesium.PolylineCollection) {
      geometry._polylines.forEach(function (polyline) {
        polyline.show = true;
      });
    }
  }
};