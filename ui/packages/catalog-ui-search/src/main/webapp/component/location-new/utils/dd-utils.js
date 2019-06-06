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
const { computeCircle, toKilometers } = require('./geo-helper')
const errorMessages = require('./errors')

const ddRegex = new RegExp('^-?[0-9]*.?[0-9]*$')
const minimumDifference = 0.0001

function ddCoordinateIsBlank(coordinate) {
  return coordinate.length === 0
}

function ddPointIsBlank(point) {
  return (
    ddCoordinateIsBlank(point.latitude) && ddCoordinateIsBlank(point.longitude)
  )
}

function inputIsBlank(dd) {
  switch (dd.shape) {
    case 'point':
      return ddPointIsBlank(dd.point)
    case 'circle':
      return ddPointIsBlank(dd.circle.point)
    case 'line':
      return dd.line.list.length === 0
    case 'polygon':
      return dd.polygon.list.length === 0
    case 'boundingbox':
      return (
        ddCoordinateIsBlank(dd.boundingbox.north) &&
        ddCoordinateIsBlank(dd.boundingbox.south) &&
        ddCoordinateIsBlank(dd.boundingbox.east) &&
        ddCoordinateIsBlank(dd.boundingbox.west)
      )
  }
}

/*
 *  Decimal degrees -> WKT conversion utils
 */
function ddPointToWkt(point) {
  return new wkx.Point(point.longitude, point.latitude)
}

function ddToWkt(dd) {
  if (inputIsBlank(dd)) {
    return null
  }

  let wkt = null
  const points = []
  switch (dd.shape) {
    case 'point':
      wkt = ddPointToWkt(dd.point).toWkt()
      break
    case 'circle':
      const distance = toKilometers(dd.circle.radius, dd.circle.units)
      wkt = computeCircle(ddPointToWkt(dd.circle.point), distance, 36).toWkt()
      break
    case 'line':
      if (dd.line.list.length > 0) {
        dd.line.list.map(point => points.push(ddPointToWkt(point)))
        wkt = new wkx.LineString(points).toWkt()
      }
      break
    case 'polygon':
      if (dd.polygon.list.length > 0) {
        dd.polygon.list.map(point => points.push(ddPointToWkt(point)))
        const p1 = points[0]
        const p2 = points[points.length - 1]
        if (p1.x !== p2.x || p1.y !== p2.y) {
          points.push(new wkx.Point(p1.x, p1.y))
        }
        wkt = new wkx.Polygon(points).toWkt()
      }
      break
    case 'boundingbox':
      const nw = new wkx.Point(dd.boundingbox.west, dd.boundingbox.north)
      const ne = new wkx.Point(dd.boundingbox.east, dd.boundingbox.north)
      const se = new wkx.Point(dd.boundingbox.east, dd.boundingbox.south)
      const sw = new wkx.Point(dd.boundingbox.west, dd.boundingbox.south)
      wkt = new wkx.Polygon([nw, ne, se, sw, nw]).toWkt()
      break
  }
  return wkt
}

/*
 *  Decimal degrees validation utils
 */
function parseDdCoordinate(coordinate) {
  if (ddRegex.exec(coordinate) == null) {
    return null
  }
  const _coordinate = parseFloat(coordinate)
  if (isNaN(_coordinate)) {
    return null
  }
  return _coordinate
}

function validateLatitudeRange(latitude) {
  return latitude >= -90 && latitude <= 90
}

function validateLongitudeRange(longitude) {
  return longitude >= -180 && longitude <= 180
}

function validateDdLatitude(latitude) {
  const _latitude = parseDdCoordinate(latitude)
  if (_latitude == null) {
    return false
  }
  return validateLatitudeRange(_latitude)
}

function validateDdLongitude(longitude) {
  const _longitude = parseDdCoordinate(longitude)
  if (_longitude == null) {
    return false
  }
  return validateLongitudeRange(_longitude)
}

function validateDdPoint(point) {
  return (
    validateDdLatitude(point.latitude) && validateDdLongitude(point.longitude)
  )
}

function validateDdBoundingBox(boundingbox) {
  const north = parseDdCoordinate(boundingbox.north)
  const south = parseDdCoordinate(boundingbox.south)
  const east = parseDdCoordinate(boundingbox.east)
  const west = parseDdCoordinate(boundingbox.west)

  if (north == null || south == null || east == null || west == null) {
    return false
  }

  if (
    !validateLatitudeRange(north) ||
    !validateLatitudeRange(south) ||
    !validateLongitudeRange(east) ||
    !validateLongitudeRange(west)
  ) {
    return false
  }

  if (north < south || east < west) {
    return false
  }

  if (north - south < minimumDifference || east - west < minimumDifference) {
    return false
  }

  return true
}

function validateDd(dd) {
  if (inputIsBlank(dd)) {
    return { valid: true, error: null }
  }

  let valid = true
  let error = null
  switch (dd.shape) {
    case 'point':
      if (!validateDdPoint(dd.point)) {
        valid = false
        error = errorMessages.invalidCoordinates
      }
      break
    case 'circle':
      const radius = parseFloat(dd.circle.radius)
      if (
        isNaN(radius) ||
        radius <= 0 ||
        toKilometers(radius, dd.circle.units) > 10000
      ) {
        valid = false
        error = errorMessages.invalidRadius
      } else if (!validateDdPoint(dd.circle.point)) {
        valid = false
        error = errorMessages.invalidCoordinates
      }
      break
    case 'line':
      if (!dd.line.list.every(validateDdPoint)) {
        valid = false
        error = errorMessages.invalidList
      } else if (dd.line.list.length < 2) {
        valid = false
        error = errorMessages.tooFewPointsLine
      }
      break
    case 'polygon':
      if (!dd.polygon.list.every(validateDdPoint)) {
        valid = false
        error = errorMessages.invalidList
      } else if (dd.polygon.list.length < 3) {
        valid = false
        error = errorMessages.tooFewPointsPolygon
      }
      break
    case 'boundingbox':
      if (
        !validateDdLatitude(dd.boundingbox.north) ||
        !validateDdLatitude(dd.boundingbox.south) ||
        !validateDdLongitude(dd.boundingbox.east) ||
        !validateDdLongitude(dd.boundingbox.west)
      ) {
        valid = false
        error = errorMessages.invalidCoordinates
      } else if (!validateDdBoundingBox(dd.boundingbox)) {
        valid = false
        error = errorMessages.invalidBoundingBoxDd
      }
      break
  }
  return { valid, error }
}

module.exports = {
  ddToWkt,
  validateDd,
  validateDdPoint,
}
