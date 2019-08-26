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
import React from 'react'
import styled from 'styled-components'

const BottomBarContainer = styled.div`
  position: absolute;
  width: 100%;
  bottom: 0;
  display: flex;
  flex-flow: row-reverse;
  border-top: ${props => props.theme.backgroundNavigation} 2px solid;
  padding: ${props => props.theme.minimumSpacing};
`

const BottomMessageContainer = styled.div`
  position: absolute;
  padding: ${props => props.theme.minimumSpacing};
  font-size: ${props => props.theme.minimumFontSize};
  align-self: center;
  left: ${props => props.theme.minimumSpacing};
`

const BottomBar = props => {
  return (
    <BottomBarContainer>
      <BottomMessageContainer>{props.bottomBarText}</BottomMessageContainer>
      {props.children}
    </BottomBarContainer>
  )
}

export { BottomBar }
