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
import { InvalidSearchFormMessage } from '../../../component/announcement/CommonMessages'
import styled from 'styled-components'
const announcement = require('../../../component/announcement/index.jsx')
const {
  validateInput,
} = require('../../../component/location-new/utils/dms-utils')
const usngs = require('usng.js')
const converter = new usngs.Converter()
const northingOffset = 10000000
const latitude = 'latitude'
const longitude = 'longitude'
interface ErrorState {
  error: boolean
  message: string
}

export function showErrorMessages(errors: any) {
  if (errors.length === 0) {
    return
  }
  let searchErrorMessage = JSON.parse(JSON.stringify(InvalidSearchFormMessage))
  if (errors.length > 1) {
    let msg = searchErrorMessage.message
    searchErrorMessage.title =
      'Your search cannot be run due to multiple errors'
    const formattedErrors = errors.map(
      (error: any) => `\u2022 ${error.title}: ${error.body}`
    )
    searchErrorMessage.message = msg.concat(formattedErrors)
  } else {
    const error = errors[0]
    searchErrorMessage.title = error.title
    searchErrorMessage.message = error.body
  }
  announcement.announce(searchErrorMessage)
}

export function getFilterErrors(filters: any) {
  const errors = new Set()
  let geometryErrors = new Set<string>()
  for (let i = 0; i < filters.length; i++) {
    const filter = filters[i]
    getGeometryErrors(filter).forEach(function(msg) {
      geometryErrors.add(msg)
    })
  }
  geometryErrors.forEach(function(err) {
    errors.add({
      title: 'Invalid geometry filter',
      body: err,
    })
  })
  return Array.from(errors)
}

export function validateGeo(
  key: string,
  value: string,
  value1?: any,
  value2?: any,
  value3?: any
) {
  switch (key) {
    case 'lat':
      return validateDDLatLon(latitude, 90, value)
    case 'lon':
      return validateDDLatLon(longitude, 180, value)
    case 'dmsLat':
      return validateDmsLatLon(latitude, value)
    case 'dmsLon':
      return validateDmsLatLon(longitude, value)
    case 'usng':
      return validateUsng(value)
    case 'utmUpsEasting':
    case 'utmUpsNorthing':
    case 'utmUpsZone':
    case 'utmUpsHemisphere':
      return validateUtmUps(key, value, value1, value2, value3)
    case 'radius':
    case 'lineWidth':
      return validateRadiusLineBuffer(key, value)
    default:
  }
}

export function getErrorComponent(errorState: ErrorState) {
  return errorState.error ? (
    <Invalid>
      <WarningIcon className="fa fa-warning" />
      <span>{errorState.message}</span>
    </Invalid>
  ) : null
}

export function validateListOfPoints(coordinates: any[], mode: string) {
  let message = ''
  const isLine = mode.includes('line')
  const numPoints = isLine ? 2 : 4
  if (
    !mode.includes('multi') &&
    !coordinates.some(coords => coords.length > 2) &&
    coordinates.length < numPoints
  ) {
    message = `Minimum of ${numPoints} points needed for ${
      isLine ? 'Line' : 'Polygon'
    }`
  }
  coordinates.forEach(coordinate => {
    if (coordinate.length > 2) {
      coordinate.forEach((coord: any) => {
        if (hasPointError(coord))
          message = JSON.stringify(coord) + ' is not a valid point.'
      })
    } else {
      if (mode.includes('multi')) {
        message = `Switch to ${isLine ? 'Line' : 'Polygon'}`
      } else if (hasPointError(coordinate)) {
        message = JSON.stringify(coordinate) + ' is not a valid point.'
      }
    }
  })
  return { error: message.length > 0, message }
}

export function validateLinePolygon(currentValue: string, mode: string) {
  if (!is2DArray(currentValue)) {
    return { error: true, message: 'Not an acceptable value' }
  }
  try {
    const pointsValid = validateListOfPoints(JSON.parse(currentValue), mode) 
    return pointsValid === undefined ? initialErrorState : pointsValid
  } catch (e) {
    return initialErrorState
  }
}

export const initialErrorState = {
  error: false,
  message: '',
}

export const initialErrorStateWithDefault = {
  error: false,
  message: '',
  defaultValue: '',
}

function is2DArray(coordinates: string) {
  try {
    const parsedCoords = JSON.parse(coordinates)
    return Array.isArray(parsedCoords) && Array.isArray(parsedCoords[0])
  } catch (e) {
    return false
  }
}

function hasPointError(point: any[]) {
  if (
    point.length !== 2 ||
    (Number.isNaN(Number.parseFloat(point[0])) &&
      Number.isNaN(Number.parseFloat(point[1])))
  ) {
    return true
  } else if (
    point[0] > 180 ||
    point[0] < -180 ||
    point[1] > 90 ||
    point[1] < -90
  ) {
    return true
  }
  return false
}

function getGeometryErrors(filter: any): Set<string> {
  const geometry = filter.geojson && filter.geojson.geometry
  const bufferWidth =
    filter.geojson.properties.buffer && filter.geojson.properties.buffer.width
  const errors = new Set<string>()
  if (!geometry) {
    return errors
  }
  switch (filter.geojson.properties.type) {
    case 'Polygon':
      if (geometry.coordinates[0].length < 4) {
        errors.add(
          'Polygon coordinates must be in the form [[x,y],[x,y],[x,y],[x,y], ... ]'
        )
      }
      break
    case 'LineString':
      if (geometry.coordinates.length < 2) {
        errors.add('Line coordinates must be in the form [[x,y],[x,y], ... ]')
      }
      if (!bufferWidth || bufferWidth == 0) {
        errors.add('Line buffer width must be greater than 0.000001')
      }
      break
    case 'Point':
      if (!bufferWidth || bufferWidth < 0.000001) {
        errors.add('Radius must be greater than 0.000001')
      }
      if (
        geometry.coordinates.some(
          (coord: any) => !coord || coord.toString().length == 0
        )
      ) {
        errors.add('Coordinates must not be empty')
      }
      break
    case 'BoundingBox':
      const box = filter.geojson.properties
      if (!box.east || !box.west || !box.north || !box.south) {
        errors.add('Bounding box must have valid values')
      }
      break
  }
  return errors
}

function validateDDLatLon(label: string, defaultCoord: number, value: string) {
  let message = ''
  let defaultValue
  if (value !== undefined && value.length === 0) {
    message = getEmptyErrorMessage(label)
    return { error: true, message, defaultValue }
  }
  if (Number(value) > defaultCoord || Number(value) < -1 * defaultCoord) {
    defaultValue = Number(value) > 0 ? defaultCoord : -1 * defaultCoord
    message = getDefaultingErrorMessage(value, label, defaultValue)
    return { error: true, message, defaultValue }
  }
  return { error: false, message, defaultValue }
}

function validateDmsLatLon(label: string, value: string) {
  let message = ''
  let defaultValue
  const validator = label === latitude ? 'dd°mm\'ss.s"' : 'ddd°mm\'ss.s"'
  if (value !== undefined && value.length === 0) {
    message = getEmptyErrorMessage(label)
    return { error: true, message, defaultValue }
  }
  if (validateInput(value, validator) !== value) {
    defaultValue = validateInput(value, validator)
    message = getDefaultingErrorMessage(value, label, defaultValue)
    return { error: true, message, defaultValue }
  }
  return { error: false, message, defaultValue }
}

function validateUsng(value: string) {
  if (value === '') {
    return { error: true, message: 'USNG / MGRS coordinates cannot be empty' }
  }
  const result = converter.USNGtoLL(value, true)
  const isInvalid = Number.isNaN(result.lat) || Number.isNaN(result.lon)
  return {
    error: isInvalid,
    message: isInvalid ? 'Invalid USNG / MGRS coordinates' : '',
  }
}

function upsValidDistance(distance: number) {
  return distance >= 800000 && distance <= 3200000
}

function validateLatLon(lat: string, lon: string) {
  const latitude = parseFloat(lat)
  const longitude = parseFloat(lon)
  return latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180
}

function validateUtmUps(
  key: string,
  utmUpsEasting: string,
  utmUpsNorthing: string,
  zoneNumber: any,
  hemisphere: any
) {
  let error = { error: false, message: '' }
  zoneNumber = Number.parseInt(zoneNumber)
  // Number('') returns 0, so we can't just blindly cast to number
  // since we want to differentiate '' from 0
  let easting = utmUpsEasting === '' ? NaN : Number(utmUpsEasting)
  let northing = utmUpsNorthing === '' ? NaN : Number(utmUpsNorthing)
  if (!isNaN(easting)) {
    easting = Number.parseFloat(utmUpsEasting)
  }
  if (!isNaN(northing)) {
    northing = Number.parseFloat(utmUpsEasting)
  }
  const northernHemisphere = hemisphere.toUpperCase() === 'NORTHERN'
  const isUps = zoneNumber === 0
  const utmUpsParts = {
    easting,
    northing,
    zoneNumber,
    hemisphere,
    northPole: northernHemisphere,
  }
  utmUpsParts.northing =
    isUps || northernHemisphere ? northing : northing - northingOffset
  // These checks are to ensure that we only mark a value as "invalid" 
  // if the user has entered something already
  const isNorthingInvalid =
    isNaN(utmUpsParts.northing) && utmUpsNorthing !== undefined
  const isEastingInvalid =
    isNaN(utmUpsParts.easting) && utmUpsEasting !== undefined
  let { lat, lon } = converter.UTMUPStoLL(utmUpsParts)
  lon = lon % 360
  if (lon < -180) {
    lon = lon + 360
  }
  if (lon > 180) {
    lon = lon - 360
  }
  // we want to validate using the validate lat lon method, but only if they're both defined
  // if one or more is undefined, we want to return true
  const isLatLonValid =
    validateLatLon(lat, lon) ||
    (utmUpsNorthing === undefined || utmUpsEasting === undefined)
  if ((isNorthingInvalid && isEastingInvalid) || !isLatLonValid) {
    error = { error: true, message: 'Invalid UTM/UPS coordinates' }
  } else if (
    key === 'utmUpsNorthing' &&
    isNaN(utmUpsParts.northing) &&
    utmUpsNorthing !== undefined
  ) {
    error = { error: true, message: 'Northing value is invalid' }
  } else if (
    key === 'utmUpsEasting' &&
    isNaN(utmUpsParts.easting) &&
    utmUpsEasting !== undefined
  ) {
    error = { error: true, message: 'Easting value is invalid' }
  } else if (
    isUps &&
    (!upsValidDistance(northing) || !upsValidDistance(easting))
  ) {
    error = { error: true, message: 'Invalid UPS distance' }
  }
  return error
}

function validateRadiusLineBuffer(key: string, value: string) {
  const label = key === 'lineWidth' ? 'Buffer ' : 'Radius '
  if ((value !== undefined && value.length === 0) || Number(value) < 0.000001) {
    return {
      error: true,
      message: label + 'cannot be less than 0.000001',
    }
  }
  return { error: false, message: '' }
}

function getDefaultingErrorMessage(
  value: string,
  label: string,
  defaultValue: number
) {
  return `${value.replace(
    /_/g,
    '0'
  )} is not an acceptable ${label} value. Defaulting to ${defaultValue}`
}

function getEmptyErrorMessage(label: string) {
  return `${label.replace(/^\w/, c => c.toUpperCase())} cannot be empty`
}

const Invalid = styled.div`
  background-color: ${props => props.theme.negativeColor};
  height: 100%;
  display: block;
  overflow: hidden;
  color: white;
`

const WarningIcon = styled.span`
  padding: ${({ theme }) => theme.minimumSpacing};
`
