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
var Backbone = require('backbone')
var CustomElements = require('../../../js/CustomElements.js')
var NotificationGroupView = require('../../../component/notification-group/notification-group.view.js')
var user = require('../../../component/singletons/user-instance.js')
var moment = require('moment')
var userNotifications = require('../../../component/singletons/user-notifications.js')

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
    this.props.listenTo(
      userNotifications,
      'add remove update',
      this.updateState.bind(this)
    )
  }
  render() {
    return userNotifications.isEmpty() ? (
      <Empty>
        <div className="notifications-empty">No Notifications</div>
      </Empty>
    ) : (
      <Notifications>
        <MarionetteRegionContainer
          classname="list-today"
          view={this.listToday()}
          viewOptions={this.replaceElement()}
        />
        <MarionetteRegionContainer
          classname="list-one"
          view={this.listPreviousOne()}
          viewOptions={this.replaceElement()}
        />
        <MarionetteRegionContainer
          classname="list-two"
          view={this.listPreviousTwo()}
          viewOptions={this.replaceElement()}
        />
        <MarionetteRegionContainer
          classname="list-three"
          view={this.listPreviousThree()}
          viewOptions={this.replaceElement()}
        />
        <MarionetteRegionContainer
          classname="list-four"
          view={this.listPreviousFour()}
          viewOptions={this.replaceElement()}
        />
        <MarionetteRegionContainer
          classname="list-five"
          view={this.listPreviousFive()}
          viewOptions={this.replaceElement()}
        />
        <MarionetteRegionContainer
          classname="list-six"
          view={this.listPreviousSix()}
          viewOptions={this.replaceElement()}
        />
        <MarionetteRegionContainer
          classname="list-older"
          view={this.listOlder()}
          viewOptions={this.replaceElement()}
        />
      </Notifications>
    )
  }
  updateState() {
    this.setState(userNotifications)
  }
  listToday() {
    return new NotificationGroupView({
      filter: function(model) {
        return moment().diff(model.get('sentAt'), 'days') === 0
      },
      date: 'Today',
    })
  }
  listPreviousOne() {
    return new NotificationGroupView({
      filter: function(model) {
        return moment().diff(model.get('sentAt'), 'days') === 1
      },
      date: 'Yesterday',
    })
  }
  listPreviousTwo() {
    return new NotificationGroupView({
      filter: function(model) {
        return moment().diff(model.get('sentAt'), 'days') === 2
      },
      date: moment()
        .subtract(2, 'days')
        .format('dddd'),
    })
  }
  listPreviousThree() {
    return new NotificationGroupView({
      filter: function(model) {
        return moment().diff(model.get('sentAt'), 'days') === 3
      },
      date: moment()
        .subtract(3, 'days')
        .format('dddd'),
    })
  }
  listPreviousFour() {
    return new NotificationGroupView({
      filter: function(model) {
        return moment().diff(model.get('sentAt'), 'days') === 4
      },
      date: moment()
        .subtract(4, 'days')
        .format('dddd'),
    })
  }
  listPreviousFive() {
    return new NotificationGroupView({
      filter: function(model) {
        return moment().diff(model.get('sentAt'), 'days') === 5
      },
      date: moment()
        .subtract(5, 'days')
        .format('dddd'),
    })
  }
  listPreviousSix() {
    return new NotificationGroupView({
      filter: function(model) {
        return moment().diff(model.get('sentAt'), 'days') === 6
      },
      date: moment()
        .subtract(6, 'days')
        .format('dddd'),
    })
  }
  listOlder() {
    return new NotificationGroupView({
      filter: function(model) {
        return moment().diff(model.get('sentAt'), 'days') >= 7
      },
      date: 'Older',
    })
  }
  replaceElement() {
    return { replaceElement: true }
  }
  onDestroy() {
    userNotifications.setSeen()
    user
      .get('user')
      .get('preferences')
      .savePreferences()
  }
}
export default withListenTo(UserNotifications)
