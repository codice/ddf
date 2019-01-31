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
`

const renderMenu = (onSelect: voidFunc) => {
  return (
    <Button onClick={onSelect} title="Save Current View as Home Location">
      <span>Set Home </span>
      <span className="cf cf-map-marker" />
    </Button>
  )
}

const ZoomToHome = (props: Props) => {
  const { saveHome, goHome } = props
  return (
    <SplitButton
      title="Return To Home Location"
      className="is-split-button"
      onSelect={goHome}
      style={{
        width: '165px',
      }}
    >
      {{
        label: 'Home',
        menu: renderMenu(saveHome),
      }}
    </SplitButton>
  )
}

export default hot(module)(ZoomToHome)
