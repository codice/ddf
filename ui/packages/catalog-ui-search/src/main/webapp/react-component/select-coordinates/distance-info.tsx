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
import styled from 'styled-components'
import { hot } from 'react-hot-loader'
import withListenTo, { WithBackboneProps } from '../backbone-container'

const MouseTooltip = require('react-sticky-mouse-tooltip')

const mapPropsToState = (props: Props) => {
  const { map } = props
  return {
    coordinates: {
      lat: map.get('mouseLat'),
      lon: map.get('mouseLon'),
    },
    isMeasuringDistance: map.get('measurementState') === 'START',
    currentDistance: map.get('currentDistance'),
  }
}

type Coordinates = {
  lat: number
  lon: number
}

type Props = {
  map: Backbone.Model
} & WithBackboneProps

type State = {
  coordinates: Coordinates
  isMeasuringDistance: Boolean
  currentDistance: Number
}

const DistanceInfoText = styled.div`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`

class DistanceInfo extends React.Component<Props, State> {
    constructor(props: Props) {
      super(props)
      this.state = mapPropsToState(props)
      this.listenToMap()
    }

    listenToMap = () => {
        const { listenTo, map } = this.props
        listenTo(
          map,
          'change:mouseLat change:mouseLon change:currentDistance',
          this.handleChange
        )
    }

    handleChange = () => {
        console.log('change handled here')
        this.setState(mapPropsToState(this.props))
    }

    render() {
        console.log('rendered')
        const distance = (this.state.currentDistance) ? this.state.currentDistance : 0

        return (
            <div>
                <MouseTooltip
                    visible={this.state.isMeasuringDistance}
                    offsetX={15}
                    offsetY={10}>
                    <DistanceInfoText>{distance}</DistanceInfoText>
                </MouseTooltip>
            </div>
        )
    }
}

export default hot(module)(withListenTo(DistanceInfo))