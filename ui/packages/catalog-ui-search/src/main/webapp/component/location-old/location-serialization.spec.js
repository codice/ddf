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
      [-125.180588, 48.603515],
      [-130.42847, 38.872222],
      [-117.556532, 41.896131],
      [-125.180588, 48.603515],
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
  const deserializedBbox = {
    mode: 'bbox',
    polygon: [
      [-122.144963, 51.08591],
      [-108.169324, 51.08591],
      [-108.169324, 12.374553],
      [-122.144963, 12.374553],
      [-122.144963, 51.08591],
    ],
    north: 51.08591,
    east: -108.169324,
    south: 12.374553,
    west: -122.144963,
  }

  const serializedBbox = {
    type: 'Feature',
    bbox: [12.374553, 51.08591, -122.144963, -108.169324],
    geometry: {
      type: 'Polygon',
      coordinates: [
        [
          [-122.144963, 51.08591],
          [-108.169324, 51.08591],
          [-108.169324, 12.374553],
          [-122.144963, 12.374553],
          [-122.144963, 51.08591],
        ],
      ],
    },
    properties: {
      type: 'BoundingBox',
      north: 51.08591,
      east: -108.169324,
      south: 12.374553,
      west: -122.144963,
    },
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
      type: 'MultiPolygon',
      coordinates: [
        [
          [
            [-125.180588, 48.603515],
            [-130.42847, 38.872222],
            [-117.556532, 41.896131],
            [-125.180588, 48.603515],
          ],
        ],
      ],
    },
    properties: {
      type: 'MultiPolygon',
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
    polyType: 'multipolygon',
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
        [
          [87.3879, 21.7495],
          [87.6879, 21.7495],
          [87.6879, 22.0495],
          [87.3879, 22.0495],
          [87.3879, 21.7495],
        ],
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
