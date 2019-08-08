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
import { BBoxEditorProps as Props } from '../bbox-editor-props'
import { LatitudeInput, LongitudeInput } from './lat-lon-input'
import * as Common from './common-styles'

const Root = Common.BBoxRoot
const TextGroup = styled.label`
  margin: 0;
  padding: 0;
  display: flex;
`
const Label = Common.CompactLabel

const LatLonBBoxEditor: React.SFC<Props> = ({
  north,
  south,
  east,
  west,
  setBBox,
}) => (
  <Root flexDirection="column">
    <TextGroup>
      <Label>North</Label>
      <LatitudeInput
        value={north}
        onChange={(value: number) => {
          setBBox({
            north: value,
            south,
            east,
            west,
          })
        }}
      />
    </TextGroup>
    <TextGroup>
      <Label>South</Label>
      <LatitudeInput
        value={south}
        onChange={(value: number) => {
          setBBox({
            north,
            south: value,
            east,
            west,
          })
        }}
      />
    </TextGroup>
    <TextGroup>
      <Label>East</Label>
      <LongitudeInput
        value={east}
        onChange={(value: number) => {
          setBBox({
            north,
            south,
            east: value,
            west,
          })
        }}
      />
    </TextGroup>
    <TextGroup>
      <Label>West</Label>
      <LongitudeInput
        value={west}
        onChange={(value: number) => {
          setBBox({
            north,
            south,
            east,
            west: value,
          })
        }}
      />
    </TextGroup>
  </Root>
)

export default LatLonBBoxEditor
