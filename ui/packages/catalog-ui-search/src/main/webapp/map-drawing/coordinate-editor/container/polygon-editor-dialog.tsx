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
import * as React from 'react'
import { GeometryJSON } from '../../geometry'
import {
  geoEditorToDialog,
  GeoEditorProps,
  FinalizeGeo,
} from './geo-editor-to-dialog'
import {
  FlatCoordinateListGeoEditor,
  Coordinates,
} from './flat-coordinate-list-geo-editor'

const MIN_POLYGON_COORDINATE_LENGTH = 2
const finalizeGeo: FinalizeGeo = geo => geo

const PolygonGeoEditor: React.SFC<GeoEditorProps> = props => (
  <FlatCoordinateListGeoEditor
    {...props}
    getCoordinatesFromGeo={geo => {
      const coordinates = (geo.geometry as GeoJSON.Polygon)
        .coordinates[0] as Coordinates
      return coordinates.length < MIN_POLYGON_COORDINATE_LENGTH
        ? [[0, 0]]
        : coordinates.slice(0, coordinates.length - 1)
    }}
    updateGeoCoordinates={(geo, coordinates) => {
      const updated: GeometryJSON = { ...geo }
      if (coordinates.length < 1) {
        coordinates = [[0, 0]]
      }
      coordinates.push(coordinates[0])
      const polyGeo = geo.geometry as GeoJSON.Polygon
      polyGeo.coordinates = [coordinates]
      return updated
    }}
  />
)

const Dialog = geoEditorToDialog(PolygonGeoEditor, 'Polygon', finalizeGeo)

export default Dialog
