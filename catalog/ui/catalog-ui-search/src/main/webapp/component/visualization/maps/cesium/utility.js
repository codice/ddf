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


/*
  A variety of helpful functions for dealing with Cesium
*/
module.exports = {
    /*
      Calculates the center of given a geometry (WKT)
    */
    calculateCartesian3CenterOfGeometry: function(geometry) {
        return Cesium.BoundingSphere.fromPoints(Cesium.Cartesian3.fromDegreesArray(_.flatten(geometry.getAllPoints()))).center;
    },
    /*
      Calculates the center of given a geometry (WKT)
    */
    calculateCartographicCenterOfGeometryInRadians: function(geometry) {
        return Cesium.Cartographic.fromCartesian(this.calculateCartesian3CenterOfGeometry(geometry));
    },
    /*
      Calculates the center of given a geometry (WKT)
    */
    calculateCartographicCenterOfGeometryInDegrees: function(geometry) {
        var cartographicCenterInRadians = this.calculateCartographicCenterOfGeometryInRadians(geometry);
        return [Cesium.Math.toDegrees(cartographicCenterInRadians.longitude), Cesium.Math.toDegrees(cartographicCenterInRadians.latitude)];
    },
    calculateWindowCenterOfGeometry: function(geometry, map) {
        var cartesian3position = geometry;
        if (cartesian3position.constructor !== Cesium.Cartesian3) {
            cartesian3position = this.calculateCartesian3CenterOfGeometry(cartesian3position);
        }
        return Cesium.SceneTransforms.wgs84ToWindowCoordinates(map.scene, cartesian3position);
    },
    /*
      Calculates the center of given geometries (WKT)
    */
    calculateCartesian3CenterOfGeometries: function(geometries) {
        var allPoints = geometries.map(function(geometry) {
            return geometry.getAllPoints();
        });
        return Cesium.BoundingSphere.fromPoints(Cesium.Cartesian3.fromDegreesArray(_.flatten(allPoints))).center;
    },
    /*
      Calculates the center of given geometries (WKT)
    */
    calculateCartographicCenterOfGeometriesInRadians: function(geometries) {
        return Cesium.Cartographic.fromCartesian(this.calculateCartesian3CenterOfGeometries(geometries));
    },
    /*
      Calculates the center of given geometries (WKT)
    */
    calculateCartographicCenterOfGeometriesInDegrees: function(geometries) {
        var cartographicCenterInRadians = this.calculateCartographicCenterOfGeometriesInRadians(geometries);
        return [Cesium.Math.toDegrees(cartographicCenterInRadians.longitude), Cesium.Math.toDegrees(cartographicCenterInRadians.latitude)];
    },
};