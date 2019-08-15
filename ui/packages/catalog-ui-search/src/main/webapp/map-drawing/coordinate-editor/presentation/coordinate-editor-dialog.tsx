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
import { CoordinateUnit, LAT_LON_DMS, LAT_LON, USNG, UTM } from '../units'
import {
  Black,
  Silver,
  Grey,
  White,
  ButtonColor,
  SubmitButtonColor,
} from './colors'
import styled from 'styled-components'
import { HTMLAttributes } from '../../../react-component/html'
import { Dropshadow } from '../../../react-component/styles/mixins'

type Props = HTMLAttributes & {
  /** Called when ok button is clicked */
  onOk: () => void
  /** Called when unit changes (should set unit) */
  setUnit: (unit: CoordinateUnit) => void
  /** Children nodes to display in dialog */
  children: React.ReactNode
  /** Coordinate unit to display */
  unit: CoordinateUnit
}

const Root = styled.div`
  margin: 0;
  padding: ${props => props.theme.mediumSpacing};
  display: flex;
  flex-direction: column;
  background-color: ${White};
  ${Dropshadow};
`

const TabRow = styled.div`
  margin: 0;
  padding: 0;
  display: flex;
  justify-content: flex-start;
`

const ControlsRow = styled.div`
  margin: 0;
  padding: 0;
  display: flex;
  justify-content: center;
`

const Body = styled.div`
  margin: 0;
  margin-bottom: ${props => props.theme.minimumSpacing};
  margin-top: ${props => props.theme.mediumSpacing};
  padding: 0;
  display: flex;
`

const Tab = styled.div<{ isSelected: boolean }>`
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: ${({ theme }) => theme.minimumFontSize};
  padding: ${({ theme }) => theme.minimumSpacing};
  cursor: pointer;
  color: ${Black};
  background-color: ${White};
  border: 1px solid ${Grey};
  border-bottom: 1px solid
    ${props => (props.isSelected ? White(props) : Grey(props))};
  :hover {
    background-color: ${props =>
      props.isSelected ? White(props) : Silver(props)};
  }
`
const Spacer = styled.div`
  flex-grow: 1;
  border-bottom: 1px solid ${Grey};
`

const Button = styled.div<{ isSubmit: boolean; onClick: () => void }>`
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: ${props => props.theme.minimumFontSize};
  line-height: ${props => props.theme.minimumButtonSize};
  height: ${props => props.theme.minimumButtonSize};
  padding: 0;
  margin: 0;
  opacity: ${props => props.theme.minimumOpacity};
  cursor: pointer;
  color: ${White};
  width: 8em;
  background-color: ${props =>
    props.isSubmit ? SubmitButtonColor(props) : ButtonColor(props)};
  :hover {
    opacity: 1;
  }
`

const CoordinateEditorDialog: React.SFC<Props> = ({
  onOk,
  setUnit,
  children,
  unit,
  ...rest
}) => {
  const renderTab = (tabUnit: CoordinateUnit): React.ReactNode => (
    <Tab isSelected={tabUnit === unit} onClick={() => setUnit(tabUnit)}>
      {tabUnit}
    </Tab>
  )
  return (
    <Root {...rest}>
      <TabRow>
        {renderTab(LAT_LON)}
        {renderTab(LAT_LON_DMS)}
        {renderTab(USNG)}
        {renderTab(UTM)}
        <Spacer />
      </TabRow>
      <Body>{children}</Body>
      <ControlsRow>
        <Button onClick={onOk} isSubmit={true}>
          OK
        </Button>
      </ControlsRow>
    </Root>
  )
}

export default CoordinateEditorDialog
