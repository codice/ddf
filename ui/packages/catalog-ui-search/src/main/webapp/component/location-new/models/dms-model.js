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
const { Direction } = require('../utils/dms-utils')

const dmsLatitude = {
  coordinate: '',
  direction: Direction.North,
}

const dmsLongitude = {
  coordinate: '',
  direction: Direction.East,
}

const dmsPoint = {
  latitude: { ...dmsLatitude },
  longitude: { ...dmsLongitude },
}

const dmsModel = {
  shape: 'point',
  point: { ...dmsPoint },
  circle: {
    point: { ...dmsPoint },
    radius: '1',
    units: 'meters',
  },
  line: {
    list: [],
  },
  polygon: {
    list: [],
  },
  boundingbox: {
    north: { ...dmsLatitude },
    south: { ...dmsLatitude },
    east: { ...dmsLongitude },
    west: { ...dmsLongitude },
  },
}

module.exports = {
  dmsPoint,
  dmsModel,
}
