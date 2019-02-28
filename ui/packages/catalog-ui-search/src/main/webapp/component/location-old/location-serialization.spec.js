import { expect, assert } from 'chai'
import { serialize, deserialize } from './location-serialization'

describe('serialize/deserialize line', () => {
  const serializedLine = {
    type: 'Feature',
    geometry: {
      type: 'LineString',
      coordinates: [[-134.388122, 53.795318], [-125.668486, 43.012693]],
    },
    properties: {
      type: 'LineString',
      buffer: {
        width: 1,
        unit: 'meters',
      },
    },
  }

  const deserializedLine = {
    mode: 'line',
    line: [[-134.388122, 53.795318], [-125.668486, 43.012693]],
    lineWidth: 1,
    lineUnits: 'meters',
  }

  it('properly tranlates filter tree JSON into location model representation', () => {
    expect(deserialize(serializedLine)).to.deep.include(deserializedLine)
  })

  it('properly tranlates filter location model into filter tree json', () => {
    expect(serialize(deserializedLine)).to.deep.include(serializedLine)
  })
})

describe('serialize/deserialize multiline', () => {
  const serializedMultiLine = {
    type: 'Feature',
    geometry: {
      type: 'MultiLineString',
      coordinates: [[-134.388122, 53.795318], [-125.668486, 43.012693]],
    },
    properties: {
      type: 'MultiLineString',
      buffer: {
        width: 1,
        unit: 'meters',
      },
    },
  }

  const deserializedMultiLine = {
    mode: 'multiline',
    multiline: [[-134.388122, 53.795318], [-125.668486, 43.012693]],
    lineWidth: 1,
    lineUnits: 'meters',
  }

  it('properly tranlates filter tree JSON into location model representation', () => {
    expect(deserialize(serializedMultiLine)).to.deep.include(
      deserializedMultiLine
    )
  })

  it('properly tranlates filter location model into filter tree json', () => {
    expect(serialize(deserializedMultiLine)).to.deep.include(
      serializedMultiLine
    )
  })
})

describe('serialize/deserialize point', () => {
  const serializedPoint = {
    type: 'Feature',
    geometry: {
      type: 'Point',
      coordinates: [31.964569, -111.131821],
    },
    properties: {
      type: 'Point',
      buffer: {
        width: 140603.222706,
        unit: 'meters',
      },
    },
  }

  const deserializedPoint = {
    mode: 'circle',
    locationType: 'latlon',
    lat: -111.131821,
    lon: 31.964569,
    radius: 140603.222706,
    radiusUnits: 'meters',
  }

  it('properly tranlates filter tree JSON into location model representation', () => {
    expect(deserialize(serializedPoint)).to.deep.include(deserializedPoint)
  })
})

describe('serialize/deserialize polygon', () => {
  const serializedPolygon = {
    type: 'Feature',
    geometry: {
      type: 'Polygon',
      coordinates: [
        [
          [-125.180588, 48.603515],
          [-130.42847, 38.872222],
          [-117.556532, 41.896131],
          [-125.180588, 48.603515],
        ],
      ],
    },
    properties: {
      type: 'Polygon',
      buffer: {
        width: 0,
        unit: 'meters',
      },
    },
  }

  const deserializedPolygon = {
    mode: 'poly',
    polygon: [
      [
        [-125.180588, 48.603515],
        [-130.42847, 38.872222],
        [-117.556532, 41.896131],
        [-125.180588, 48.603515],
      ],
    ],
    polygonBufferWidth: 0,
    polygonBufferUnits: 'meters',
    polyType: 'polygon',
  }

  it('properly tranlates filter tree JSON into location model representation', () => {
    expect(deserialize(serializedPolygon)).to.deep.include(deserializedPolygon)
  })

  it('properly tranlates filter location model into filter tree json', () => {
    expect(serialize(deserializedPolygon)).to.deep.include(serializedPolygon)
  })
})

describe('serialize/deserialize bbox', () => {
  const serializedBbox = {
    type: 'Feature',
    bbox: [-44.449923, -27.849887, -112.533338, -92.952499],
    geometry: {
      type: 'Polygon',
      coordinates: {
        type: 'Feature',
        properties: {},
        geometry: {
          type: 'Polygon',
          coordinates: [
            [
              [-44.449923, -27.849887],
              [-112.533338, -27.849887],
              [-112.533338, -92.952499],
              [-44.449923, -92.952499],
              [-44.449923, -27.849887],
            ],
          ],
        },
      },
    },
    properties: {
      type: 'BoundingBox',
      north: -27.849887,
      east: -92.952499,
      south: -44.449923,
      west: -112.533338,
    },
  }

  const deserializedBbox = {
    mode: 'bbox',
    polygon: [
      [
        [-112.533338, -27.849887],
        [-92.952499, -27.849887],
        [-92.952499, -44.449923],
        [-112.533338, -44.449923],
        [-112.533338, -27.849887],
      ],
    ],
    north: -27.849887,
    east: -92.952499,
    south: -44.449923,
    west: -112.533338,
  }

  it('properly tranlates filter tree JSON into location model representation', () => {
    expect(deserialize(serializedBbox)).to.deep.include(deserializedBbox)
  })

  it('properly tranlates filter location model into filter tree json', () => {
    expect(serialize(deserializedBbox)).to.deep.include(serializedBbox)
  })
})

describe('serialize/deserialize multipolygon', () => {
  const serializedPolygon = {
    type: 'Feature',
    geometry: {
      type: 'Polygon',
      coordinates: [
        [
          [-125.180588, 48.603515],
          [-130.42847, 38.872222],
          [-117.556532, 41.896131],
          [-125.180588, 48.603515],
        ],
      ],
    },
    properties: {
      type: 'Polygon',
      buffer: {
        width: 0,
        unit: 'meters',
      },
    },
  }

  const deserializedPolygon = {
    mode: 'poly',
    polygon: [
      [
        [-125.180588, 48.603515],
        [-130.42847, 38.872222],
        [-117.556532, 41.896131],
        [-125.180588, 48.603515],
      ],
    ],
    polygonBufferWidth: 0,
    polygonBufferUnits: 'meters',
    polyType: 'polygon',
  }

  it('properly tranlates filter tree JSON into location model representation', () => {
    expect(deserialize(serializedPolygon)).to.deep.include(deserializedPolygon)
  })

  it('properly tranlates filter location model into filter tree json', () => {
    expect(serialize(deserializedPolygon)).to.deep.include(serializedPolygon)
  })
})

describe('serialize/deserialize keyword', () => {
  const serializedKeyword = {
    mode: 'keyword',
    keywordValue: 'Egra, IND',
    polygon: [
      [87.3879, 21.7495],
      [87.6879, 21.7495],
      [87.6879, 22.0495],
      [87.3879, 22.0495],
      [87.3879, 21.7495],
    ],
    polygonBufferWidth: 0,
    polygonBufferUnits: 'meters',
    polyType: 'polygon',
  }

  const deserializedKeyword = {
    type: 'Feature',
    geometry: {
      type: 'Polygon',
      coordinates: [
        [87.3879, 21.7495],
        [87.6879, 21.7495],
        [87.6879, 22.0495],
        [87.3879, 22.0495],
        [87.3879, 21.7495],
      ],
    },
    properties: {
      type: 'Keyword',
      keywordValue: 'Egra, IND',
      buffer: {
        width: 0,
        unit: 'meters',
      },
    },
  }

  it('properly tranlates filter tree JSON into location model representation', () => {
    expect(deserialize(deserializedKeyword)).to.deep.include(serializedKeyword)
  })

  it('properly tranlates filter location model into filter tree json', () => {
    expect(serialize(serializedKeyword)).to.deep.include(deserializedKeyword)
  })
})
