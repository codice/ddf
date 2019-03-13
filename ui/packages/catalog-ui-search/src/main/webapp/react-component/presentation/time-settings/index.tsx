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
import { hot } from 'react-hot-loader'
import styled from '../../styles/styled-components'
import * as PropertyView from '../../../component/property/property.view'

import MarionetteRegionContainer from '../../container/marionette-region-container'

const Root = styled.div`
  overflow: auto;
  padding: ${props => props.theme.minimumSpacing}
    ${props => props.theme.minimumSpacing};
`

const Time = styled.div`
  width: 100%;
  font-weight: bolder;
  padding-top: 10px;
  padding-right: 10px;
  padding-bottom: 10px;
  padding-left: 10px;
`

const TimeLabel = styled.div`
  padding-top: 10px;
  padding-bottom: 10px;
`

const TimeValue = styled.div`
  padding-top: 10px;
  padding-left: 10px;
`

type Props = {
  timeZoneModel: any
  timeFormatModel: any
  currentTime: string
}

export default hot(module)((props: Props) => (
  <Root {...props}>
    <MarionetteRegionContainer
      view={PropertyView}
      viewOptions={{ model: props.timeZoneModel }}
      bindView={view => view.turnOnEditing()}
    />
    <MarionetteRegionContainer
      view={PropertyView}
      viewOptions={{ model: props.timeFormatModel }}
      bindView={view => view.turnOnEditing()}
    />
    <Time>
      <TimeLabel>Current Time (example)</TimeLabel>
      <TimeValue>{props.currentTime}</TimeValue>
    </Time>
  </Root>
))
