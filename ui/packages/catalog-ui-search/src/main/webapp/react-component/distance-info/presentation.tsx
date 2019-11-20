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


type Coordinates = {
  lat: number
  lon: number
}

type Props = {
  coordinates: Coordinates
  isMeasuringDistance: Boolean
  currentDistance: Number
}

const Root = styled.div<Props>`
  font-family: 'Inconsolata', 'Lucida Console', monospace;
  display: block;
  width: auto;
  height: auto;
  position: absolute;
  left: 0px;
  bottom: 0px;
  text-align: left;
  max-width: 50%;
`

const DistanceInfoText = styled.div`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`

const render = (props: Props) => {
    console.log('rendered')
    const distance = (props.currentDistance) ? props.currentDistance : 0

    return (
        <Root {...props}>
                <DistanceInfoText>{distance}</DistanceInfoText>
        </Root>
    )
}

export default render