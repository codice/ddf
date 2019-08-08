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
import { UTM } from './utm-formatting'
import { DMS } from './dms-formatting'
import { Extent } from '../geometry'

type Position = 'north' | 'south' | 'east' | 'west'

type BBox = Extent

type LatLonBBox = { [Key in Position]: number }

type LatLonDMSBBox = { [Key in Position]: DMS }

type USNGBBox = string

type UTMBBox = {
  upperLeft: UTM
  lowerRight: UTM
}

type CoordinateValue = LatLonBBox | LatLonDMSBBox | USNGBBox | UTMBBox

const Indexes = {
  north: 3,
  south: 1,
  west: 0,
  east: 2,
}

export {
  BBox,
  CoordinateValue,
  Indexes,
  LatLonBBox,
  LatLonDMSBBox,
  Position,
  USNGBBox,
  UTMBBox,
}
