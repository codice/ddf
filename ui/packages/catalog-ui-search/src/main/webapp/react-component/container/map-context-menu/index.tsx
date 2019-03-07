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
import CopyCoordinates from '../../presentation/copy-coordinates'
import { Menu, MenuItem } from '../../menu'
import styled from '../../styles/styled-components'

const Icon = styled.div`
  display: inline-block;
  text-align: center;
  width: ${props => props.theme.minimumFontSize};
`
const Title = styled.div`
  display: inline-block;
  margin-left: ${props => props.theme.minimumSpacing};
  margin-right: ${props => props.theme.minimumSpacing};
`
const Description = styled.div`
  display: block;
  margin-left: calc(
    ${props => props.theme.minimumSpacing} +
      ${props => props.theme.minimumFontSize}
  );
  margin-right: ${props => props.theme.minimumSpacing};
  line-height: normal;
  margin-top: calc(0 - ${props => props.theme.minimumSpacing});
  margin-bottom: ${props => props.theme.minimumSpacing};
`

interface Props {
  onChange: (value: string) => void
  mouseLat: number | null | undefined
  mouseLon: number | null | undefined
  target: string | null | undefined
  clickDms: string
  clickLat: string
  clickLon: string
  clickMgrs: string
  clickUtmUps: string
  selectionCount: number
  closeMenu: () => void
}

export const MapContextMenu = (props: Props) => {
  const hasTarget = typeof props.target === 'string'
  const hasSelection = props.selectionCount > 0
  const hasMouseCoordinates =
    typeof props.mouseLat !== 'undefined' &&
    typeof props.mouseLon !== 'undefined'
  const menuItems = []
  if (hasMouseCoordinates) {
    menuItems.push(
      <MenuItem key="0" value="CopyCoordinates">
        <CopyCoordinates
          clickDms={props.clickDms}
          clickLat={props.clickLat}
          clickLon={props.clickLon}
          clickMgrs={props.clickMgrs}
          clickUtmUps={props.clickUtmUps}
          closeParent={props.closeMenu}
        />
      </MenuItem>
    )
  }
  menuItems.push(
    <MenuItem key="10" value="Histogram">
      <Icon className="interaction-icon fa fa-bar-chart" />
      <Title>View Histogram</Title>
    </MenuItem>
  )
  if (hasSelection) {
    menuItems.push(
      <MenuItem key="11" value="HistogramSelection">
        <Icon className="interaction-icon fa fa-bar-chart" />
        <Title>View Histogram</Title>
        <Description>({props.selectionCount} selected)</Description>
      </MenuItem>
    )
  }
  if (hasTarget) {
    menuItems.push(
      <MenuItem key="20" value="Inspector">
        <Icon className="interaction-icon fa fa-info" />
        <Title>View Inspector</Title>
        <Description>({props.target})</Description>
      </MenuItem>
    )
  }
  if (hasSelection) {
    menuItems.push(
      <MenuItem key="21" value="InspectorSelection">
        <Icon className="interaction-icon fa fa-info" />
        <Title>View Inspector</Title>
        <Description>({props.selectionCount} selected)</Description>
      </MenuItem>
    )
  }
  return <Menu onChange={props.onChange}>{menuItems}</Menu>
}
