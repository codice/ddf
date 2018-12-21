/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
import * as React from 'react'
import styled from '../../styles/styled-components'
import { hot } from 'react-hot-loader'
import Enum from '../../container/enum'

type Props = {
  selected: string
  example: string
  update: (selected: string) => void
}

const Root = styled<
  { selected: string; example: string; update: (selected: string) => void },
  'div'
>('div')`
  overflow: auto;
  padding: ${props => props.theme.minimumSpacing}
    ${props => props.theme.minimumSpacing};
`
const ExampleCoordinates = styled.div`
  display: block;
  width: 100%;
  white-space: nowrap;
  padding: ${props => props.theme.minimumSpacing};
  position: relative;

  &.example-label,
  &.example-value {
    width: 50%;
    display: inline-block;
    vertical-align: middle;
    position: relative;
  }

  &.example-label {
    text-align: right;
  }
`

const Label = styled.label`
  .example-label & {
    vertical-align: middle;
    cursor: auto;
    font-weight: bolder;
    max-width: calc(100% - ${props => props.theme.minimumButtonSize});
    margin: 0px;
    line-height: 1.4;
    padding: ${props => props.theme.minimumSpacing} 0px;
    min-height: ${props => props.theme.minimumButtonSize};
    overflow: hidden;
    text-overflow: ellipsis;
    word-wrap: normal;
    white-space: normal;
  }
`

const render = (props: Props) => {
  const { selected, example, update } = props
  return (
    <Root {...props}>
      <Enum
        options={[
          { label: 'Degrees, Minutes, Seconds', value: 'degrees' },
          { label: 'Decimal', value: 'decimal' },
          { label: 'MGRS', value: 'mgrs' },
          { label: 'UTM/UPS', value: 'utm' },
        ]}
        value={selected}
        label="Coordinate Format"
        onChange={update}
      />

      <ExampleCoordinates>
        <div className="example-label">
          <Label>Example Coordinates</Label>
        </div>
        <div className="example-value">
          <span>{example}</span>
        </div>
      </ExampleCoordinates>
    </Root>
  )
}

export default hot(module)(render)
