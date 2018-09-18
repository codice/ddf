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
/*jshint bitwise: false*/
const $ = require('jquery')
const cql = require('js/cql')
const DistanceUtils = require('js/DistanceUtils.js')

function sanitizeForCql(text) {
  return text
    .split('[')
    .join('(')
    .split(']')
    .join(')')
    .split("'")
    .join('')
    .split('"')
    .join('')
}

function lineToCQLLine(model) {
  var cqlLINE = model.map(function(point) {
    return point[0] + ' ' + point[1]
  })
  return cqlLINE
}

function polygonToCQLPolygon(model) {
  var cqlPolygon = model.map(function(point) {
    return point[0] + ' ' + point[1]
  })
  if (cqlPolygon[0] !== cqlPolygon[cqlPolygon.length - 1]) {
    cqlPolygon.push(cqlPolygon[0])
  }
  return [cqlPolygon]
}

function polygonToCQLMultiPolygon(model) {
  return model.map(function(polygon) {
    return polygonToCQLPolygon(polygon)
  })
}

function bboxToCQLPolygon(model) {
  if (model.locationType === 'usng') {
    return [
      model.mapWest + ' ' + model.mapSouth,
      model.mapWest + ' ' + model.mapNorth,
      model.mapEast + ' ' + model.mapNorth,
      model.mapEast + ' ' + model.mapSouth,
      model.mapWest + ' ' + model.mapSouth,
    ]
  } else {
    return [
      model.west + ' ' + model.south,
      model.west + ' ' + model.north,
      model.east + ' ' + model.north,
      model.east + ' ' + model.south,
      model.west + ' ' + model.south,
    ]
  }
}

function generateAnyGeoFilter(property, model) {
  switch (model.type) {
    case 'LINE':
      return {
        type: 'DWITHIN',
        property: property,
        value:
          'LINESTRING' +
          sanitizeForCql(JSON.stringify(lineToCQLLine(model.line))),
        distance: DistanceUtils.getDistanceInMeters(
          model.lineWidth,
          model.lineUnits
        ),
      }
    case 'POLYGON':
      return {
        type: model.polygonBufferWidth > 0 ? 'DWITHIN' : 'INTERSECTS',
        property: property,
        value: `POLYGON${sanitizeForCql(
          JSON.stringify(polygonToCQLPolygon(model.polygon))
        )}`,
        ...(model.polygonBufferWidth && {
          distance: DistanceUtils.getDistanceInMeters(
            model.polygonBufferWidth,
            model.polygonBufferUnits
          ),
        }),
      }
    case 'MULTIPOLYGON':
      var poly =
        'MULTIPOLYGON' +
        sanitizeForCql(JSON.stringify(polygonToCQLMultiPolygon(model.polygon)))
      return {
        type: model.polygonBufferWidth > 0 ? 'DWITHIN' : 'INTERSECTS',
        property: property,
        value: poly,
        ...(model.polygonBufferWidth && {
          distance: DistanceUtils.getDistanceInMeters(
            model.polygonBufferWidth,
            model.polygonBufferUnits
          ),
        }),
      }
    case 'BBOX':
      return {
        type: 'INTERSECTS',
        property: property,
        value:
          'POLYGON(' +
          sanitizeForCql(JSON.stringify(bboxToCQLPolygon(model))) +
          ')',
      }
    case 'POINTRADIUS':
      return {
        type: 'DWITHIN',
        property: property,
        value: 'POINT(' + model.lon + ' ' + model.lat + ')',
        distance: DistanceUtils.getDistanceInMeters(
          model.radius,
          model.radiusUnits
        ),
      }
    default:
      return {
        type: 'INTERSECTS',
        property: property,
        value: '',
      }
  }
}

function buildIntersectOrCQL(shapes) {
  var locationFilter = ''
  $.each(
    shapes,
    function(i, shape) {
      locationFilter += this.buildIntersectCQL(shape)

      if (i !== shapes.length - 1) {
        locationFilter += ' OR '
      }
    }.bind(this)
  )

  return locationFilter
}

function arrayFromPartialWkt(partialWkt) {
  // remove the leading and trailing parentheses
  var result = partialWkt.replace(/^\(/, '').replace(/\)$/, '')
  // change parentheses to array brackets
  result = result.replace(/\(/g, '[').replace(/\)/g, ']')
  // change each space-separated coordinate pair to a two-element array
  result = result.replace(/([^,\[\]]+)\s+([^,\[\]]+)/g, '[$1,$2]')
  // build nested arrays from the string
  return JSON.parse(result)
}

function sanitizeGeometryCql(cqlString) {
  //sanitize polygons
  let polygons = cqlString.match(
    /'POLYGON\(\((-?[0-9]*.?[0-9]* -?[0-9]*.?[0-9]*,?)*\)\)'/g
  )
  if (polygons) {
    polygons.forEach(polygon => {
      cqlString = cqlString.replace(polygon, polygon.replace(/'/g, ''))
    })
  }

  //sanitize multipolygons
  let multipolygons = cqlString.match(/'MULTIPOLYGON\(\(\(.*\)\)\)'/g)
  if (multipolygons) {
    multipolygons.forEach(multipolygon => {
      cqlString = cqlString.replace(
        multipolygon,
        multipolygon.replace(/'/g, '')
      )
    })
  }

  //sanitize points
  let points = cqlString.match(/'POINT\(-?[0-9]*.?[0-9]* -?[0-9]*.?[0-9]*\)'/g)
  if (points) {
    points.forEach(point => {
      cqlString = cqlString.replace(point, point.replace(/'/g, ''))
    })
  }

  //sanitize linestrings
  let linestrings = cqlString.match(
    /'LINESTRING\((-?[0-9]*.?[0-9]* -?[0-9]*.?[0-9]*.?)*\)'/g
  )
  if (linestrings) {
    linestrings.forEach(linestring => {
      cqlString = cqlString.replace(linestring, linestring.replace(/'/g, ''))
    })
  }
  return cqlString
}

function getProperty(filter) {
  if (typeof filter.property !== 'string') {
    return null
  }
  return filter.property.split('"').join('')
}

function generateFilter(type, property, value, metacardDefinitions) {
  if (!metacardDefinitions) {
    metacardDefinitions = require('component/singletons/metacard-definitions')
  }
  switch (metacardDefinitions.metacardTypes[property].type) {
    case 'LOCATION':
    case 'GEOMETRY':
      return generateAnyGeoFilter(property, value)
    default:
      return {
        type: type,
        property: '"' + property + '"',
        value: value,
      }
  }
}

function generateFilterForFilterFunction(filterFunctionName, params) {
  return {
    type: '=',
    value: true,
    property: {
      type: 'FILTER_FUNCTION',
      filterFunctionName,
      params,
    },
  }
}

function isGeoFilter(type) {
  return type === 'DWITHIN' || type === 'INTERSECTS'
}

function transformFilterToCQL(filter) {
  return this.sanitizeGeometryCql(
    '(' + cql.write(cql.simplify(cql.read(cql.write(filter)))) + ')'
  )
}

function transformCQLToFilter(cqlString) {
  return cql.simplify(cql.read(cqlString))
}

const isPolygonFilter = filter => {
  return (
    filter.value &&
    filter.value.value &&
    filter.value.value.indexOf('POLYGON') >= 0
  )
}

function isPointRadiusFilter(filter) {
  var filterValue =
    typeof filter.value === 'object' ? filter.value.value : filter.value
  return filterValue && filterValue.indexOf('POINT') >= 0
}

function buildIntersectCQL(locationGeometry) {
  var locationFilter = ''
  var locationWkt = locationGeometry.toWkt()
  var locationType = locationGeometry.toGeoJSON().type.toUpperCase()

  var shapes
  switch (locationType) {
    case 'POINT':
    case 'LINESTRING':
      locationFilter = '(DWITHIN(anyGeo, ' + locationWkt + ', 1, meters))'
      break
    case 'POLYGON':
      // Test if the shape wkt contains ,(
      if (/,\(/.test(locationWkt)) {
        shapes = locationWkt.split(',(')

        $.each(
          shapes,
          function(i, polygon) {
            locationWkt = polygon.replace(/POLYGON|[()]/g, '')
            locationWkt = 'POLYGON((' + locationWkt + '))'
            locationFilter += '(INTERSECTS(anyGeo, ' + locationWkt + '))'

            if (i !== shapes.length - 1) {
              locationFilter += ' OR '
            }
          }.bind(this)
        )
      } else {
        locationFilter = '(INTERSECTS(anyGeo, ' + locationWkt + '))'
      }
      break
    case 'MULTIPOINT':
      shapes = locationGeometry.points
      locationFilter = buildIntersectOrCQL.call(this, shapes)
      break
    case 'MULTIPOLYGON':
      shapes = locationGeometry.polygons
      locationFilter = buildIntersectOrCQL.call(this, shapes)
      break
    case 'MULTILINESTRING':
      shapes = locationGeometry.lineStrings
      locationFilter = buildIntersectOrCQL.call(this, shapes)
      break
    case 'GEOMETRYCOLLECTION':
      shapes = locationGeometry.geometries
      locationFilter = buildIntersectOrCQL.call(this, shapes)
      break
    default:
      console.log('unknown location type')
      return
  }

  return locationFilter
}

function arrayFromPolygonWkt(wkt) {
  // Handle POLYGON with no internal rings (i.e. holes)
  if (wkt.startsWith('POLYGON')) {
    const polygon = wkt.match(/\(\([^\(\)]+\)\)/g)
    return polygon.length === 1 ? arrayFromPartialWkt(polygon[0]) : []
  }

  // Handle MULTIPOLYGON with no internal rings (i.e. holes)
  let polygons = wkt.match(/\(\([^\(\)]+\)\)/g)
  if (polygons) {
    return polygons.map(
      function(polygon) {
        return arrayFromPartialWkt(polygon)
      }.bind(this)
    )
  }
  return []
}

module.exports = {
  sanitizeGeometryCql,
  getProperty,
  generateFilter,
  generateFilterForFilterFunction,
  isGeoFilter,
  transformFilterToCQL,
  transformCQLToFilter,
  isPolygonFilter,
  isPointRadiusFilter,
  buildIntersectCQL,
  arrayFromPolygonWkt,
}
