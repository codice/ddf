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
import CopyCoordinates from '../copy-coordinates'
import { Menu, MenuItem } from '../menu'
import styled from 'styled-components'

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
  mouseLat?: number
  mouseLon?: number
  target?: string
  coordinateValues: {
    dms: string
    lat: string
    lon: string
    mgrs: string
    utmUps: string
  }
  selectionCount: number
  closeMenu: () => void
  key: number
}

const renderCopyCoordinatesMenu = ({ coordinateValues, closeMenu }: Props) => (
  <MenuItem value="CopyCoordinates">
    <CopyCoordinates
      coordinateValues={coordinateValues}
      closeParent={closeMenu}
    />
  </MenuItem>
)

const renderMenu = ({ onChange, key }: Props, menuItems: any[]) => (
  <Menu key={key} onChange={onChange}>
    {menuItems}
  </Menu>
)

export const MapContextMenu = (props: Props) => {
  const { mouseLat, mouseLon } = props
  const hasMouseCoordinates =
    typeof mouseLat === 'number' && typeof mouseLon === 'number'
  const menuItems = []
  if (hasMouseCoordinates) {
    menuItems.push(renderCopyCoordinatesMenu(props))
  }

  const keyedItems = menuItems.map((m, i) => ({ key: i, ...m }))
  return renderMenu(props, keyedItems)
}
