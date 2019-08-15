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
import { makeGeometry } from './utilities'
import { GeometryJSON, Geometry, Extent, DEFAULT_PROPERTIES } from './geometry'
import { Shape } from '../shape-utils'
import { LengthUnit } from './units'

const makeGeometryJSONFromGeometry = (
  id: string,
  geometry: Geometry,
  shape: Shape,
  buffer: number = 0,
  bufferUnit: LengthUnit = DEFAULT_PROPERTIES.bufferUnit
): GeometryJSON =>
  makeGeometry(
    id,
    geometry,
    DEFAULT_PROPERTIES.color,
    shape,
    buffer,
    bufferUnit
  )

const makePointGeo = (id: string, lat: number, lon: number): GeometryJSON =>
  makeGeometryJSONFromGeometry(
    id,
    {
      type: 'Point',
      coordinates: [lon, lat],
    } as GeoJSON.Point,
    'Point'
  )

const makePointRadiusGeo = (
  id: string,
  lat: number,
  lon: number,
  radius: number,
  radiusUnit: LengthUnit
): GeometryJSON =>
  makeGeometryJSONFromGeometry(
    id,
    {
      type: 'Point',
      coordinates: [lon, lat],
    } as GeoJSON.Point,
    'Point Radius',
    radius,
    radiusUnit
  )

const makePolygonGeo = (
  id: string,
  lonLatCoordinateList: [number, number][],
  buffer: number,
  bufferUnit: LengthUnit
): GeometryJSON =>
  makeGeometryJSONFromGeometry(
    id,
    {
      type: 'Polygon',
      coordinates: [[...lonLatCoordinateList, lonLatCoordinateList[0]]],
    } as GeoJSON.Polygon,
    'Polygon',
    buffer,
    bufferUnit
  )

const makeLineGeo = (
  id: string,
  lonLatCoordinateList: [number, number][],
  buffer: number,
  bufferUnit: LengthUnit
): GeometryJSON =>
  makeGeometryJSONFromGeometry(
    id,
    {
      type: 'LineString',
      coordinates: lonLatCoordinateList,
    } as GeoJSON.LineString,
    'Line',
    buffer,
    bufferUnit
  )

const makeBBoxGeo = (id: string, extent: Extent): GeometryJSON =>
  makeGeometryJSONFromGeometry(
    id,
    {
      ...turf.bboxPolygon(extent).geometry,
    } as GeoJSON.Polygon,
    'Bounding Box'
  )

export {
  makeBBoxGeo,
  makeLineGeo,
  makePointGeo,
  makePointRadiusGeo,
  makePolygonGeo,
}
