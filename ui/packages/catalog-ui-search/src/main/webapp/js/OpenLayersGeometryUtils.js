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

/*jshint esversion: 6, bitwise: false*/
const ol = require('openlayers')
const properties = require('./properties.js')
const Common = require('./Common')

module.exports = {
  getCoordinatesFromGeometry: geometry => {
    const type = geometry.getType()
    switch (type) {
      case 'LineString':
        return geometry.getCoordinates()
      case 'Polygon':
        return geometry.getCoordinates()[0]
      case 'Circle':
        return [geometry.getCenter()]
      default:
        return []
    }
  },
  setCoordinatesForGeometry: (geometry, coordinates) => {
    const type = geometry.getType()
    switch (type) {
      case 'LineString':
        geometry.setCoordinates(coordinates)
        break
      case 'Polygon':
        geometry.setCoordinates([coordinates])
        break
      case 'Circle':
        geometry.setCenter(coordinates[0])
        break
      default:
        break
    }
  },
  mapCoordinateToLonLat: point =>
    ol.proj.transform(point, properties.projection, 'EPSG:4326'),
  lonLatToMapCoordinate: point =>
    ol.proj.transform(point, 'EPSG:4326', properties.projection),
  wrapCoordinatesFromGeometry: geometry => {
    let coordinates = module.exports
      .getCoordinatesFromGeometry(geometry)
      .map(module.exports.mapCoordinateToLonLat)
    coordinates = Common.wrapMapCoordinatesArray(coordinates).map(
      module.exports.lonLatToMapCoordinate
    )
    module.exports.setCoordinatesForGeometry(geometry, coordinates)
    return geometry
  },
}
