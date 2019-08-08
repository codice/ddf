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
import * as turf from '@turf/turf'
import * as _ from 'lodash'
import { Shape } from '../shape-utils'
import { LengthUnit, METERS } from './units'
import {
  GeometryJSON,
  Geometry,
  Extent,
  BUFFER_SHAPE_PROPERTY,
  CIRCLE_BUFFER,
  DEFAULT_GEOMETRY,
  DEFAULT_PROPERTIES,
  POLYGON_LINE_BUFFER,
} from './geometry'

const DistanceUtils = require('../../js/DistanceUtils.js')

const MINIMUM_POLYGON_OR_LINE_COORDINATE_LENGTH_FOR_TURF_JS = 2

const makeGeometry = (
  id: string,
  geometryJSON: any,
  color: string,
  shape: Shape,
  buffer: number = 0,
  bufferUnit: LengthUnit = METERS
): GeometryJSON => {
  const geometry: Geometry = turf.getGeom(_.cloneDeep(geometryJSON))
  const json: GeometryJSON = {
    type: 'Feature',
    properties: {
      id,
      color,
      shape,
      buffer,
      bufferUnit,
    },
    geometry,
    bbox: [0, 0, 0, 0],
  }
  json.bbox = geoToExtent(makeBufferedGeo(json))
  return json
}

const makeEmptyGeometry = (
  id: string,
  shape: Shape,
  initialProperties: object = {}
): GeometryJSON => ({
  type: 'Feature',
  properties: {
    ...DEFAULT_PROPERTIES,
    ...initialProperties,
    shape,
    id,
    buffer: shape === 'Point Radius' ? 1 : 0,
    bufferUnit: METERS,
  },
  bbox: [0, 0, 0, 0],
  geometry: _.cloneDeep(DEFAULT_GEOMETRY[shape]),
})

const makeBufferedGeo = (geo: GeometryJSON): GeometryJSON => {
  if (
    ((geo.geometry.type === 'Polygon' &&
      geo.geometry.coordinates[0].length >=
        MINIMUM_POLYGON_OR_LINE_COORDINATE_LENGTH_FOR_TURF_JS) ||
      (geo.geometry.type === 'LineString' &&
        geo.geometry.coordinates.length >=
          MINIMUM_POLYGON_OR_LINE_COORDINATE_LENGTH_FOR_TURF_JS) ||
      geo.geometry.type === 'Point') &&
    geo.properties.shape !== 'Bounding Box' &&
    geo.properties.buffer &&
    geo.properties.bufferUnit &&
    geo.properties.buffer > 0
  ) {
    // Copy JSON since turf.buffer has side effects
    geo = _.cloneDeep(geo)
    const bufferedGeo = turf.buffer(
      geo,
      DistanceUtils.getDistanceInMeters(
        geo.properties.buffer,
        geo.properties.bufferUnit
      ),
      {
        units: 'meters',
      }
    )
    if (bufferedGeo && bufferedGeo.properties) {
      bufferedGeo.properties[BUFFER_SHAPE_PROPERTY] =
        geo.geometry.type === 'Point' ? CIRCLE_BUFFER : POLYGON_LINE_BUFFER
    }
    return bufferedGeo as GeometryJSON
  }
  return geo
}

const bboxToExtent = (bbox: GeoJSON.BBox): Extent => [
  bbox[0],
  bbox[1],
  bbox[2],
  bbox[3],
]

const geoToExtent = (geo: GeometryJSON): Extent => {
  return bboxToExtent(turf.bbox(geo))
}

export {
  bboxToExtent,
  geoToExtent,
  makeGeometry,
  makeBufferedGeo,
  makeEmptyGeometry,
}
