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
import withListenTo, { WithBackboneProps } from '../backbone-container'
import MapInfoPresentation from './presentation'
import { hot } from 'react-hot-loader'
import { Coordinates, Format, Attribute } from '.'

const user = require('../../component/singletons/user-instance.js')
const properties = require('properties')

type State = {
  coordinates: Coordinates
  format: Format
  attributes: Attribute[]
  measurementState: String
  currentDistance: number
}

type Props = {
  map: Backbone.Model
} & WithBackboneProps

const mapPropsToState = (props: Props) => {
  const { map } = props
  return {
    coordinates: {
      lat: map.get('mouseLat'),
      lon: map.get('mouseLon'),
    },
    format: getCoordinateFormat(),
    attributes: getAttributes(map),
    measurementState: map.get('measurementState'),
    currentDistance: map.get('currentDistance'),
  }
}

const getAttributes = (map: Backbone.Model) => {
  if (map.get('targetMetacard') === undefined) {
    return []
  }
  return properties.summaryShow
    .map((attribute: string) => {
      const value = map
        .get('targetMetacard')
        .get('metacard')
        .get('properties')
        .get('measurementState')
        .get('currentDistance')
        .get(attribute)
      return { name: attribute, value }
    })
    .filter(({ value }: Attribute) => value !== undefined)
}

const getCoordinateFormat = () =>
  user
    .get('user')
    .get('preferences')
    .get('coordinateFormat')

class MapInfo extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = mapPropsToState(props)
    this.listenToMap()
  }

  listenToMap = () => {
    const { listenTo, map } = this.props
    listenTo(
      map,
      'change:mouseLat change:mouseLon change:targetMetacard change:currentDistance',
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

  render() {
    return <MapInfoPresentation {...this.state} />
  }
}

export default hot(module)(withListenTo(MapInfo))
