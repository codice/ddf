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
const plugin = require('plugins/location-serialization')

const LineString = {
  'json->location': json => {
    const {
      geometry: { coordinates },
      properties: { buffer } = {},
    } = json

    const { width = 1, unit = 'meters' } = buffer

    return {
      mode: 'line',
      line: coordinates,
      lineWidth: width,
      lineUnits: unit,
    }
  },
  'location->json': location => {
    const { line = [], lineWidth = 1, lineUnits = 'meters' } = location

    return {
      type: 'Feature',
      geometry: {
        type: 'LineString',
        coordinates: line,
      },
      properties: {
        type: 'LineString',
        buffer: {
          width: lineWidth,
          unit: lineUnits,
        },
      },
    }
  },
}

const MultiLineString = {
  'json->location': json => {
    const {
      geometry: { coordinates },
      properties: { buffer } = {},
    } = json

    const { width = 1, unit = 'meters' } = buffer

    return {
      mode: 'multiline',
      multiline: coordinates,
      lineWidth: width,
      lineUnits: unit,
    }
  },
  'location->json': location => {
    const { multiline = [], lineWidth = 1, lineUnits = 'meters' } = location

    return {
      type: 'Feature',
      geometry: {
        type: 'MultiLineString',
        coordinates: multiline,
      },
      properties: {
        type: 'MultiLineString',
        buffer: {
          width: lineWidth,
          unit: lineUnits,
        },
      },
    }
  },
}

const Point = {
  'json->location': json => {
    const {
      geometry: { coordinates },
      properties: { buffer } = {},
    } = json

    const [lon = 0, lat = 0] = coordinates
    const { width = 1, unit = 'meters' } = buffer

    return {
      mode: 'circle',
      locationType: 'latlon',
      lat,
      lon,
      radius: width,
      radiusUnits: unit,
    }
  },
  'location->json': location => {
    const { lat = 0, lon = 0, radius = 1, radiusUnits = 'meters' } = location

    return {
      type: 'Feature',
      geometry: {
        type: 'Point',
        coordinates: [lon, lat],
      },
      properties: {
        type: 'Point',
        buffer: {
          width: radius,
          unit: radiusUnits,
        },
      },
    }
  },
}

const Polygon = {
  'json->location': json => {
    const {
      geometry: { coordinates },
      properties: { buffer } = {},
    } = json

    const [polygon] = coordinates
    const { width = 0, unit = 'meters' } = buffer

    return {
      mode: 'poly',
      polygon,
      polygonBufferWidth: width,
      polygonBufferUnits: unit,
      polyType: 'polygon',
    }
  },
  'location->json': location => {
    const {
      polygon = [],
      polygonBufferWidth = 0,
      polygonBufferUnits = 'meters',
      polyType = 'polygon',
    } = location

    if (polyType === 'multipolygon') {
      return MultiPolygon['location->json'](location)
    }

    return {
      type: 'Feature',
      geometry: {
        type: 'Polygon',
        coordinates: [polygon],
      },
      properties: {
        type: 'Polygon',
        buffer: {
          width: polygonBufferWidth,
          unit: polygonBufferUnits,
        },
      },
    }
  },
}

const BoundingBox = {
  'json->location': json => {
    const {
      geometry: { coordinates },
      properties: { north, east, south, west },
    } = json

    const [polygon] = coordinates

    return {
      mode: 'bbox',
      polygon,
      north,
      east,
      south,
      west,
    }
  },
  'location->json': location => {
    const { north, east, south, west } = location
    return {
      type: 'Feature',
      bbox: [south, north, west, east],
      geometry: {
        type: 'Polygon',
        coordinates: [
          [
            [west, north],
            [east, north],
            [east, south],
            [west, south],
            [west, north],
          ],
        ],
      },
      properties: {
        type: 'BoundingBox',
        north,
        east,
        south,
        west,
      },
    }
  },
}

const MultiPolygon = {
  'json->location': json => {
    const {
      geometry: { coordinates },
      properties: { buffer } = {},
    } = json

    const { width = 0, unit = 'meters' } = buffer

    const polygon = coordinates.map(([child]) => child)

    return {
      mode: 'poly',
      polygon,
      polygonBufferWidth: width,
      polygonBufferUnits: unit,
      polyType: 'multipolygon',
    }
  },
  'location->json': location => {
    const {
      polygon = [],
      polygonBufferWidth = 0,
      polygonBufferUnits = 'meters',
    } = location

    const coordinates = polygon.map(child => [child])

    return {
      type: 'Feature',
      geometry: {
        type: 'MultiPolygon',
        coordinates,
      },
      properties: {
        type: 'MultiPolygon',
        buffer: {
          width: polygonBufferWidth,
          unit: polygonBufferUnits,
        },
      },
    }
  },
}

const Keyword = {
  'json->location': json => {
    const {
      properties: { keywordValue, buffer },
      geometry = {},
    } = json

    const { type, coordinates = [] } = geometry
    const { width = 0, unit = 'meters' } = buffer

    const [polygon] =
      type === 'Polygon' ? coordinates : [coordinates.map(([child]) => child)]

    return {
      mode: 'keyword',
      keywordValue,
      polygon,
      polygonBufferWidth: width,
      polygonBufferUnits: unit,
      polyType: type === 'Polygon' ? 'polygon' : 'multipolygon',
    }
  },
  'location->json': location => {
    const {
      polygon = [],
      polyType,
      keywordValue,
      polygonBufferWidth = 0,
      polygonBufferUnits = 'meters',
    } = location

    const coordinates =
      polyType === 'polygon' ? [polygon] : polygon.map(child => [child])

    return {
      type: 'Feature',
      geometry: {
        type: polyType === 'polygon' ? 'Polygon' : 'MultiPolygon',
        coordinates,
      },
      properties: {
        type: 'Keyword',
        keywordValue,
        buffer: {
          width: polygonBufferWidth,
          unit: polygonBufferUnits,
        },
      },
    }
  },
}

const Serializers = plugin.Serializers({
  line: LineString,
  multiline: MultiLineString,
  circle: Point,
  poly: Polygon,
  keyword: Keyword,
  bbox: BoundingBox,
})

const Deserializers = plugin.Deserializers({
  Point,
  Polygon,
  MultiPolygon,
  LineString,
  MultiLineString,
  BoundingBox,
  Keyword,
})

export const serialize = location => {
  const mode = location.mode
  if (mode) {
    const serializer = Serializers[mode]['location->json']
    if (typeof serializer === 'function') {
      return serializer(location)
    }
  }
}

export const deserialize = json => {
  if (json) {
    const deserializer = Deserializers[json.properties.type]['json->location']
    if (typeof deserializer === 'function') {
      return deserializer(json)
    }
  }
}
