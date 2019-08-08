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
import { DMS, decimalToDMS, dmsToDecimal } from '../dms-formatting'
import { DMSLatitudeEditor, DMSLongitudeEditor } from './dms-value-editor'
import * as Common from './common-styles'
import Props from '../point-editor-props'

const Root = Common.Column
const TextGroup = Common.SpacedInputLabelRow
const Label = Common.Label

const LatLonDMSPointEditor: React.SFC<Props> = ({
  lat,
  lon,
  setCoordinate,
}) => {
  const dmsLat = decimalToDMS(lat)
  const dmsLon = decimalToDMS(lon)
  return (
    <Root>
      <TextGroup>
        <Label>Latitude</Label>
        <DMSLatitudeEditor
          value={dmsLat}
          setValue={(value: DMS) => {
            const decimalValue = dmsToDecimal(value)
            setCoordinate(decimalValue, lon)
          }}
        />
      </TextGroup>
      <TextGroup style={{ marginBottom: 0 }}>
        <Label>Longitude</Label>
        <DMSLongitudeEditor
          value={dmsLon}
          setValue={(value: DMS) => {
            const decimalValue = dmsToDecimal(value)
            setCoordinate(lat, decimalValue)
          }}
        />
      </TextGroup>
    </Root>
  )
}

export default LatLonDMSPointEditor
