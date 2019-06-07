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

const _ = require('underscore')
const Cesium = require('cesium')

/*
  A variety of helpful functions for dealing with Cesium
*/
module.exports = {
  /*
      Calculates the center of given a geometry (WKT)
    */
  calculateCartesian3CenterOfGeometry(propertyModel) {
    return Cesium.BoundingSphere.fromPoints(
      Cesium.Cartesian3.fromDegreesArray(_.flatten(propertyModel.getPoints()))
    ).center
  },
  /*
      Calculates the center of given a geometry (WKT)
    */
  calculateCartographicCenterOfGeometryInRadians(propertyModel) {
    return Cesium.Cartographic.fromCartesian(
      this.calculateCartesian3CenterOfGeometry(propertyModel)
    )
  },
  /*
      Calculates the center of given a geometry (WKT)
    */
  calculateCartographicCenterOfGeometryInDegrees(propertyModel) {
    const cartographicCenterInRadians = this.calculateCartographicCenterOfGeometryInRadians(
      propertyModel
    )
    return [
      Cesium.Math.toDegrees(cartographicCenterInRadians.longitude),
      Cesium.Math.toDegrees(cartographicCenterInRadians.latitude),
    ]
  },
  calculateWindowCenterOfGeometry(geometry, map) {
    let cartesian3position = geometry
    if (cartesian3position.constructor !== Cesium.Cartesian3) {
      cartesian3position = this.calculateCartesian3CenterOfGeometry(
        cartesian3position
      )
    }
    return Cesium.SceneTransforms.wgs84ToWindowCoordinates(
      map.scene,
      cartesian3position
    )
  },
  /*
      Calculates the center of given geometries (WKT)
    */
  calculateCartesian3CenterOfGeometries(propertyModels) {
    const allPoints = propertyModels.map(propertyModel =>
      propertyModel.getPoints()
    )
    return Cesium.BoundingSphere.fromPoints(
      Cesium.Cartesian3.fromDegreesArray(_.flatten(allPoints))
    ).center
  },
  /*
      Calculates the center of given geometries (WKT)
    */
  calculateCartographicCenterOfGeometriesInRadians(propertyModels) {
    return Cesium.Cartographic.fromCartesian(
      this.calculateCartesian3CenterOfGeometries(propertyModels)
    )
  },
  /*
      Calculates the center of given geometries (WKT)
    */
  calculateCartographicCenterOfGeometriesInDegrees(propertyModels) {
    const cartographicCenterInRadians = this.calculateCartographicCenterOfGeometriesInRadians(
      propertyModels
    )
    return [
      Cesium.Math.toDegrees(cartographicCenterInRadians.longitude),
      Cesium.Math.toDegrees(cartographicCenterInRadians.latitude),
    ]
  },
}
