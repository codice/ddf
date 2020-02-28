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
const React = require('react')
const MaskedTextField = require('./masked-text-field')

import styled from 'styled-components'

const dmsRegex = new RegExp('^([0-9]*)°([0-9]*)\'([0-9]*\\.?[0-9]*)"$')
const LAT_DEGREES_DIGITS = 2
const LON_DEGREES_DIGITS = 3
const DEFAULT_SECONDS_PRECISION = 4

const Direction = Object.freeze({
  North: 'N',
  South: 'S',
  East: 'E',
  West: 'W',
})

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

function padCoordinate(coordinate) {
  if (coordinate === undefined) {
    return coordinate
  }
  const matches = dmsRegex.exec(coordinate)
  if (!matches) {
    return coordinate
  }
  let deg = matches[1]
  let min = matches[2]
  let sec = matches[3]
  deg = padComponent(deg)
  min = padComponent(min)
  sec = padComponent(sec)
  return deg + '°' + min + "'" + sec + '"'
}

function padComponent(numString = '') {
  while (numString.includes('_')) {
    if (numString.includes('.')) {
      numString = numString.replace('_', '0')
    } else {
      numString = numString.replace('_', '')
      numString = '0' + numString
    }
  }
  return numString
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

const decimalMask = ['.', /\d/, /\d/, /\d/, '"']

function latitudeDMSMask(rawValue) {
  const baseMask = [/\d/, /\d/, '°', /\d/, /\d/, "'", /\d/, /\d/]

  const pattern = new RegExp(
    '^[0-9_]{2,3}[°*][0-9_]{2,3}[`\'’]([0-9_]{2,3}(?:[.][0-9]{0,3})?)"?'
  )
  const match = rawValue.match(pattern)
  if (match) {
    const seconds = match[1]
    if (seconds.includes('.')) {
      return baseMask.concat(decimalMask)
    }
  }
  return baseMask.concat('"')
}

function longitudeDMSMask(rawValue) {
  const baseMask = [/\d/, /\d/, /\d/, '°', /\d/, /\d/, "'", /\d/, /\d/]

  const pattern = new RegExp(
    '^[0-9_]{3,4}[°*][0-9_]{2,3}[`\'’]([0-9_]{2,3}(?:[.][0-9]{0,3})?)"?'
  )
  const match = rawValue.match(pattern)
  if (match) {
    const seconds = match[1]
    if (seconds.includes('.')) {
      return baseMask.concat(decimalMask)
    }
  }
  return baseMask.concat('"')
}

const Root = styled.div`
  display: flex;
  flex: 1;
`

const MaskedCoordinate = props => {
  const { placeholder, mask, value, onChange, children, ...otherProps } = props
  return (
    <Root>
      <MaskedTextField
        placeholder={placeholder}
        mask={mask}
        value={value}
        onChange={onChange}
        {...otherProps}
      />
      {children}
    </Root>
  )
}

const DmsLatitude = props => {
  return (
    <MaskedCoordinate
      placeholder="dd°mm'ss.sss&quot;"
      mask={latitudeDMSMask}
      placeholderChar="_"
      {...props}
      onBlur={event => {
        props.onChange(padCoordinate(props.value), event.type)
      }}
    />
  )
}

const DmsLongitude = props => {
  return (
    <MaskedCoordinate
      placeholder="ddd°mm'ss.sss&quot;"
      mask={longitudeDMSMask}
      placeholderChar="_"
      {...props}
      onBlur={event => {
        props.onChange(padCoordinate(props.value), event.type)
      }}
    />
  )
}

module.exports = {
  dmsCoordinateToDD,
  parseDmsCoordinate,
  ddToDmsCoordinateLat,
  ddToDmsCoordinateLon,
  getSecondsPrecision,
  Direction,
  DmsLongitude,
  DmsLatitude,
}
