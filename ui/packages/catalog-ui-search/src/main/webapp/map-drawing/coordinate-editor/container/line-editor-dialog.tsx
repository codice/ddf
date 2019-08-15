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

const finalizeGeo: FinalizeGeo = geo => geo

const LineGeoEditor: React.SFC<GeoEditorProps> = props => (
  <FlatCoordinateListGeoEditor
    {...props}
    getCoordinatesFromGeo={geo => {
      const coordinates = (geo.geometry as GeoJSON.LineString)
        .coordinates as Coordinates
      return coordinates.length < 1 ? [[0, 0]] : coordinates
    }}
    updateGeoCoordinates={(geo, coordinates) => {
      const updated: GeometryJSON = { ...geo }
      if (coordinates.length < 1) {
        coordinates = [[0, 0]]
      }
      const lineGeo = geo.geometry as GeoJSON.LineString
      lineGeo.coordinates = coordinates
      return updated
    }}
  />
)
const LineEditorDialog = geoEditorToDialog(LineGeoEditor, 'Line', finalizeGeo)

export { LineGeoEditor, LineEditorDialog }
