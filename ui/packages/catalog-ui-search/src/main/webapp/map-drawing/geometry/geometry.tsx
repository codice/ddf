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
import { Shape } from '../shape-utils'
import { LengthUnit, METERS } from './units'

type Extent = [number, number, number, number]

type GeometryJSONProperties = {
  color: string
  shape: Shape
  buffer?: number
  bufferUnit: LengthUnit
  id: string
}

const DEFAULT_PROPERTIES: GeometryJSONProperties = {
  shape: 'Polygon',
  id: '',
  color: 'blue',
  buffer: 0,
  bufferUnit: METERS,
}

type Geometry = GeoJSON.Polygon | GeoJSON.Point | GeoJSON.LineString

type GeometryJSON = GeoJSON.Feature & {
  bbox: Extent
  properties: GeometryJSONProperties
  geometry: Geometry
}

const DEFAULT_POLYGON: GeoJSON.Polygon = {
  type: 'Polygon',
  coordinates: [[[0, 0]]],
}

const DEFAULT_POINT: GeoJSON.Point = {
  type: 'Point',
  coordinates: [0, 0],
}

const DEFAULT_GEOMETRY: { [shape in Shape]: Geometry } = {
  Polygon: DEFAULT_POLYGON,
  'Bounding Box': DEFAULT_POLYGON,
  'Point Radius': DEFAULT_POINT,
  Line: {
    type: 'LineString',
    coordinates: [[0, 0]],
  },
  Point: DEFAULT_POINT,
}
const CIRCLE_BUFFER_PROPERTY_VALUE = 'circle'
const POLYGON_LINE_BUFFER_PROPERTY_VALUE = 'polygon/line'
const BUFFER_SHAPE_PROPERTY = 'bufferShape'
type BufferShape = 'circle' | 'polygon/line'

export {
  GeometryJSON,
  GeometryJSONProperties,
  Geometry,
  BufferShape,
  Extent,
  BUFFER_SHAPE_PROPERTY,
  CIRCLE_BUFFER_PROPERTY_VALUE,
  DEFAULT_GEOMETRY,
  DEFAULT_PROPERTIES,
  POLYGON_LINE_BUFFER_PROPERTY_VALUE,
}
