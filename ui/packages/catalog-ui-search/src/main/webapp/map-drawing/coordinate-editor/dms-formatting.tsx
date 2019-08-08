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
type DMS = {
  degree: number
  minute: number
  second: number
}

const SECONDS_PRECISION = 1

const dmsValueToString = (dms: DMS, isLongitude: boolean): string => {
  const { degree, minute, second } = dmsSetSign(dms, 1)
  const heading =
    dmsSign(dms) < 0 ? (isLongitude ? 'W' : 'S') : isLongitude ? 'E' : 'N'
  return `${degree.toFixed(0)}\xB0 ${minute.toFixed(0)}' ${second.toFixed(
    SECONDS_PRECISION
  )}" ${heading}`
}

const dmsCoordinateToString = (lat: DMS, lon: DMS): string =>
  `${dmsValueToString(lat, false)} ${dmsValueToString(lon, true)}`

const dmsToDecimal = ({ degree, minute, second }: DMS): number =>
  (Math.abs(degree) + Math.abs(minute) / 60.0 + Math.abs(second) / 3600.0) *
  dmsSign({ degree, minute, second })

const decimalToDMS = (decimal: number): DMS => {
  const absoluteValue = Math.abs(decimal)
  const degree = Math.floor(absoluteValue)
  const minute = Math.floor((absoluteValue - degree) * 60)
  const second = (absoluteValue - degree - minute / 60) * 3600
  const sign = decimal < 0 ? -1 : 1
  return {
    degree: degree * sign,
    minute: minute,
    second: second,
  }
}

const dmsSign = ({ degree, minute, second }: DMS): number =>
  degree < 0 || minute < 0 || second < 0 ? -1 : 1

const dmsSetSign = ({ degree, minute, second }: DMS, sign: -1 | 1): DMS => ({
  degree: Math.abs(degree) * sign,
  minute: Math.abs(minute) * sign,
  second: Math.abs(second) * sign,
})

export {
  DMS,
  dmsValueToString,
  dmsCoordinateToString,
  dmsToDecimal,
  decimalToDMS,
  dmsSign,
  dmsSetSign,
  SECONDS_PRECISION,
}
