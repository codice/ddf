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
import { Button, buttonTypeEnum } from '../presentation/button'
import UserNotifications from '../presentation/user-notifications/user-notifications'
import UserView from '../user'

const HelpView = require('../../component/help/help.view.js')
const UserSettings = require('../../component/user-settings/user-settings.view.js')
const SlideoutViewInstance = require('../../component/singletons/slideout.view-instance.js')
const SlideoutRightViewInstance = require('../../component/singletons/slideout.right.view-instance.js')
const user = require('../../component/singletons/user-instance.js')
export interface Props {
  username: string
  hasUnseenNotifications: boolean
  isGuest: boolean
}

const toggleAlerts = () => {
  SlideoutRightViewInstance.updateContent(UserNotifications)
  SlideoutRightViewInstance.open()
}

const toggleHelp = () => {
  HelpView.toggleHints()
}

const toggleUserSettings = () => {
  SlideoutViewInstance.updateContent(UserSettings)
  SlideoutViewInstance.open()
}

const toggleUser = () => {
  SlideoutRightViewInstance.updateContent(UserView)
  SlideoutRightViewInstance.open()
}

export const Help = () => (
  <Button
    className="navigation-item"
    icon="fa fa-question"
    buttonType={buttonTypeEnum.neutral}
    title="Shows helpful hints in the current context"
    onClick={toggleHelp}
    fadeUntilHover={true}
  />
)

export const Settings = () => (
  <Button
    className="navigation-item"
    icon="fa fa-cog"
    buttonType={buttonTypeEnum.neutral}
    title="Shows settings for the application"
    onClick={toggleUserSettings}
    fadeUntilHover={true}
  />
)

export const Notifications = () => (
  <Button
    className="navigation-item item-alerts"
    buttonType={buttonTypeEnum.neutral}
    title="Shows notifications"
    onClick={toggleAlerts}
    fadeUntilHover={true}
  >
    <span className="fa fa-bell" />
    <span className="alerts-badge fa fa-exclamation" />
  </Button>
)

export const User = () => (
  <Button
    className="navigation-item item-user"
    buttonType={buttonTypeEnum.neutral}
    onClick={toggleUser}
    fadeUntilHover={true}
  >
    <div className="user-unique" title={`Logged in as ${user.getUserName()}`}>
      <span className="fa fa-user" />
      <span className="">{user.isGuest() ? 'Guest' : user.getUserName()}</span>
    </div>
  </Button>
)
