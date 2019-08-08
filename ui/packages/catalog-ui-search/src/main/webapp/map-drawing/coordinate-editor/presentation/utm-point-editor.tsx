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
import NumberInput from './number-input'
import * as Common from './common-styles'
import { Converter } from 'usng.js'
import { latLonTo, LatLonDD } from '../coordinate-converter'
const Dropdown = require('../../../react-component/dropdown')
const { Menu, MenuItem } = require('../../../react-component/menu')
import Props from '../point-editor-props'

const MAX_EASTING = 834000
const MIN_EASTING = 160000
const MAX_NORTHING = 10000000
const MIN_NORTHING = 0
const NUMBER_OF_ZONE_VALUES = 61

const Root = Common.Column
const InputGroup = styled.label`
  margin: 0;
  padding: 0;
  display: flex;
  margin-bottom: ${props => props.theme.minimumSpacing};
`
const SelectGroup = Common.Row
const Label = Common.Label
const HemisphereButton = Common.SpacedToggleButton
const UTMInput = styled(NumberInput)`
  width: 6em;
`

class UTMPointEditor extends React.Component<Props> {
  unitConverter: {
    UTMtoLLwithNS: (
      northing: number,
      easting: number,
      zone: number,
      precision: null,
      hemisphere: 'N' | 'S'
    ) => LatLonDD
  }
  constructor(props: Props) {
    super(props)
    this.unitConverter = new (Converter as any)()
  }
  render() {
    const { lat, lon, setCoordinate } = this.props
    const { easting, northing, zone, hemisphere } = latLonTo.UTM(lat, lon)
    return (
      <Root>
        <InputGroup>
          <Label>Easting</Label>
          <UTMInput
            value={easting}
            maxValue={MAX_EASTING}
            minValue={MIN_EASTING}
            decimalPlaces={0}
            onChange={(value: number) => {
              const { lat, lon } = this.unitConverter.UTMtoLLwithNS(
                northing,
                value,
                zone,
                null,
                hemisphere
              )
              setCoordinate(lat, lon)
            }}
          />
        </InputGroup>
        <InputGroup>
          <Label>Northing</Label>
          <UTMInput
            value={northing}
            maxValue={MAX_NORTHING}
            minValue={MIN_NORTHING}
            decimalPlaces={0}
            onChange={(value: number) => {
              const { lat, lon } = this.unitConverter.UTMtoLLwithNS(
                value,
                easting,
                zone,
                null,
                hemisphere
              )
              setCoordinate(lat, lon)
            }}
          />
        </InputGroup>
        <InputGroup>
          <Label>Zone</Label>
          <Dropdown label={zone}>
            <Menu
              value={zone}
              onChange={(value: number) => {
                const { lat, lon } = this.unitConverter.UTMtoLLwithNS(
                  northing,
                  easting,
                  value,
                  null,
                  hemisphere
                )
                setCoordinate(lat, lon)
              }}
            >
              {Array(NUMBER_OF_ZONE_VALUES)
                .fill(0)
                .map((_: number, zone: number) => (
                  <MenuItem key={zone} value={zone} />
                ))}
            </Menu>
          </Dropdown>
        </InputGroup>
        <SelectGroup>
          <Label>Hemisphere</Label>
          <HemisphereButton
            title="Northern Hemisphere"
            isSelected={hemisphere === 'N'}
            onClick={() => {
              const { lat, lon } = this.unitConverter.UTMtoLLwithNS(
                northing,
                easting,
                zone,
                null,
                'N'
              )
              setCoordinate(lat, lon)
            }}
          >
            N
          </HemisphereButton>
          <HemisphereButton
            title="Southern Hemisphere"
            isSelected={hemisphere === 'S'}
            onClick={() => {
              const { lat, lon } = this.unitConverter.UTMtoLLwithNS(
                northing,
                easting,
                zone,
                null,
                'S'
              )
              setCoordinate(lat, lon)
            }}
          >
            S
          </HemisphereButton>
        </SelectGroup>
      </Root>
    )
  }
}

export default UTMPointEditor
