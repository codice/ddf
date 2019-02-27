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
import withListenTo, { WithBackboneProps } from '../backbone-container'
import MapInfoPresentation from '../../presentation/map-info'
import { hot } from 'react-hot-loader'

const user = require('../../../component/singletons/user-instance.js')
const properties = require('properties')
const metacardDefinitions = require('component/singletons/metacard-definitions')

export interface Attribute {
  name: string
  value: string
}

interface State {
  lat: number
  lon: number
  format: string
  attributes: Attribute[]
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
    attributes: getAttributes(props),
  }
}

const getAttributes = ({ map }: Props) => {
  if (map.get('targetMetacard') === undefined) {
    return []
  }
  return properties.summaryShow
    .map((attribute: string) => {
      const definition = metacardDefinitions.metacardTypes[attribute]
      const attributeName =
        typeof definition !== 'undefined'
          ? definition.alias || definition.id
          : attribute
      const attributeValue = map
        .get('targetMetacard')
        .get('metacard')
        .get('properties')
        .get(attributeName)
      return { name: attributeName, value: attributeValue }
    })
    .filter(({ value }: Attribute) => typeof value !== 'undefined')
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

  render() {
    return <MapInfoPresentation {...this.state} />
  }
}

export default hot(module)(withListenTo(MapInfo))
