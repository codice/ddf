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

function degreesToRadians(degrees) {
  return (degrees * Math.PI) / 180
}

function radiansToDegrees(radians) {
  return (radians * 180) / Math.PI
}

/*
 * Constants used for the calculations below:
 * R is Earth's approximate radius. Assumes a perfect circle, which will produce at most 0.5% error
 */
const R = 6371.01

/*
 * Given a starting point, initial bearing, and distance travelled, returns the destination point
 * reached by travelling the given distance along a great circle arc at that bearing.
 * Reference: https://www.movable-type.co.uk/scripts/latlong.html#destPoint
 * @param point: wkx Point
 * @param bearing: degrees from north
 * @param distance: kilometers
 */
function computeDestination(point, bearing, distance) {
  if (distance < 0) {
    return null
  }

  const lat1 = degreesToRadians(point.y)
  const lon1 = degreesToRadians(point.x)
  const radBearing = degreesToRadians(bearing)
  const radDistance = distance / R

  let lat2 = Math.asin(
    Math.sin(lat1) * Math.cos(radDistance) +
      Math.cos(lat1) * Math.sin(radDistance) * Math.cos(radBearing)
  )
  let lon2 =
    lon1 +
    Math.atan2(
      Math.sin(radBearing) * Math.sin(radDistance) * Math.cos(lat1),
      Math.cos(radDistance) - Math.sin(lat1) * Math.sin(lat2)
    )
  if (isNaN(lat2) || isNaN(lon2)) {
    return null
  }

  lat2 = radiansToDegrees(lat2)
  lon2 = radiansToDegrees(lon2)
  if (lon2 > 180 || lon2 < -180) {
    lon2 = ((lon2 + 540) % 360) - 180
  }
  return new wkx.Point(lon2, lat2)
}

/*
 * TODO: Use Spatial4j buffered point, e.g. BUFFER(POINT(0 0), 10), instead of approximating circle
 * Given a point and distance, returns an n-point polygon approximating a circle surrounding the
 * point with radius equal to the input distance.
 * @param point: wkx Point
 * @param distance: kilometers
 * @param n: number of points used to approximate the circle
 */
function computeCircle(point, distance, n) {
  if (distance < 0 || n < 0) {
    return null
  }

  const points = []
  for (let i = 0; i < n; i++) {
    points.push(computeDestination(point, (360 * i) / n, distance))
  }
  points.push(points[0])
  return new wkx.Polygon(points)
}

/*
 * Converts the given distance to kilometers. All conversions are exact. Note that the
 * international definition for nautical mile is used (1 nautical mile = 1852 meters).
 * Reference: https://www.sfei.org/it/gis/map-interpretation/conversion-constants
 */
function toKilometers(distance, units) {
  switch (units) {
    case 'meters':
      return distance / 1000
    case 'kilometers':
      return distance
    case 'feet':
      return distance * 0.0003048
    case 'yards':
      return distance * 0.0009144
    case 'miles':
      return distance * 1.609344
    case 'nautical miles':
      return distance * 1.852
  }
}

module.exports = {
  computeCircle,
  toKilometers,
}
