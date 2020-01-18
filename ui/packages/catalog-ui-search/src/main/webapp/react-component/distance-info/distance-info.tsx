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
import withListenTo, { WithBackboneProps } from '../backbone-container'

const LEFT_OFFSET = 390
const TOP_OFFSET = 180

const DistanceInfoPresentation = require('./presentation').default

const mapPropsToState = (props: Props) => {
  const { map } = props
  const distance = map.get('currentDistance')
  return {
    showDistance: map.get('measurementState') === 'START' && distance,
    currentDistance: distance,
    left: map.get('distanceInfo')['left'] - LEFT_OFFSET + 'px',
    top: map.get('distanceInfo')['top'] - TOP_OFFSET + 'px',
  }
}

type Props = {
  map: Backbone.Model
} & WithBackboneProps

type State = {
  showDistance: Boolean
  currentDistance: Number
  left: String
  top: String
}

class DistanceInfo extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = { ...mapPropsToState(props), showDistance: false }
  }

  componentDidMount() {
    this.listenToMap()
  }

  componentWillUnmount() {
    const { stopListening, map } = this.props
    stopListening(map, 'change:currentDistance', this.handleDistanceChange)
    stopListening(
      map,
      'change:measurementState',
      this.handleMeasurementStateChange
    )
  }

  listenToMap = () => {
    const { listenTo, map } = this.props
    listenTo(map, 'change:currentDistance', this.handleDistanceChange)
    listenTo(map, 'change:measurementState', this.handleMeasurementStateChange)
  }

  handleDistanceChange = () => {
    this.setState(mapPropsToState(this.props))
  }

  handleMeasurementStateChange = () => {
    this.setState(mapPropsToState(this.props))
  }

  render() {
    return this.state.showDistance ? (
      <DistanceInfoPresentation {...this.state} />
    ) : null
  }
}

export default hot(module)(withListenTo(DistanceInfo))
