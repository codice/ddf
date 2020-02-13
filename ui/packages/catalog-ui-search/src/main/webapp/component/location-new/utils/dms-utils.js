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

const dmsRegex = new RegExp('^([0-9]*)°([0-9]*)\'([0-9]*\\.?[0-9]*)"$')
const minimumDifference = 0.0001

const LAT_DEGREES_DIGITS = 2
const LON_DEGREES_DIGITS = 3
const DEFAULT_SECONDS_PRECISION = 4

const Direction = Object.freeze({
  North: 'N',
  South: 'S',
  East: 'E',
  West: 'W',
})

function dmsCoordinateIsBlank(coordinate) {
  return coordinate.coordinate.length === 0
}

function dmsPointIsBlank(point) {
  return (
    dmsCoordinateIsBlank(point.latitude) &&
    dmsCoordinateIsBlank(point.longitude)
  )
}

function inputIsBlank(dms) {
  switch (dms.shape) {
    case 'point':
      return dmsPointIsBlank(dms.point)
    case 'circle':
      return dmsPointIsBlank(dms.circle.point)
    case 'line':
      return dms.line.list.length === 0
    case 'polygon':
      return dms.polygon.list.length === 0
    case 'boundingbox':
      return (
        dmsCoordinateIsBlank(dms.boundingbox.north) &&
        dmsCoordinateIsBlank(dms.boundingbox.south) &&
        dmsCoordinateIsBlank(dms.boundingbox.east) &&
        dmsCoordinateIsBlank(dms.boundingbox.west)
      )
  }
}

function parseDmsCoordinate(coordinate) {
  const matches = dmsRegex.exec(coordinate.coordinate)
  if (matches == null) {
    return null
  }

  const seconds = parseFloat(matches[3])
  if (isNaN(seconds)) {
    return null
  }

  return {
    degrees: parseInt(matches[1]),
    minutes: parseInt(matches[2]),
    seconds,
    direction: coordinate.direction,
  }
}

function dmsCoordinateToDD(coordinate) {
  const dd =
    coordinate.degrees + coordinate.minutes / 60 + coordinate.seconds / 3600
  if (
    coordinate.direction === Direction.North ||
    coordinate.direction === Direction.East
  ) {
    return dd
  } else {
    return -dd
  }
}

/*
 *  DMS -> WKT conversion utils
 */
function dmsPointToWkt(point) {
  const latitude = parseDmsCoordinate(point.latitude)
  const longitude = parseDmsCoordinate(point.longitude)
  const _latitude = dmsCoordinateToDD(latitude)
  const _longitude = dmsCoordinateToDD(longitude)
  return new wkx.Point(_longitude, _latitude)
}

function dmsToWkt(dms) {
  if (inputIsBlank(dms)) {
    return null
  }

  let wkt = null
  const points = []
  switch (dms.shape) {
    case 'point':
      wkt = dmsPointToWkt(dms.point).toWkt()
      break
    case 'circle':
      const distance = toKilometers(dms.circle.radius, dms.circle.units)
      wkt = computeCircle(dmsPointToWkt(dms.circle.point), distance, 36).toWkt()
      break
    case 'line':
      if (dms.line.list.length > 0) {
        dms.line.list.map(point => points.push(dmsPointToWkt(point)))
        wkt = new wkx.LineString(points).toWkt()
      }
      break
    case 'polygon':
      if (dms.polygon.list.length > 0) {
        dms.polygon.list.map(point => points.push(dmsPointToWkt(point)))
        const p1 = points[0]
        const p2 = points[points.length - 1]
        if (p1.x !== p2.x || p1.y !== p2.y) {
          points.push(new wkx.Point(p1.x, p1.y))
        }
        wkt = new wkx.Polygon(points).toWkt()
      }
      break
    case 'boundingbox':
      const nw = {
        latitude: dms.boundingbox.north,
        longitude: dms.boundingbox.west,
      }
      const ne = {
        latitude: dms.boundingbox.north,
        longitude: dms.boundingbox.east,
      }
      const se = {
        latitude: dms.boundingbox.south,
        longitude: dms.boundingbox.east,
      }
      const sw = {
        latitude: dms.boundingbox.south,
        longitude: dms.boundingbox.west,
      }
      const _nw = dmsPointToWkt(nw)
      const _ne = dmsPointToWkt(ne)
      const _se = dmsPointToWkt(se)
      const _sw = dmsPointToWkt(sw)
      wkt = new wkx.Polygon([_nw, _ne, _se, _sw, _nw]).toWkt()
      break
  }
  return wkt
}

/*
 *  DMS validation utils
 */
function validateLatitudeRange(coordinate) {
  if (
    coordinate.degrees > 90 ||
    coordinate.minutes > 60 ||
    coordinate.seconds > 60
  ) {
    return false
  }
  if (
    coordinate.degrees === 90 &&
    (coordinate.minutes > 0 || coordinate.seconds > 0)
  ) {
    return false
  }
  return true
}

function validateLongitudeRange(coordinate) {
  if (
    coordinate.degrees > 180 ||
    coordinate.minutes > 60 ||
    coordinate.seconds > 60
  ) {
    return false
  }
  if (
    coordinate.degrees === 180 &&
    (coordinate.minutes > 0 || coordinate.seconds > 0)
  ) {
    return false
  }
  return true
}

function validateDmsLatitude(latitude) {
  const _latitude = parseDmsCoordinate(latitude)
  if (_latitude == null) {
    return false
  }
  return validateLatitudeRange(_latitude)
}

function validateDmsLongitude(longitude) {
  const _longitude = parseDmsCoordinate(longitude)
  if (_longitude == null) {
    return false
  }
  return validateLongitudeRange(_longitude)
}

function validateDmsPoint(point) {
  return (
    validateDmsLatitude(point.latitude) && validateDmsLongitude(point.longitude)
  )
}

function validateDmsBoundingBox(boundingbox) {
  const north = parseDmsCoordinate(boundingbox.north)
  const south = parseDmsCoordinate(boundingbox.south)
  const east = parseDmsCoordinate(boundingbox.east)
  const west = parseDmsCoordinate(boundingbox.west)

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

  const ddNorth = dmsCoordinateToDD(north)
  const ddSouth = dmsCoordinateToDD(south)
  const ddEast = dmsCoordinateToDD(east)
  const ddWest = dmsCoordinateToDD(west)
  if (ddNorth < ddSouth || ddEast < ddWest) {
    return false
  }

  if (
    ddNorth - ddSouth < minimumDifference ||
    ddEast - ddWest < minimumDifference
  ) {
    return false
  }

  return true
}

function validateDms(dms) {
  if (inputIsBlank(dms)) {
    return { valid: true, error: null }
  }

  let valid = true
  let error = null
  switch (dms.shape) {
    case 'point':
      if (!validateDmsPoint(dms.point)) {
        valid = false
        error = errorMessages.invalidCoordinates
      }
      break
    case 'circle':
      const radius = parseFloat(dms.circle.radius)
      if (
        isNaN(radius) ||
        radius <= 0 ||
        toKilometers(radius, dms.circle.units) > 10000
      ) {
        valid = false
        error = errorMessages.invalidRadius
      } else if (!validateDmsPoint(dms.circle.point)) {
        valid = false
        error = errorMessages.invalidCoordinates
      }
      break
    case 'line':
      if (!dms.line.list.every(validateDmsPoint)) {
        valid = false
        error = errorMessages.invalidList
      } else if (dms.line.list.length < 2) {
        valid = false
        error = errorMessages.tooFewPointsLine
      }
      break
    case 'polygon':
      if (!dms.polygon.list.every(validateDmsPoint)) {
        valid = false
        error = errorMessages.invalidList
      } else if (dms.polygon.list.length < 3) {
        valid = false
        error = errorMessages.tooFewPointsPolygon
      }
      break
    case 'boundingbox':
      if (
        !validateDmsLatitude(dms.boundingbox.north) ||
        !validateDmsLatitude(dms.boundingbox.south) ||
        !validateDmsLongitude(dms.boundingbox.east) ||
        !validateDmsLongitude(dms.boundingbox.west)
      ) {
        valid = false
        error = errorMessages.invalidCoordinates
      } else if (!validateDmsBoundingBox(dms.boundingbox)) {
        valid = false
        error = errorMessages.invalidBoundingBoxDms
      }
      break
  }
  return { valid, error }
}

/*
 *  Decimal degrees -> DMS conversion utils
 */
function roundTo(num, sigDigits) {
  const scaler = 10 ** sigDigits
  return Math.round(num * scaler) / scaler
}

function pad(num, width) {
  return num.toString().padStart(width, '0')
}

function padDecimal(num, width) {
  const decimalParts = num.toString().split('.')
  if (decimalParts.length > 1) {
    return decimalParts[0].padStart(width, '0') + '.' + decimalParts[1]
  } else {
    return pad(num, width)
  }
}

function ddToDmsCoordinate(
  dd,
  direction,
  degreesPad,
  secondsPrecision = DEFAULT_SECONDS_PRECISION
) {
  const ddAbsoluteValue = Math.abs(dd)
  const degrees = Math.trunc(ddAbsoluteValue)
  const degreeFraction = ddAbsoluteValue - degrees
  const minutes = Math.trunc(60 * degreeFraction)
  const seconds = 3600 * degreeFraction - 60 * minutes
  const secondsRounded = roundTo(seconds, secondsPrecision)
  return {
    coordinate: `${pad(degrees, degreesPad)}°${pad(minutes, 2)}'${padDecimal(
      secondsRounded,
      2
    )}"`,
    direction,
  }
}

function ddToDmsCoordinateLat(
  dd,
  secondsPrecision = DEFAULT_SECONDS_PRECISION
) {
  if (!isNaN(dd)) {
    const direction = dd >= 0 ? Direction.North : Direction.South
    return ddToDmsCoordinate(
      dd,
      direction,
      LAT_DEGREES_DIGITS,
      secondsPrecision
    )
  }
}

function ddToDmsCoordinateLon(
  dd,
  secondsPrecision = DEFAULT_SECONDS_PRECISION
) {
  if (!isNaN(dd)) {
    const direction = dd >= 0 ? Direction.East : Direction.West
    return ddToDmsCoordinate(
      dd,
      direction,
      LON_DEGREES_DIGITS,
      secondsPrecision
    )
  }
}

function getSecondsPrecision(dmsCoordinate) {
  if (dmsCoordinate === undefined) {
    return
  }
  const decimalIndex = dmsCoordinate.indexOf('.')
  // Must subtract 2 instead of 1 because the DMS coordinate ends with "
  const lastNumberIndex = dmsCoordinate.length - 2
  if (decimalIndex > -1 && lastNumberIndex > decimalIndex) {
    return lastNumberIndex - decimalIndex
  }
}

module.exports = {
  dmsToWkt,
  validateDms,
  validateDmsPoint,
  dmsCoordinateToDD,
  parseDmsCoordinate,
  ddToDmsCoordinateLat,
  ddToDmsCoordinateLon,
  getSecondsPrecision,
  Direction,
}
