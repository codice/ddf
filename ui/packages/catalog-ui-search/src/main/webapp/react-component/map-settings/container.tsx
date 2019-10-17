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
import MapSettingsPresentation from './presentation'
import Dropdown from '../presentation/dropdown'
import { hot } from 'react-hot-loader'
import withListenTo, {
  WithBackboneProps,
} from '../../react-component/backbone-container'
const user = require('../../component/singletons/user-instance.js')

const save = (newFormat: string) => {
  const preferences = user.get('user').get('preferences')
  preferences.set({
    coordinateFormat: newFormat,
  })
  preferences.savePreferences()
}

const Span = styled.span`
  padding-right: 5px;
`
type State = {
  selected: string
}

class MapSettings extends React.Component<WithBackboneProps, State> {
  constructor(props: WithBackboneProps) {
    super(props)
    this.state = {
      selected: user
        .get('user')
        .get('preferences')
        .get('coordinateFormat'),
    }
  }

  componentDidMount = () =>
    this.props.listenTo(
      user.get('user').get('preferences'),
      'change:coordinateFormat',
      (_prefs: any, value: string) => this.setState({ selected: value })
    )

  update(newFormat: string) {
    save(newFormat)
    this.setState({ selected: newFormat })
  }

  render() {
    const { selected } = this.state

    const mapSettingsProps = {
      selected,
      update: (newFormat: string) => this.update(newFormat),
    }

    const mapSettings = <MapSettingsPresentation {...mapSettingsProps} />

    return (
      <Dropdown content={mapSettings}>
        <Span className="interaction-text">Settings</Span>
        <Span className="interaction-icon fa fa-cog" />
      </Dropdown>
    )
  }
}

export default hot(module)(withListenTo(MapSettings))
export const testComponent = withListenTo(MapSettings)
