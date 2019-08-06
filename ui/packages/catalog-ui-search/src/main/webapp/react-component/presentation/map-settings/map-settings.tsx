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
import styled from '../../styles/styled-components'
import { hot } from 'react-hot-loader'
import Enum from '../../container/enum'
import ExampleCoordinates from './example-coordinates'

type Props = {
  selected: string
  update: (selected: string) => void
}

const Root = styled.div`
  overflow: auto;
  min-width: ${props => props.theme.minimumScreenSize};
  padding: ${props => props.theme.minimumSpacing}
    ${props => props.theme.minimumSpacing};
`

const render = (props: Props) => {
  const { selected, update } = props
  return (
    <Root>
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

      <ExampleCoordinates {...props} />
    </Root>
  )
}

export default hot(module)(render)
export const testComponent = render
