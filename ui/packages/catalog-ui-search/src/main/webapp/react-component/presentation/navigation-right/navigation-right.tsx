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
import styled from '../../styles/styled-components'
import { keyframes } from '../../styles/styled-components'
import { CustomElement } from '../../styles/mixins'
import { Button, buttonTypeEnum } from '../button'
import plugin from 'plugins/navigation-right'

const HelpView = require('../../../component/help/help.view.js')
const UserSettings = require('../../../component/user-settings/user-settings.view.js')
import UserNotifications from '../user-notifications/user-notifications'
const SlideoutViewInstance = require('../../../component/singletons/slideout.view-instance.js')
const SlideoutRightViewInstance = require('../../../component/singletons/slideout.right.view-instance.js')
const user = require('../../../component/singletons/user-instance.js')
import UserView from '../../../react-component/container/user'
export interface Props {
  username: string
  hasUnseenNotifications: boolean
  isGuest: boolean
}

const navigationRightUserIcon = '1.375rem'
const unseenNotifications = keyframes`
    0% {
        opacity: ${props => props.theme.minimumOpacity};
        transform: scale(1);
    }
    100% {
        opacity: 1;
        transform: scale(1.2);
    }
`

const Root = styled<Props, 'div'>('div')`
  ${CustomElement} white-space: nowrap;
  overflow: hidden;

  .navigation-item {
    display: inline-block;
    width: ${props => props.theme.minimumButtonSize};
    height: 100%;
    text-align: center;
    vertical-align: top;
    line-height: inherit;
  }

  .navigation-item.item-user {
    padding: 0px ${props => props.theme.minimumSpacing};
    max-width: 9rem + ${navigationRightUserIcon};
    width: auto;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .alerts-badge {
    position: absolute;
    font-size: ${props => props.theme.minimumFontSize};
    top: 35%;
    transform: scale(0) translateY(-50%);
    transition: transform ${props => props.theme.coreTransitionTime} ease-in-out;
    color: ${props => props.theme.warningColor};
  }

  .item-alerts {
    transition: transform 4 * ${props => props.theme.coreTransitionTime}
      ease-in-out;
    transform: scale(1);
  }

  .user-unique {
    white-space: nowrap;
    vertical-align: top;
    position: relative;
  }

  .user-unique span:first-of-type {
    display: inline-block;
    overflow: hidden;
    text-overflow: ellipsis;
    vertical-align: top;
    max-width: calc(9rem - ${navigationRightUserIcon});
    padding-right: ${navigationRightUserIcon};
  }

  .user-unique span:nth-of-type(2) {
    position: absolute;
    right: 0px;
    top: 50%;
    display: inline-block;
    width: ${navigationRightUserIcon};
    line-height: inherit !important;
    vertical-align: top;
    transform: translateY(-50%);
  }

  .user-unique {
    ${props => (props.isGuest ? 'display:none;' : '')};
  }

  .user-guest {
    ${props => (!props.isGuest ? 'display:none;' : '')};
  }

  ${props => {
    if (props.hasUnseenNotifications) {
      return `
                .item-alerts {
                    opacity: 1;
                    animation: ${unseenNotifications} ${props.theme.multiple(
        4,
        props.theme.coreTransitionTime,
        's'
      )} 5 alternate ease-in-out;
                    transform: scale(1.2);
                }

                .alerts-badge {
                    transform: scale(1) translateY(-50%);
                }
            `
    }
  }};
`

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

const Help = () => (
  <Button
    className="navigation-item"
    icon="fa fa-question"
    buttonType={buttonTypeEnum.neutral}
    title="Shows helpful hints in the current context"
    onClick={toggleHelp}
    fadeUntilHover={true}
  />
)

const Settings = () => (
  <Button
    className="navigation-item"
    icon="fa fa-cog"
    buttonType={buttonTypeEnum.neutral}
    title="Shows settings for the application"
    onClick={toggleUserSettings}
    fadeUntilHover={true}
  />
)

const Notifications = () => (
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

const User = () => (
  <Button
    className="navigation-item item-user"
    buttonType={buttonTypeEnum.neutral}
    onClick={toggleUser}
    fadeUntilHover={true}
  >
    <div className="user-unique" title={`Logged in as ${user.getUserName()}`}>
      <span className="">{user.getUserName()}</span>
      <span className="fa fa-user" />
    </div>
    <div className="user-guest" title="Logged in as guest.">
      <span className="">Sign In</span>
    </div>
  </Button>
)

const buttons = plugin([Help, Settings, Notifications, User])

export default function NavigationRight(props: Props) {
  return (
    <Root {...props}>
      {buttons.map((Component: any, i: number) => (
        <Component key={i} />
      ))}
    </Root>
  )
}
