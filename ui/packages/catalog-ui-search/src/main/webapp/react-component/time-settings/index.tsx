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
import styled from 'styled-components'

import MarionetteRegionContainer from '../marionette-region-container'

const PropertyView = require('../../component/property/property.view')

const Root = styled.div`
  overflow: auto;
  padding: ${props => props.theme.minimumSpacing};
`

const Time = styled.div`
  width: 100%;
  font-weight: bolder;
  padding-top: ${props => props.theme.minimumSpacing};
  padding-right: ${props => props.theme.minimumSpacing};
  padding-bottom: ${props => props.theme.minimumSpacing};
  padding-left: ${props => props.theme.minimumSpacing};
`

const TimeLabel = styled.div`
  padding-top: ${props => props.theme.minimumSpacing};
  padding-bottom: ${props => props.theme.minimumSpacing};
`

const TimeValue = styled.div`
  padding-top: ${props => props.theme.minimumSpacing};
  padding-left: ${props => props.theme.minimumSpacing};
`

type Props = {
  timeZoneModel: any
  timeFormatModel: any
  currentTime: string
}

type MarionetteRegionView = {
  turnOnEditing: () => void
}

class TimeSettingsPresentation extends React.Component<Props, {}> {
  timeZoneView: MarionetteRegionView
  timeFormatView: MarionetteRegionView

  componentDidMount = () => {
    this.timeZoneView.turnOnEditing()
    this.timeFormatView.turnOnEditing()
  }

  render = () => {
    if (!this.timeZoneView)
      this.timeZoneView = new PropertyView({ model: this.props.timeZoneModel })

    if (!this.timeFormatView)
      this.timeFormatView = new PropertyView({
        model: this.props.timeFormatModel,
      })

    return (
      <Root {...this.props}>
        <MarionetteRegionContainer view={this.timeZoneView} />
        <MarionetteRegionContainer view={this.timeFormatView} />
        <Time>
          <TimeLabel>Current Time (example)</TimeLabel>
          <TimeValue>{this.props.currentTime}</TimeValue>
        </Time>
      </Root>
    )
  }
}

export default hot(module)(TimeSettingsPresentation)
