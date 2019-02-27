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
const metacardDefinitions = require('../../../component/singletons/metacard-definitions')
const mtgeo = require('mt-geo')
const usngs = require('usng.js')
const Common = require('js/Common')

const converter = new usngs.Converter()
const usngPrecision = 6

import { Attribute, Coordinates, Props } from '.'
import { validCoordinates } from './map-info'

export const formatAttribute = ({ name, value }: Attribute): string => {
  const isDate = metacardDefinitions.metacardTypes[name].type === 'DATE'
  return `${name.toUpperCase()}: ${
    isDate ? Common.getHumanReadableDateTime(value) : value
  }`
}

export const formatCoordinates = ({ lat, lon, format }: Props): any => {
  const withFormat = {
    degrees: () => `${mtgeo.toLat(lat)} ${mtgeo.toLon(lon)}`,
    decimal: () => decimalComponent({ lat, lon }),
    mgrs: () =>
      lat > 84 || lat < -80
        ? 'In UPS Space'
        : converter.LLtoUSNG(lat, lon, usngPrecision),
    utm: () => converter.LLtoUTMUPS(lat, lon),
  }
  if (!(format in withFormat)) {
    throw `Unrecognized coordinate format value [${format}]`
  }

  return validCoordinates({ lat, lon })
    ? (withFormat as any)[format]()
    : undefined
}

const decimalComponent = ({ lat, lon }: Coordinates) => {
  const numPlaces = 6
  const latPadding = numPlaces + 4
  const lonPadding = numPlaces + 5

  const formattedLat = lat
    .toFixed(numPlaces)
    .toString()
    .padStart(latPadding, ' ')

  const formattedLon = lon
    .toFixed(numPlaces)
    .toString()
    .padStart(lonPadding, ' ')

  return `${formattedLat} ${formattedLon}`
}
