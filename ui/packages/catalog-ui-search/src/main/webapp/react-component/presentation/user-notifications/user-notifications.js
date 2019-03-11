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
import React from 'react'
import styled from '../../styles/styled-components'
import withListenTo from '../../container/backbone-container'
import MarionetteRegionContainer from '../../container/marionette-region-container'
const Backbone = require('backbone')
const CustomElements = require('../../../js/CustomElements.js')
const NotificationGroupView = require('../../../component/notification-group/notification-group.view.js')
const user = require('../../../component/singletons/user-instance.js')
const moment = require('moment')
const userNotifications = require('../../../component/singletons/user-notifications.js')

const Empty = styled.div`
  transition: transform ${props => props.theme.coreTransitionTime} linear;
  transform: scale(1);
  text-align: center;
  font-size: ${props => props.theme.largeFontSize}
  padding: ${props => props.theme.mediumSpacing}
`
const Notifications = styled.div`
  height: 100%;
  width: 100%;
  display: block;
  overflow: auto;
  padding: ${props => props.theme.mediumSpacing};
`

class UserNotifications extends React.Component {
  constructor(props) {
    super(props)
    this.state = userNotifications
    this.props.listenTo(userNotifications, 'add remove update', () =>
      this.setState(userNotifications)
    )
  }
  render() {
    let notificationsList = []
    for (let i = 0; i <= 8; i++) {
      notificationsList.push(
        <MarionetteRegionContainer
          key={i.toString()}
          view={this.listPreviousDays(i)}
          viewOptions={{ replaceElement: true }}
        />
      )
    }
    return userNotifications.isEmpty() ? (
      <Empty>
        <div>No Notifications</div>
      </Empty>
    ) : (
      <Notifications>{notificationsList}</Notifications>
    )
  }
  listPreviousDays(numDays) {
    if (numDays < 7) {
      return new NotificationGroupView({
        filter: model => {
          return moment().diff(model.get('sentAt'), 'days') === numDays
        },
        date: this.informalName(numDays),
      })
    } else {
      return new NotificationGroupView({
        filter: model => {
          return moment().diff(model.get('sentAt'), 'days') >= 7
        },
        date: 'Older',
      })
    }
  }
  informalName(daysAgo) {
    switch (daysAgo) {
      case 0:
        return 'Today'
        break
      case 1:
        return 'Yesterday'
        break
      default:
        return moment()
          .subtract(daysAgo, 'days')
          .format('dddd')
        break
    }
  }
  componentWillUnmount() {
    userNotifications.setSeen()
    user
      .get('user')
      .get('preferences')
      .savePreferences()
  }
}
export default withListenTo(UserNotifications)
