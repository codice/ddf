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


type Props = {
  isMeasuringDistance: Boolean
  currentDistance: number
  left: String
  top: String
}

const Root = styled.div<Props>`
  font-family: 'Inconsolata', 'Lucida Console', monospace;
  background: ${props => props.theme.backgroundModal};
  display: block;
  width: auto;
  height: auto;
  font-size: ${props => props.theme.mediumFontSize};
  position: absolute;
  text-align: left;
  padding: ${props => props.theme.minimumSpacing};
  max-width: 50%;
`

const DistanceInfoText = styled.div`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`

/*
 * Formats the current distance value to a string with the appropriate unit of measurement.
 */
const getDistanceText = (distance: number) => {
  // use meters when distance is under 1000m and convert to kilometers when ≥1000m
  const distanceText =
    distance < 1000 ? `${distance} m` : `${(distance * 0.001).toFixed(2)} km`

  return distanceText
}

const render = (props: Props) => {
  console.log('rendered')
  const distance = props.currentDistance ? props.currentDistance : 0

  return (
    <Root {...props} style={{ left: props.left, top: props.top}}>
      <DistanceInfoText>{getDistanceText(distance)}</DistanceInfoText>
    </Root>
  )
}

export default render
