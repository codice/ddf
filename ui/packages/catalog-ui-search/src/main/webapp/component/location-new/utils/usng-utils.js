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
const usng = require('usng.js')
const converter = new usng.Converter()

const { computeCircle, toKilometers } = require('./geo-helper')
const errorMessages = require('./errors')

function validateUsngGrid(grid) {
  return converter.isUSNG(grid) !== 0
}

function gridIsBlank(grid) {
  return grid.length === 0
}

function inputIsBlank(usng) {
  switch (usng.shape) {
    case 'point':
      return gridIsBlank(usng.point)
    case 'circle':
      return gridIsBlank(usng.circle.point)
    case 'line':
      return usng.line.list.length === 0
    case 'polygon':
      return usng.polygon.list.length === 0
    case 'boundingbox':
      return gridIsBlank(usng.boundingbox)
  }
}

/*
 *  USNG/MGRS -> WKT conversion utils
 */
function usngGridToWktPoint(grid) {
  const LL = converter.USNGtoLL(grid, true)
  return new wkx.Point(LL.lon, LL.lat)
}

function usngToWkt(usng) {
  if (inputIsBlank(usng)) {
    return null
  }

  let wkt = null
  const points = []
  switch (usng.shape) {
    case 'point':
      wkt = usngGridToWktPoint(usng.point).toWkt()
      break
    case 'circle':
      const distance = toKilometers(usng.circle.radius, usng.circle.units)
      wkt = computeCircle(
        usngGridToWktPoint(usng.circle.point),
        distance,
        36
      ).toWkt()
      break
    case 'line':
      if (usng.line.list.length > 0) {
        usng.line.list.map(grid => points.push(usngGridToWktPoint(grid)))
        wkt = new wkx.LineString(points).toWkt()
      }
      break
    case 'polygon':
      if (usng.polygon.list.length > 0) {
        usng.polygon.list.map(grid => points.push(usngGridToWktPoint(grid)))
        const p1 = points[0]
        const p2 = points[points.length - 1]
        if (p1.x !== p2.x || p1.y !== p2.y) {
          points.push(new wkx.Point(p1.x, p1.y))
        }
        wkt = new wkx.Polygon(points).toWkt()
      }
      break
    case 'boundingbox':
      const grid = converter.isUSNG(usng.boundingbox)
      const bbox = converter.USNGtoLL(grid, false)
      const minLon = bbox.west
      const minLat = bbox.south
      const maxLon = bbox.east
      const maxLat = bbox.north
      const nw = new wkx.Point(minLon, maxLat)
      const ne = new wkx.Point(maxLon, maxLat)
      const se = new wkx.Point(maxLon, minLat)
      const sw = new wkx.Point(minLon, minLat)
      wkt = new wkx.Polygon([nw, ne, se, sw, nw]).toWkt()
      break
  }
  return wkt
}

/*
 *  USNG/MGRS validation utils
 */
function validateUsng(usng) {
  if (inputIsBlank(usng)) {
    return { valid: true, error: null }
  }

  let valid = true
  let error = null
  switch (usng.shape) {
    case 'point':
      if (!validateUsngGrid(usng.point)) {
        valid = false
        error = errorMessages.invalidUsngGrid
      }
      break
    case 'circle':
      const radius = parseFloat(usng.circle.radius)
      if (
        isNaN(radius) ||
        radius <= 0 ||
        toKilometers(radius, usng.circle.units) > 10000
      ) {
        valid = false
        error = errorMessages.invalidRadius
      } else if (!validateUsngGrid(usng.circle.point)) {
        valid = false
        error = errorMessages.invalidUsngGrid
      }
      break
    case 'line':
      if (!usng.line.list.every(validateUsngGrid)) {
        valid = false
        error = errorMessages.invalidList
      } else if (usng.line.list.length < 2) {
        valid = false
        error = errorMessages.tooFewPointsLine
      }
      break
    case 'polygon':
      if (!usng.polygon.list.every(validateUsngGrid)) {
        valid = false
        error = errorMessages.invalidList
      } else if (usng.line.list.length < 3) {
        valid = false
        error = errorMessages.tooFewPointsPolygon
      }
      break
    case 'boundingbox':
      if (!validateUsngGrid(usng.boundingbox)) {
        valid = false
        error = errorMessages.invalidUsngGrid
      }
      break
  }
  return { valid, error }
}

module.exports = {
  usngToWkt,
  validateUsng,
  validateUsngGrid,
}
