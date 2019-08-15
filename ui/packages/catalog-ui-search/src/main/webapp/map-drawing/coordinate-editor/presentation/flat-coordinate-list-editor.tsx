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
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faPlus, faMinus } from '@fortawesome/free-solid-svg-icons'
import styled from 'styled-components'
import { CoordinateUnit } from '../units'
import { LengthUnit } from '../../geometry'
import PointEditor from '../container/point-editor'
import LengthEditor from './length-editor'
import ToggleButton from './toggle-button'
import CoordinateValue from './coordinate-value'
import * as Common from './common-styles'

type Props = {
  /** Buffer */
  buffer: number
  /** Buffer unit of measure */
  bufferUnit: LengthUnit
  /** Array of coordinates in lon/lat */
  coordinateList: [number, number][]
  /** Coordinate unit */
  coordinateUnit: CoordinateUnit
  /** Lattiude of current selected coordinate */
  lat: number
  /** Longitude of current selected coordinate */
  lon: number
  /** Sets buffer */
  setBuffer: (buffer: number) => void
  /** Sets buffer unit of measure */
  setUnit: (unit: LengthUnit) => void
  /** Sets coordinate at selected point */
  setCoordinate: (lat: number, lon: number) => void
  /** Adds new coordinate at selected point */
  addCoordinate: () => void
  /** Deletes coordiante from selected point */
  deleteCoordinate: () => void
  /** Selects coordinate at index (should update selectedIndex) */
  selectCoordinate: (index: number) => void
  /** Selected coordinate index */
  selectedIndex: number
}

const Root = styled.div`
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  min-width: 25rem;
  min-height: 20rem;
`
const InputGroup = styled.label`
  margin: ${props => props.theme.minimumSpacing} 0;
  padding: 0;
  display: flex;
`
const Label = Common.Label
const List = styled.ul`
  margin: 0;
  padding: 0;
  max-height: 15rem;
  display: flex;
  flex-grow: 1;
  flex-basis: 8rem;
  flex-direction: column;
  overflow-y: auto;
  background-color: ${props => props.theme.backgroundSlideout};
`
const ListItem = styled.li`
  margin: 0;
  padding: 0;
  display: flex;
`
const ControlsGroup = styled.div`
  display: flex;
  justify-content: flex-end;
`
const Button = styled.div<{ onClick: () => void; title: string }>`
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: ${({ theme }) => theme.mediumFontSize};
  padding: ${props => props.theme.minimumSpacing};
  margin: 0;
  opacity: ${props => props.theme.minimumOpacity};
  cursor: pointer;
  :hover {
    opacity: 1;
  }
`
const CoordinateButton = styled(ToggleButton)`
  width: 100%;
`

const FlatCoordinateListEditor: React.SFC<Props> = ({
  buffer,
  bufferUnit,
  coordinateList,
  coordinateUnit,
  lat,
  lon,
  setBuffer,
  setUnit,
  setCoordinate,
  addCoordinate,
  deleteCoordinate,
  selectCoordinate,
  selectedIndex,
}) => (
  <Root>
    <PointEditor
      lat={lat}
      lon={lon}
      unit={coordinateUnit}
      setCoordinate={setCoordinate}
    />
    <InputGroup>
      <Label>Buffer</Label>
      <LengthEditor
        length={buffer}
        unit={bufferUnit}
        setUnit={setUnit}
        setLength={setBuffer}
      />
    </InputGroup>
    <ControlsGroup>
      <Button onClick={addCoordinate} title="Add New Coordinate">
        <FontAwesomeIcon icon={faPlus} />
      </Button>
      <Button onClick={deleteCoordinate} title="Delete Coordinate">
        <FontAwesomeIcon icon={faMinus} />
      </Button>
    </ControlsGroup>
    <List>
      {coordinateList.map(([coordinateLon, coordinateLat], index: number) => (
        <ListItem key={index}>
          <CoordinateButton
            isSelected={index === selectedIndex}
            onClick={() => selectCoordinate(index)}
            title={`Select Coordinate #${index + 1}`}
          >
            <CoordinateValue
              lat={coordinateLat}
              lon={coordinateLon}
              unit={coordinateUnit}
            />
          </CoordinateButton>
        </ListItem>
      ))}
    </List>
  </Root>
)

export default FlatCoordinateListEditor
