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
const _ = require('underscore')
const metacardDefinitions = require('../component/singletons/metacard-definitions.js')
const Terraformer = require('terraformer')
const TerraformerWKTParser = require('terraformer-wkt-parser')
const CQLUtils = require('./CQLUtils.js')
const Turf = require('@turf/turf')
const wkx = require('wkx')
const moment = require('moment')
const cql = require('./cql.js')

// strip extra quotes
const stripQuotes = value => {
  return value.replace(/^"(.+(?="$))"$/, '$1')
}

const getDurationFromRelativeValue = value => {
  return value.substring(9, value.length - 1)
}

const polygonStringToCoordinates = polygonString => {
  try {
    return polygonString
      .substring('POLYGON(('.length, polygonString.length - '))'.length)
      .split(',')
      .map(stringPair => {
        const pair = stringPair.split(' ')
        return [Number(pair[0]), Number(pair[1])]
      })
  } catch (error) {
    console.error(error)
    return []
  }
}

const createBufferedPolygon = (coordinates, distance) =>
  Turf.buffer(Turf.lineString(coordinates), Math.max(distance, 1), 'meters')

function checkTokenWithWildcard(token, filter) {
  const filterRegex = new RegExp(filter.split('*').join('.*'))
  return filterRegex.test(token)
}

function checkToken(token, filter) {
  if (filter.indexOf('*') >= 0) {
    return checkTokenWithWildcard(token, filter)
  } else if (token === filter) {
    return true
  }
  return false
}

function matchesILIKE(value, filter) {
  const valueToCheckFor = filter.value.toLowerCase()
  value = value.toString().toLowerCase()
  const tokens = value.split(' ')
  for (let i = 0; i <= tokens.length - 1; i++) {
    if (checkToken(tokens[i], valueToCheckFor)) {
      return true
    }
  }
  return false
}

function matchesLIKE(value, filter) {
  const valueToCheckFor = filter.value
  const tokens = value.toString().split(' ')
  for (let i = 0; i <= tokens.length - 1; i++) {
    if (checkToken(tokens[i], valueToCheckFor)) {
      return true
    }
  }
  return false
}

function matchesEQUALS(value, filter) {
  const valueToCheckFor = filter.value
  if (value.toString() === valueToCheckFor.toString()) {
    return true
  }
  return false
}

function matchesNOTEQUALS(value, filter) {
  const valueToCheckFor = filter.value
  if (value.toString() !== valueToCheckFor.toString()) {
    return true
  }
  return false
}

function matchesGreaterThan(value, filter) {
  const valueToCheckFor = filter.value
  if (value > valueToCheckFor) {
    return true
  }
  return false
}

function matchesGreaterThanOrEqualTo(value, filter) {
  const valueToCheckFor = filter.value
  if (value >= valueToCheckFor) {
    return true
  }
  return false
}

function matchesLessThan(value, filter) {
  const valueToCheckFor = filter.value
  if (value < valueToCheckFor) {
    return true
  }
  return false
}

function matchesLessThanOrEqualTo(value, filter) {
  const valueToCheckFor = filter.value
  if (value <= valueToCheckFor) {
    return true
  }
  return false
}

// terraformer doesn't offically support Point, MultiPoint, FeatureCollection, or GeometryCollection
// terraformer incorrectly supports MultiPolygon, so turn it into a Polygon first
function intersects(terraformerObject, value) {
  let intersected = false
  switch (value.type) {
    case 'Point':
      return terraformerObject.contains(value)
    case 'MultiPoint':
      value.coordinates.forEach(coordinate => {
        intersected =
          intersected ||
          intersects(terraformerObject, {
            type: 'Point',
            coordinates: coordinate,
          })
      })
      return intersected
    case 'LineString':
    case 'MultiLineString':
    case 'Polygon':
      return terraformerObject.intersects(value)
    case 'MultiPolygon':
      value.coordinates.forEach(coordinate => {
        intersected =
          intersected ||
          intersects(terraformerObject, {
            type: 'Polygon',
            coordinates: coordinate,
          })
      })
      return intersected
    case 'Feature':
      return intersects(terraformerObject, value.geometry)
    case 'FeatureCollection':
      value.features.forEach(feature => {
        intersected = intersected || intersects(terraformerObject, feature)
      })
      return intersected
    case 'GeometryCollection':
      value.geometries.forEach(geometry => {
        intersected = intersected || intersects(terraformerObject, geometry)
      })
      return intersected
    default:
      return intersected
  }
}

function matchesPOLYGON(value, filter) {
  const polygonToCheck = TerraformerWKTParser.parse(filter.value.value)
  if (intersects(polygonToCheck, value)) {
    return true
  }
  return false
}

const matchesBufferedPOLYGON = (value, filter) => {
  const bufferedPolygon = createBufferedPolygon(
    polygonStringToCoordinates(filter.value.value),
    filter.distance
  )
  const teraformedPolygon = new Terraformer.Polygon({
    type: 'Polygon',
    coordinates: bufferedPolygon.geometry.coordinates,
  })
  return intersects(teraformedPolygon, value)
}

function matchesCIRCLE(value, filter) {
  if (filter.distance <= 0) {
    return false
  }
  const points = filter.value.value
    .substring(6, filter.value.value.length - 1)
    .split(' ')
  const circleToCheck = new Terraformer.Circle(points, filter.distance, 64)
  const polygonCircleToCheck = new Terraformer.Polygon(circleToCheck.geometry)
  if (intersects(polygonCircleToCheck, value)) {
    return true
  }
  return false
}

function matchesLINESTRING(value, filter) {
  let pointText = filter.value.value.substring(11)
  pointText = pointText.substring(0, pointText.length - 1)
  const lineWidth = filter.distance || 0
  if (lineWidth <= 0) {
    return false
  }
  const line = pointText
    .split(',')
    .map(coordinate => coordinate.split(' ').map(value => Number(value)))
  const turfLine = Turf.lineString(line)
  const bufferedLine = Turf.buffer(turfLine, lineWidth, 'meters')
  const polygonToCheck = new Terraformer.Polygon({
    type: 'Polygon',
    coordinates: bufferedLine.geometry.coordinates,
  })
  if (intersects(polygonToCheck, value)) {
    return true
  }
  return false
}

function matchesBEFORE(value, filter) {
  const date1 = moment(value)
  const date2 = moment(filter.value)
  if (date1 <= date2) {
    return true
  }
  return false
}

function matchesAFTER(value, filter) {
  const date1 = moment(value)
  const date2 = moment(filter.value)
  if (date1 >= date2) {
    return true
  }
  return false
}

function matchesRelative(value, filter) {
  const date1 = moment(value)
  const date2 = moment().subtract(
    moment.duration(getDurationFromRelativeValue(filter.value))
  )
  if (date1 >= date2) {
    return true
  }
  return false
}

function matchesDURING(value, filter) {
  return (
    filter.from && filter.to && moment(value).isBetween(filter.from, filter.to)
  )
}

/*
    Because the relative and = matchers use the same comparator we need to differentiate them by type
*/
function determineEqualsMatcher(filter) {
  if (
    metacardDefinitions.metacardTypes[stripQuotes(filter.property)].type ===
    'DATE'
  ) {
    return matchesRelative
  } else {
    return matchesEQUALS
  }
}

function flattenMultivalueProperties(valuesToCheck) {
  return _.flatten(valuesToCheck, true)
}

function matchesFilter(metacard, filter) {
  if (!filter.filters) {
    let valuesToCheck = []
    if (
      metacardDefinitions.metacardTypes[filter.property] &&
      metacardDefinitions.metacardTypes[filter.property].type === 'GEOMETRY'
    ) {
      filter.property = 'anyGeo'
    }
    switch (filter.property) {
      case '"anyText"':
        valuesToCheck = Object.keys(metacard.properties)
          .filter(
            property =>
              Boolean(metacardDefinitions.metacardTypes[property]) &&
              metacardDefinitions.metacardTypes[property].type === 'STRING'
          )
          .map(property => metacard.properties[property])
        break
      case 'anyGeo':
        valuesToCheck = Object.keys(metacard.properties)
          .filter(
            property =>
              Boolean(metacardDefinitions.metacardTypes[property]) &&
              metacardDefinitions.metacardTypes[property].type === 'GEOMETRY'
          )
          .map(
            property =>
              new Terraformer.Primitive(
                wkx.Geometry.parse(metacard.properties[property]).toGeoJSON()
              )
          )
        break
      default:
        const valueToCheck =
          metacard.properties[filter.property.replace(/['"]+/g, '')]
        if (valueToCheck !== undefined) {
          valuesToCheck.push(valueToCheck)
        }
        break
    }

    if (valuesToCheck.length === 0) {
      return filter.value === '' // aligns with how querying works on the server
    }

    valuesToCheck = flattenMultivalueProperties(valuesToCheck)

    for (let i = 0; i <= valuesToCheck.length - 1; i++) {
      switch (filter.type) {
        case 'ILIKE':
          if (matchesILIKE(valuesToCheck[i], filter)) {
            return true
          }
          break
        case 'LIKE':
          if (matchesLIKE(valuesToCheck[i], filter)) {
            return true
          }
          break
        case '=':
          if (determineEqualsMatcher(filter)(valuesToCheck[i], filter)) {
            return true
          }
          break
        case '!=':
          if (matchesNOTEQUALS(valuesToCheck[i], filter)) {
            return true
          }
          break
        case '>':
          if (matchesGreaterThan(valuesToCheck[i], filter)) {
            return true
          }
          break
        case '>=':
          if (matchesGreaterThanOrEqualTo(valuesToCheck[i], filter)) {
            return true
          }
          break
        case '<':
          if (matchesLessThan(valuesToCheck[i], filter)) {
            return true
          }
          break
        case '<=':
          if (matchesLessThanOrEqualTo(valuesToCheck[i], filter)) {
            return true
          }
          break
        case 'INTERSECTS':
          if (matchesPOLYGON(valuesToCheck[i], filter)) {
            return true
          }
          break
        case 'DWITHIN':
          if (CQLUtils.isPointRadiusFilter(filter)) {
            if (matchesCIRCLE(valuesToCheck[i], filter)) {
              return true
            }
          } else if (CQLUtils.isPolygonFilter(filter)) {
            if (matchesBufferedPOLYGON(valuesToCheck[i], filter)) {
              return true
            }
          } else if (matchesLINESTRING(valuesToCheck[i], filter)) {
            return true
          }
          break
        case 'AFTER':
          if (matchesAFTER(valuesToCheck[i], filter)) {
            return true
          }
          break
        case 'BEFORE':
          if (matchesBEFORE(valuesToCheck[i], filter)) {
            return true
          }
          break
        case 'DURING':
          if (matchesDURING(valuesToCheck[i], filter)) {
            return true
          }
          break
      }
    }
    return false
  } else {
    return matchesFilters(metacard, filter)
  }
}

function matchesFilters(metacard, resultFilter) {
  let i
  switch (resultFilter.type) {
    case 'AND':
      for (i = 0; i <= resultFilter.filters.length - 1; i++) {
        if (!matchesFilter(metacard, resultFilter.filters[i])) {
          return false
        }
      }
      return true
    case 'NOT AND':
      for (i = 0; i <= resultFilter.filters.length - 1; i++) {
        if (!matchesFilter(metacard, resultFilter.filters[i])) {
          return true
        }
      }
      return false
    case 'OR':
      for (i = 0; i <= resultFilter.filters.length - 1; i++) {
        if (matchesFilter(metacard, resultFilter.filters[i])) {
          return true
        }
      }
      return false
    case 'NOT OR':
      for (i = 0; i <= resultFilter.filters.length - 1; i++) {
        if (matchesFilter(metacard, resultFilter.filters[i])) {
          return false
        }
      }
      return true
    default:
      return matchesFilter(metacard, resultFilter)
  }
}

module.exports = {
  matchesFilters,
  matchesCql(metacardJSON, cqlString) {
    if (cqlString === '') {
      return true
    }
    return this.matchesFilters(metacardJSON, cql.read(cqlString))
  },
}
