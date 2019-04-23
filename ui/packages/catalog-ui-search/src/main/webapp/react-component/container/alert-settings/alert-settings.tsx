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
const user = require('../../../component/singletons/user-instance.js')
import AlertSettingsComponent from '../../presentation/alert-settings'
import withListenTo, { WithBackboneProps } from '../backbone-container'
import { hot } from 'react-hot-loader'

type Props = {} & WithBackboneProps
type State = {
  persistence: boolean
  expiration: number
}

type Value = {
  [key: string]: unknown
}

const save = (value: Value) => {
  const preferences = user.get('user').get('preferences')
  preferences.set(value)
  preferences.savePreferences()
}

const onExpirationChange = (value: Value) => {
  save({
    alertExpiration: value,
  })
}

const onPersistenceChange = (value: Value) => {
  save({
    alertPersistence: value,
  })
}

const mapBackboneToState = () => {
  return {
    persistence: user
      .get('user')
      .get('preferences')
      .get('alertPersistence'),
    expiration: user
      .get('user')
      .get('preferences')
      .get('alertExpiration'),
  }
}

class AlertSettings extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = mapBackboneToState()
    this.props.listenTo(
      user.get('user').get('preferences'),
      'change',
      this.updateState
    )
  }
  updateState = () => {
    this.setState(mapBackboneToState())
  }
  render() {
    const { persistence, expiration } = this.state
    return (
      <AlertSettingsComponent
        persistence={persistence}
        expiration={expiration}
        onPersistenceChange={onPersistenceChange}
        onExpirationChange={onExpirationChange}
      />
    )
  }
}

export default hot(module)(withListenTo(AlertSettings))
