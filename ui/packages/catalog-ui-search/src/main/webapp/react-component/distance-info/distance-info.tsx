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

const DistanceInfoPresentation = require('./presentation').default

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

    const move => (evt : Event) {
        if (evt.target.tagName.toLowerCase() === 'svg') {
            evt.preventDefault();
            var svgPos = this.refs.svg.getBoundingClientRect();
            var x = evt.clientX,
                    y = evt.clientY;
            if (evt.type==='touchmove') {
                x = evt.touches[0].pageX,
                y = evt.touches[0].pageY;
            }
            x = x - svgPos.left;
            y = y - svgPos.top;
            var points = this.state.points;
            if (points.length > 50) {
                points.shift();
            }
            points.push({
                x: x,
                y: y
            });
            this.setState(points);
        }
    }

    render() {
        return <DistanceInfoPresentation onMouseMove={this.move.bind(this)} {...this.state} />
    }
}

export default hot(module)(withListenTo(DistanceInfo))