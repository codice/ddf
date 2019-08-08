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
import LatLonDMSPointEditor from '../presentation/lat-lon-dms-point-editor'
import LatLonPointEditor from '../presentation/lat-lon-point-editor'
import USNGPointEditor from '../presentation/usng-point-editor'
import UTMPointEditor from '../presentation/utm-point-editor'
import EditorProps from '../point-editor-props'

type Props = EditorProps & {
  unit: Units.CoordinateUnit
}

type Editor = React.ComponentType<EditorProps>

const editorMap = (unit: Units.CoordinateUnit): Editor => {
  switch (unit) {
    case Units.LAT_LON:
      return LatLonPointEditor
    case Units.LAT_LON_DMS:
      return LatLonDMSPointEditor
    case Units.USNG:
      return USNGPointEditor
    case Units.UTM:
      return UTMPointEditor
    default:
      throw new Error(`Unit "${unit}" not supported!`)
  }
}

const PointEditor: React.SFC<Props> = ({ lat, lon, setCoordinate, unit }) => {
  const EditorTag = editorMap(unit)
  return <EditorTag lat={lat} lon={lon} setCoordinate={setCoordinate} />
}

export default PointEditor
