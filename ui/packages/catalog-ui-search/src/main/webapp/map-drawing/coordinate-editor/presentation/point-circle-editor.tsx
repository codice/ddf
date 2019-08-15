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
import { CoordinateUnit } from '../units'
import { LengthUnit } from '../../geometry'
import PointEditor from '../container/point-editor'
import LengthEditor from './length-editor'
import * as Common from './common-styles'

type PointProps = {
  /** Coordinate Unit to display */
  coordinateUnit: CoordinateUnit
  /** Latitude degrees */
  lat: number
  /** Longitued degrees */
  lon: number
  /** Set coordinate in lat/lon degrees */
  setCoordinate: (lat: number, lon: number) => void
}

type CircleProps = PointProps & {
  /** Radius */
  radius: number
  /** Radius unit of measure */
  radiusUnit: LengthUnit
  /** Set radius */
  setRadius: (buffer: number) => void
  /** Set radius unit of measure */
  setRadiusUnit: (unit: LengthUnit) => void
}

const Root = styled.div`
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  min-width: 25rem;
`
const PointRoot = styled(Root)`
  min-height: 8rem;
`
const CircleRoot = styled(Root)`
  min-height: 10rem;
`
const InputGroup = styled.label`
  margin: 0;
  margin-top: ${props => props.theme.minimumSpacing};
  padding: 0;
  display: flex;
`
const Label = Common.Label

const FixedHeightPointEditor: React.SFC<PointProps> = ({
  coordinateUnit,
  lat,
  lon,
  setCoordinate,
}) => (
  <PointRoot>
    <PointEditor
      lat={lat}
      lon={lon}
      unit={coordinateUnit}
      setCoordinate={setCoordinate}
    />
  </PointRoot>
)
const CircleEditor: React.SFC<CircleProps> = ({
  radius,
  radiusUnit,
  coordinateUnit,
  lat,
  lon,
  setRadius,
  setRadiusUnit,
  setCoordinate,
}) => (
  <CircleRoot>
    <PointEditor
      lat={lat}
      lon={lon}
      unit={coordinateUnit}
      setCoordinate={setCoordinate}
    />
    <InputGroup>
      <Label>Radius</Label>
      <LengthEditor
        length={radius}
        unit={radiusUnit}
        setUnit={setRadiusUnit}
        setLength={setRadius}
      />
    </InputGroup>
  </CircleRoot>
)

export { FixedHeightPointEditor, CircleEditor }
