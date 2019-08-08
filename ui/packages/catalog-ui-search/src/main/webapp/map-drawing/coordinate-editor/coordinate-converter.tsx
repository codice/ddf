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
import { DMS, decimalToDMS } from './dms-formatting'
import { UTM } from './utm-formatting'
import { Converter } from 'usng.js'

const DECIMAL_DEGREES_PRECISION = 6
const USNG_CONVERSION_PRECISION = 6
const UnitConverter = new (Converter as any)()

type LatLonDMS = {
  lat: DMS
  lon: DMS
}

type LatLonDD = {
  lat: number
  lon: number
}

type CoordinateValue = LatLonDMS | LatLonDD | string | UTM

const latLonTo = {
  LatLonDD: (lat: number, lon: number): LatLonDD => ({
    lat,
    lon,
  }),
  LatLonDMS: (lat: number, lon: number): LatLonDMS => ({
    lat: decimalToDMS(lat),
    lon: decimalToDMS(lon),
  }),
  USNGBox: (north: number, south: number, east: number, west: number): string =>
    UnitConverter.LLBboxtoUSNG(north, south, east, west),
  USNG: (lat: number, lon: number, precision: number): string =>
    UnitConverter.LLtoUSNG(lat, lon, precision),
  UTM: (lat: number, lon: number): UTM => {
    const {
      easting,
      northing,
      zoneNumber,
      northPole,
    } = UnitConverter.LLtoUTMUPSObject(lat, lon)
    return {
      easting,
      northing,
      zone: zoneNumber,
      hemisphere: northPole ? 'N' : 'S',
    }
  },
}

export {
  LatLonDMS,
  LatLonDD,
  CoordinateValue,
  latLonTo,
  USNG_CONVERSION_PRECISION,
  DECIMAL_DEGREES_PRECISION,
}
