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
import BBoxEditor from './bbox-editor'
import { extentToBBox } from '../bbox-editor-props'
import { GeometryJSON, Extent } from '../../geometry'
import {
  geoEditorToDialog,
  GeoEditorProps as Props,
  FinalizeGeo,
} from './geo-editor-to-dialog'

const updateGeoWithExtentBBox = (
  geo: GeometryJSON,
  extent: Extent
): GeometryJSON => {
  const { north, south, east, west } = extentToBBox(extent)
  const coordinates: number[][][] = [
    [[west, south], [west, north], [east, north], [east, south], [west, south]],
  ]
  return {
    ...geo,
    bbox: extent,
    geometry: {
      ...geo.geometry,
      coordinates,
    } as GeoJSON.Polygon,
  }
}

const finalizeGeo: FinalizeGeo = geo => {
  const { north, south, east, west } = extentToBBox(geo.bbox)
  const orientationCorrectedBBox: Extent = [
    Math.min(east, west),
    Math.min(north, south),
    Math.max(east, west),
    Math.max(north, south),
  ]
  return updateGeoWithExtentBBox(geo, orientationCorrectedBBox)
}

class BBoxGeoEditor extends React.Component<Props> {
  render() {
    const { geo, coordinateUnit, onUpdateGeo } = this.props
    const extent = geo.bbox
    return (
      <BBoxEditor
        setExtent={extent => {
          onUpdateGeo(updateGeoWithExtentBBox(geo, extent))
        }}
        extent={extent}
        unit={coordinateUnit}
      />
    )
  }
}

const Dialog = geoEditorToDialog(BBoxGeoEditor, 'Bounding Box', finalizeGeo)

export default Dialog

export { updateGeoWithExtentBBox, finalizeGeo }
