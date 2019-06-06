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

const wkx = require('wkx')
const errorMessages = require('./errors')
const DistanceUtils = require('../../../js/DistanceUtils')

function convertUserValueToWKT(val) {
  val = val
    .split(' (')
    .join('(')
    .split(', ')
    .join(',')
  val = val
    .split('MULTIPOINT')
    .map((value, index) => {
      if (value.indexOf('((') === 0) {
        const endOfMultiPoint = value.indexOf('))') + 2
        let multipointStr = value.substring(0, endOfMultiPoint)
        multipointStr = multipointStr
          .split('((')
          .join('(')
          .split('),(')
          .join(',')
          .split('))')
          .join(')')
        return multipointStr + value.substring(endOfMultiPoint)
      } else {
        return value
      }
    })
    .join('MULTIPOINT')
  return val
}

function removeTrailingZeros(wkt) {
  return wkt.replace(/[-+]?[0-9]*\.?[0-9]+/g, number => Number(number))
}

function checkCoordinateOrder(coordinate) {
  return (
    coordinate[0] >= -180 &&
    coordinate[0] <= 180 &&
    coordinate[1] >= -90 &&
    coordinate[1] <= 90
  )
}

function checkGeometryCoordinateOrdering(geometry) {
  switch (geometry.type) {
    case 'Point':
      return checkCoordinateOrder(geometry.coordinates)
    case 'LineString':
    case 'MultiPoint':
      return geometry.coordinates.every(coordinate =>
        checkCoordinateOrder(coordinate)
      )
    case 'Polygon':
    case 'MultiLineString':
      return geometry.coordinates.every(line =>
        line.every(coordinate => checkCoordinateOrder(coordinate))
      )
    case 'MultiPolygon':
      return geometry.coordinates.every(multipolygon =>
        multipolygon.every(polygon =>
          polygon.every(coordinate => checkCoordinateOrder(coordinate))
        )
      )
    case 'GeometryCollection':
      return geometry.geometries.every(subgeometry =>
        checkGeometryCoordinateOrdering(subgeometry)
      )
  }
}

function checkForm(wkt) {
  try {
    const test = wkx.Geometry.parse(wkt)
    return test.toWkt() === removeTrailingZeros(convertUserValueToWKT(wkt))
  } catch (err) {
    return false
  }
}

function checkLonLatOrdering(wkt) {
  try {
    const test = wkx.Geometry.parse(wkt)
    return checkGeometryCoordinateOrdering(test.toGeoJSON())
  } catch (err) {
    return false
  }
}

function inputIsBlank(wkt) {
  return !wkt || wkt.length === 0
}

function validateWkt(wkt) {
  if (inputIsBlank(wkt)) {
    return { valid: true, error: null }
  }

  let valid = true
  let error = null
  if (!checkForm(wkt)) {
    valid = false
    error = errorMessages.malformedWkt
  } else if (!checkLonLatOrdering(wkt)) {
    valid = false
    error = errorMessages.invalidWktCoordinates
  }
  return { valid, error }
}

function createCoordPair(coordinate) {
  return coordinate.map(val => DistanceUtils.coordinateRound(val)).join(' ')
}

function createLineString(coordinates) {
  return (
    '(' +
    coordinates
      .map(coord => {
        return createCoordPair(coord)
      })
      .join(', ') +
    ')'
  )
}

function createMultiLineString(coordinates) {
  return (
    '(' +
    coordinates
      .map(line => {
        return createLineString(line)
      })
      .join(', ') +
    ')'
  )
}

function createMultiPolygon(coordinates) {
  return (
    '(' +
    coordinates
      .map(line => {
        return createMultiLineString(line)
      })
      .join(', ') +
    ')'
  )
}

function createRoundedWktGeo(geoJson) {
  switch (geoJson.type) {
    case 'Point':
      return (
        geoJson.type.toUpperCase() +
        '(' +
        createCoordPair(geoJson.coordinates) +
        ')'
      )
    case 'LineString':
    case 'MultiPoint':
      return geoJson.type.toUpperCase() + createLineString(geoJson.coordinates)
    case 'Polygon':
    case 'MultiLineString':
      return (
        geoJson.type.toUpperCase() + createMultiLineString(geoJson.coordinates)
      )
    case 'MultiPolygon':
      return (
        geoJson.type.toUpperCase() + createMultiPolygon(geoJson.coordinates)
      )
    case 'GeometryCollection':
      return (
        geoJson.type.toUpperCase() +
        '(' +
        geoJson.geometries.map(geo => createRoundedWktGeo(geo)).join(', ') +
        ')'
      )
  }
}

function roundWktCoords(wkt) {
  if (!inputIsBlank(wkt) && checkForm(wkt) && checkLonLatOrdering(wkt)) {
    let parsed = wkx.Geometry.parse(wkt)
    let geoJson = parsed.toGeoJSON()
    return createRoundedWktGeo(geoJson)
  } else {
    return wkt
  }
}

module.exports = {
  validateWkt,
  roundWktCoords,
}
