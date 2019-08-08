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
import { LatitudeInput, LongitudeInput } from './lat-lon-input'
import * as Common from './common-styles'
import Props from '../point-editor-props'

const Root = Common.Column
const TextGroup = Common.SpacedInputLabelRow
const Label = Common.Label

const LatLonPointEditor: React.SFC<Props> = ({ lat, lon, setCoordinate }) => (
  <Root>
    <TextGroup>
      <Label>Latitude</Label>
      <LatitudeInput
        value={lat}
        onChange={(value: number) => setCoordinate(value, lon)}
      />
    </TextGroup>
    <TextGroup style={{ marginBottom: 0 }}>
      <Label>Longitude</Label>
      <LongitudeInput
        value={lon}
        onChange={(value: number) => setCoordinate(lat, value)}
      />
    </TextGroup>
  </Root>
)

export default LatLonPointEditor
