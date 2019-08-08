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
import UTMPointEditor from './utm-point-editor'
import { BBoxEditorProps as Props } from '../bbox-editor-props'
import * as Common from './common-styles'

const Root = Common.BBoxRoot
const PointGroup = styled.div`
  margin: 0;
  padding: 0;
  font-size: ${props => props.theme.minimumFontSize};
  display: flex;
  margin-right: ${props => props.theme.minimumSpacing};
  flex-direction: column;
  align-items: center;
  flex-grow: 1;
`
const Label = styled.div`
  margin: 0;
  margin-bottom: ${props => props.theme.minimumSpacing};
  padding: 0;
  font-size: ${props => props.theme.minimumFontSize};
  display: flex;
  font-weight: bold;
`

class UTMBBoxEditor extends React.Component<Props> {
  render() {
    const { north, south, east, west, setBBox } = this.props
    return (
      <Root flexDirection="row">
        <PointGroup>
          <Label>Upper Left</Label>
          <UTMPointEditor
            lat={north}
            lon={west}
            setCoordinate={(lat, lon) => {
              setBBox({
                north: lat,
                south,
                east,
                west: lon,
              })
            }}
          />
        </PointGroup>
        <PointGroup>
          <Label>Lower Right</Label>
          <UTMPointEditor
            lat={south}
            lon={east}
            setCoordinate={(lat, lon) => {
              setBBox({
                north,
                south: lat,
                east: lon,
                west,
              })
            }}
          />
        </PointGroup>
      </Root>
    )
  }
}

export default UTMBBoxEditor
