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

const _ = require('underscore')
const Openlayers = require('openlayers')
const properties = require('../../../../js/properties.js')

function convertPointCoordinate(point) {
  const coords = [point[0], point[1]]
  return Openlayers.proj.transform(coords, 'EPSG:4326', properties.projection)
}

function unconvertPointCoordinate(point) {
  return Openlayers.proj.transform(point, properties.projection, 'EPSG:4326')
}

/*
  A variety of helpful functions for dealing with Openlayers
*/
module.exports = {
  /*
      Calculates the center of given a geometry (WKT)
    */
  calculateOpenlayersCenterOfGeometry(propertyModel) {
    const lineObject = propertyModel
      .getPoints()
      .map(coordinate => convertPointCoordinate(coordinate))
    const extent = Openlayers.extent.boundingExtent(lineObject)
    return Openlayers.extent.getCenter(extent)
  },
  /*
      Calculates the center of given a geometry (WKT)
    */
  calculateCartographicCenterOfGeometryInDegrees(propertyModel) {
    const openlayersCenter = this.calculateOpenlayersCenterOfGeometry(
      propertyModel
    )
    return unconvertPointCoordinate(openlayersCenter)
  },
  /*
      Calculates the center of given geometries (WKT)
    */
  calculateOpenlayersCenterOfGeometries(propertyModels) {
    const allPoints = _.flatten(
      propertyModels.map(propertyModel => propertyModel.getPoints()),
      true
    ).map(coordinate => convertPointCoordinate(coordinate))
    const extent = Openlayers.extent.boundingExtent(allPoints)
    return Openlayers.extent.getCenter(extent)
  },
  /*
      Calculates the center of given geometries (WKT)
    */
  calculateCartographicCenterOfGeometriesInDegrees(propertyModels) {
    const openlayersCenter = this.calculateOpenlayersCenterOfGeometries(
      propertyModels
    )
    return unconvertPointCoordinate(openlayersCenter)
  },
}
