const wkx = require('wkx')
const DistanceUtils = require('../../js/DistanceUtils')
const Turf = require('@turf/turf')

const is3DArray = array =>
  Array.isArray(array) && Array.isArray(array[0]) && Array.isArray(array[0][0])
const is4DArray = array => is3DArray(array) && Array.isArray(array[0][0][0])

const LineString = {
  'json->location': ({
    geometry: { coordinates },
    properties: { buffer } = {},
  }) => ({
    mode: 'line',
    line: coordinates,
    lineWidth: buffer.width,
    lineUnits: buffer.unit,
  }),
  'location->json': ({ line = [], lineWidth = 1, lineUnits = 'meters' }) => ({
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
  }),
}

const MultiLineString = {
  'json->location': ({
    geometry: { coordinates },
    properties: { buffer } = {},
  }) => ({
    mode: 'multiline',
    multiline: coordinates,
    lineWidth: buffer.width,
    lineUnits: buffer.unit,
  }),
  'location->json': ({
    multiline = [],
    lineWidth = 1,
    lineUnits = 'meters',
  }) => ({
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
  }),
}

const Point = {
  'json->location': ({
    geometry: { coordinates } = [lat, lon],
    properties: { buffer } = {},
  }) => ({
    mode: 'circle',
    locationType: 'latlon',
    lat: coordinates[0],
    lon: coordinates[1],
    radius: buffer.width,
    radiusUnits: buffer.unit,
  }),
  'location->json': ({
    lat = 0,
    lon = 0,
    radius = 1,
    radiusUnits = 'meters',
  }) => ({
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
  }),
}

const Polygon = {
  'json->location': ({
    geometry: { coordinates },
    properties: { buffer } = {},
  }) => ({
    mode: 'poly',
    polygon: coordinates,
    polygonBufferWidth: buffer.width,
    polygonBufferUnits: buffer.unit,
    polyType: 'polygon',
  }),
  'location->json': ({
    polygon = [],
    polygonBufferWidth = 0,
    polygonBufferUnits = 'meters',
  }) => ({
    type: 'Feature',
    geometry: {
      type: 'Polygon',
      coordinates: is3DArray(polygon) ? polygon : [polygon],
    },
    properties: {
      type: 'Polygon',
      buffer: {
        width: polygonBufferWidth,
        unit: polygonBufferUnits,
      },
    },
  }),
}

const BoundingBox = {
  'json->location': ({ properties: { north, east, south, west } }) => {
    const coordinates = [
      [
        [west, north],
        [east, north],
        [east, south],
        [west, south],
        [west, north],
      ],
    ]

    return {
      mode: 'bbox',
      polygon: coordinates,
      north,
      east,
      south,
      west,
    }
  },
  'location->json': ({ north, east, south, west }) => {
    return {
      type: 'Feature',
      bbox: [south, north, west, east],
      geometry: {
        type: 'Polygon',
        coordinates: Turf.bboxPolygon([south, north, west, east]),
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
  'json->location': ({
    geometry: { coordinates },
    properties: { buffer } = {},
  }) => ({
    mode: 'multipolygon',
    polygon: coordinates,
    polygonBufferWidth,
    polygonBufferUnits,
    polyType: 'multipolygon',
  }),
  'location->json': ({
    polygon = [],
    polygonBufferWidth,
    polygonBufferUnits,
  }) => ({
    type: 'Feature',
    geometry: {
      type: 'MultiPolygon',
      coordinates: is3DArray(polygon) ? [polygon] : polygon,
    },
    properties: {
      type: 'MultiPolygon',
      buffer: {
        width: polygonBufferWidth,
        unit: polygonBufferUnits,
      },
    },
  }),
}

const Keyword = {
  'json->location': ({
    properties: { keywordValue, buffer },
    geometry = {},
  }) => {
    const { type, coordinates } = geometry
    return {
      mode: 'keyword',
      keywordValue,
      polygon: coordinates,
      polygonBufferWidth: buffer.width,
      polygonBufferUnits: buffer.unit,
      polyType: type === 'MultiPolygon' ? 'multipolygon' : 'polygon',
    }
  },
  'location->json': ({
    polygon = [],
    polyType,
    keywordValue,
    polygonBufferWidth,
    polygonBufferUnits,
  }) => ({
    type: 'Feature',
    geometry: {
      type: polyType === 'polygon' ? 'Polygon' : 'MultiPolygon',
      coordinates: polygon,
    },
    properties: {
      type: 'Keyword',
      keywordValue,
      buffer: {
        width: polygonBufferWidth,
        unit: polygonBufferUnits,
      },
    },
  }),
}

const Serializers = {
  line: LineString,
  multiline: MultiLineString,
  circle: Point,
  poly: Polygon,
  multipolygon: MultiPolygon,
  keyword: Keyword,
  bbox: BoundingBox,
}

const Deserializers = {
  Point,
  Polygon,
  MultiPolygon,
  LineString,
  MultiLineString,
  BoundingBox,
  Keyword,
}

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
