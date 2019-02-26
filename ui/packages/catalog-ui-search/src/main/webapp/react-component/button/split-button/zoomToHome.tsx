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
import SplitButton from '.'
import styled from '../../styles/styled-components'
import { hot } from 'react-hot-loader'

type voidFunc = () => void

type Props = {
  goHome: voidFunc
  saveHome: voidFunc
}
const Button = styled.button`
  flex: 1;
  margin-left: ${props => props.theme.mediumSpacing};
  margin-right: ${props => props.theme.mediumSpacing};
`
const Icon = styled.div`
  display: inline-block;
  background-color: inherit;
  margin-left: ${props => props.theme.minimumSpacing};
  line-height: ${props => `${props.theme.minimumLineSize} !important`};
  text-align: center;
  font-size: ${props => props.theme.mediumFontSize};
`

const renderMenu = (onSelect: voidFunc) => {
  return (
    <Button onClick={onSelect} title="Save Current View as Home Location">
      <span>
        Set Home
        <Icon className="cf cf-map-marker" />
      </span>
    </Button>
  )
}

const ZoomToHome = (props: Props) => {
  const { saveHome, goHome } = props
  return (
    <SplitButton title="Return To Home Location" onSelect={goHome}>
      {{
        label: (
          <span>
            Home
            <Icon className="fa fa-home" />
          </span>
        ),
        menu: renderMenu(saveHome),
      }}
    </SplitButton>
  )
}

export default hot(module)(ZoomToHome)
