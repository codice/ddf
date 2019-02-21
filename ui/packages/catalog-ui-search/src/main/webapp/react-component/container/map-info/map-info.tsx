/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
import * as React from 'react'
// import styled from '../../styles/styled-components'

const user = require('../../../component/singletons/user-instance.js')

import withListenTo, { WithBackboneProps } from '../backbone-container'
import MapInfoPresentation from '../../presentation/map-info'
import { hot } from 'react-hot-loader'

type State = {
  lat: number
  lon: number
  format: string
}
type Props = {
  map: Backbone.Model
} & WithBackboneProps

const mapPropsToState = (props: Props) => {
  const { map } = props
  return {
    lat: map.get('mouseLat'),
    lon: map.get('mouseLon'),
    format: getCoordinateFormat(),
  }
}

const getCoordinateFormat = () =>
  user
    .get('user')
    .get('preferences')
    .get('coordinateFormat')

class MapInfo extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    // this.state = {
    //   selected: `34°26′56″N 083°42′15″W`
    // }
    this.state = mapPropsToState(props)
    this.listenToMap()
  }

  listenToMap = () => {
    const { listenTo, map } = this.props
    listenTo(
      map,
      'change:mouseLat change:mouseLon change:target',
      this.handleChange
    )
    listenTo(
      user.get('user').get('preferences'),
      'change:coordinateFormat',
      this.handleChange
    )
  }
  handleChange = () => {
    this.setState(mapPropsToState(this.props))
  }

  // update(newFormat: string) {
  //   this.setState({ selected: newFormat })
  // }

  render() {
    return <MapInfoPresentation {...this.state} />
  }
}

export default hot(module)(withListenTo(MapInfo))
