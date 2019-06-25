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
import NavigationRightComponent from '../../presentation/navigation-right'
const user = require('../../../component/singletons/user-instance.js')
const notifications = require('../../../component/singletons/user-notifications.js')
import withListenTo, { WithBackboneProps } from '../backbone-container'

type Props = {} & WithBackboneProps

interface State {
  username: string
  isGuest: boolean
  hasUnseenNotifications: boolean
}

class NavigationRightContainer extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = {
      isGuest: user.isGuest(),
      username: user.getUserName(),
      hasUnseenNotifications: notifications.hasUnseen(),
    }
  }
  componentDidMount() {
    this.props.listenTo(
      notifications,
      'change add remove reset update',
      this.handleUnseenNotifications.bind(this)
    )
  }
  handleUnseenNotifications() {
    this.setState({
      hasUnseenNotifications: notifications.hasUnseen(),
    })
  }
  render() {
    return (
      <NavigationRightComponent
        username={this.state.username}
        hasUnseenNotifications={this.state.hasUnseenNotifications}
        isGuest={this.state.isGuest}
      />
    )
  }
}

export default withListenTo(NavigationRightContainer)
