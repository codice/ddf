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
import { METERS } from '../../geometry'
import {
  geoEditorToDialog,
  GeoEditorProps as Props,
  FinalizeGeo,
} from './geo-editor-to-dialog'
import { FixedHeightPointEditor } from '../presentation/point-circle-editor'
import { updateCircleGeo } from '../circle-geo-writer'

const finalizeGeo: FinalizeGeo = geo => geo

class PointGeoEditor extends React.Component<Props> {
  render() {
    const { geo, coordinateUnit, onUpdateGeo } = this.props
    const lon = (geo.geometry as GeoJSON.Point).coordinates[0]
    const lat = (geo.geometry as GeoJSON.Point).coordinates[1]
    return (
      <FixedHeightPointEditor
        coordinateUnit={coordinateUnit}
        lat={lat}
        lon={lon}
        setCoordinate={(latValue: number, lonValue: number) => {
          onUpdateGeo(updateCircleGeo(geo, latValue, lonValue, 0, METERS))
        }}
      />
    )
  }
}

const PointEditorDialog = geoEditorToDialog(
  PointGeoEditor,
  'Point',
  finalizeGeo
)

export { PointGeoEditor, PointEditorDialog }
