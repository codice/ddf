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
import styled from 'styled-components'
import { DMS, decimalToDMS, dmsToDecimal } from '../dms-formatting'
import { DMSLatitudeEditor, DMSLongitudeEditor } from './dms-value-editor'
import { BBox, BBoxEditorProps as Props } from '../bbox-editor-props'
import * as Common from './common-styles'

const Root = Common.BBoxRoot
const TextGroup = styled.label`
  margin: 0;
  padding: 0;
  display: flex;
`
const Label = Common.CompactLabel

const LatLonDMSBBoxEditor: React.SFC<Props> = ({ setBBox, ...rest }) => {
  const bbox: BBox = rest
  const north = decimalToDMS(bbox.north)
  const south = decimalToDMS(bbox.south)
  const east = decimalToDMS(bbox.east)
  const west = decimalToDMS(bbox.west)
  return (
    <Root flexDirection="column">
      <TextGroup>
        <Label>North</Label>
        <DMSLatitudeEditor
          value={north}
          setValue={(value: DMS) => {
            setBBox({
              ...bbox,
              north: dmsToDecimal(value),
            })
          }}
        />
      </TextGroup>
      <TextGroup>
        <Label>South</Label>
        <DMSLatitudeEditor
          value={south}
          setValue={(value: DMS) => {
            setBBox({
              ...bbox,
              south: dmsToDecimal(value),
            })
          }}
        />
      </TextGroup>
      <TextGroup>
        <Label>East</Label>
        <DMSLongitudeEditor
          value={east}
          setValue={(value: DMS) => {
            setBBox({
              ...bbox,
              east: dmsToDecimal(value),
            })
          }}
        />
      </TextGroup>
      <TextGroup>
        <Label>West</Label>
        <DMSLongitudeEditor
          value={west}
          setValue={(value: DMS) => {
            setBBox({
              ...bbox,
              west: dmsToDecimal(value),
            })
          }}
        />
      </TextGroup>
    </Root>
  )
}

export default LatLonDMSBBoxEditor
