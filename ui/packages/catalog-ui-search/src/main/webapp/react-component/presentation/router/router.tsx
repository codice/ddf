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
import styled from '../../styles/styled-components'
import { CustomElement } from '../../styles/mixins'
import Routes from '../../container/routes-container'

const Router = styled.div`
  ${CustomElement} overflow: hidden;

  > *:first-child {
    height: calc(2 * ${props => props.theme.minimumLineSize});
  }

  > *:not(:first-child) {
    height: calc(100% - 2 * ${props => props.theme.minimumLineSize});
  }
`

interface Props {
  nav: React.ReactNode
  routeDefinitions: any
}

export default function(props: Props) {
  return (
    <Router>
      {props.nav}
      <Routes isMenu={false} routeDefinitions={props.routeDefinitions} />
    </Router>
  )
}
