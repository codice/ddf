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
const { validateWkt, roundWktCoords } = require('./wkt-utils')
const { ddToWkt, validateDd, validateDdPoint } = require('./dd-utils')
const {
  dmsToWkt,
  validateDms,
  validateDmsPoint,
  dmsCoordinateToDD,
  parseDmsCoordinate,
  ddToDmsCoordinateLat,
  ddToDmsCoordinateLon,
  getSecondsPrecision,
  Direction,
} = require('./dms-utils')
const { usngToWkt, validateUsng, validateUsngGrid } = require('./usng-utils')
const errorMessages = require('./errors')

module.exports = {
  validateWkt,
  roundWktCoords,
  validateDd,
  validateDdPoint,
  validateDms,
  validateDmsPoint,
  validateUsng,
  validateUsngGrid,
  ddToWkt,
  ddToDmsCoordinateLat,
  ddToDmsCoordinateLon,
  parseDmsCoordinate,
  dmsCoordinateToDD,
  dmsToWkt,
  usngToWkt,
  errorMessages,
  getSecondsPrecision,
  Direction,
}
