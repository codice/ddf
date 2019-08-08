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
import * as Units from '../units'
import LatLonDMSBBoxEditor from '../presentation/lat-lon-dms-bbox-editor'
import LatLonBBoxEditor from '../presentation/lat-lon-bbox-editor'
import USNGBBoxEditor from '../presentation/usng-bbox-editor'
import UTMBBoxEditor from '../presentation/utm-bbox-editor'
import {
  extentToBBox,
  bboxToExtent,
  BBoxEditorProps,
} from '../bbox-editor-props'
import { Extent } from '../../geometry'

type Props = {
  setExtent: (extent: Extent) => void
  extent: Extent
  unit: Units.CoordinateUnit
}

type Editor = React.ComponentType<BBoxEditorProps>

const editorMap = (unit: Units.CoordinateUnit): Editor => {
  switch (unit) {
    case Units.LAT_LON:
      return LatLonBBoxEditor
    case Units.LAT_LON_DMS:
      return LatLonDMSBBoxEditor
    case Units.USNG:
      return USNGBBoxEditor
    case Units.UTM:
      return UTMBBoxEditor
    default:
      throw new Error(`Unit "${unit}" not supported!`)
  }
}

const BBoxEditor: React.SFC<Props> = ({ setExtent, extent, unit }) => {
  const { north, south, east, west } = extentToBBox(extent)
  const EditorTag = editorMap(unit)
  return (
    <EditorTag
      north={north}
      south={south}
      east={east}
      west={west}
      setBBox={bbox => {
        setExtent(bboxToExtent(bbox))
      }}
    />
  )
}

export default BBoxEditor
