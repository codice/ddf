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
/*global define*/
/*jshint bitwise: false*/

var EARTH_MEAN_RADIUS_METERS = 6371008.7714

var DEGREES_TO_RADIANS = Math.PI / 180
var RADIANS_TO_DEGREES = 1 / DEGREES_TO_RADIANS

var METERS_KILOMETERS = 1000
var METERS_FEET = 0.3048
var METERS_YARDS = 0.9144
var METERS_MILES = 1609.344
var METERS_NAUTICAL_MILES = 1852

/* 6 Digits of precision used because it gives precision up to 
0.11 meters, which was deemed precise enough for any use case 
of application */
var DECIMAL_PRECISION = 6

module.exports = {
  distToDegrees: function(distanceInMeters) {
    return this.toDegrees(this.distToRadians(distanceInMeters))
  },
  distToRadians: function(distanceInMeters) {
    return distanceInMeters / EARTH_MEAN_RADIUS_METERS
  },
  toDegrees: function(distanceInRadians) {
    return distanceInRadians * RADIANS_TO_DEGREES
  },
  getDistanceInMeters: function(distance, units) {
    distance = distance || 0
    switch (units) {
      case 'kilometers':
        return distance * METERS_KILOMETERS
      case 'feet':
        return distance * METERS_FEET
      case 'yards':
        return distance * METERS_YARDS
      case 'miles':
        return distance * METERS_MILES
      case 'nautical miles':
        return distance * METERS_NAUTICAL_MILES
      case 'meters':
      default:
        return distance
    }
  },
  getDistanceFromMeters: function(distance, units) {
    distance = distance || 0
    switch (units) {
      case 'kilometers':
        return distance / METERS_KILOMETERS
      case 'feet':
        return distance / METERS_FEET
      case 'yards':
        return distance / METERS_YARDS
      case 'miles':
        return distance / METERS_MILES
      case 'nautical miles':
        return distance / METERS_NAUTICAL_MILES
      case 'meters':
      default:
        return distance
    }
  },
  altitudeRound: function(value) {
    // round the value, don't need picometer precision.
    return Math.round(value)
  },
  coordinateRound: function(value) {
    return parseFloat(parseFloat(value).toFixed(DECIMAL_PRECISION))
  },
}
