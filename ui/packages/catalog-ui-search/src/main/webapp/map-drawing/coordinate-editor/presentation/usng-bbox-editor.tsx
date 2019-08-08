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
import * as Common from './common-styles'
import { BBox, BBoxEditorProps as Props } from '../bbox-editor-props'
import { Converter } from 'usng.js'
import { latLonTo } from '../coordinate-converter'
import USNGInput from './usng-input'

const Root = Common.BBoxRoot
const TextGroup = Common.Row
const Label = Common.Label

class USNGBBoxEditor extends React.Component<Props> {
  unitConverter: {
    isUSNG: (usng: string) => 0 | string
    USNGtoLL: (usng: 0 | string, getCenter: boolean) => BBox
  }
  constructor(props: Props) {
    super(props)
    this.unitConverter = new (Converter as any)()
  }
  render() {
    const { north, south, east, west, setBBox } = this.props
    const usng = latLonTo.USNGBox(north, south, east, west)
    return (
      <Root flexDirection="column">
        <TextGroup>
          <Label>USNG/MGRS</Label>
          <USNGInput
            value={usng}
            onChange={(usng: string) => {
              const matrix: string | 0 = this.unitConverter.isUSNG(usng)
              if (matrix) {
                const bbox = this.unitConverter.USNGtoLL(matrix, false)
                setBBox(bbox)
              }
            }}
          />
        </TextGroup>
      </Root>
    )
  }
}

export default USNGBBoxEditor
