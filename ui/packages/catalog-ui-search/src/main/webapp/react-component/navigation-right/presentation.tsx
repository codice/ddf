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
import styled, { css, keyframes } from 'styled-components'
import { transparentize } from 'polished'
import { CustomElement } from '../styles/mixins'
import ExtensionPoints from '../../extension-points'

export interface Props {
  username: string
  hasUnseenNotifications: boolean
  isGuest: boolean
}

const navigationRightUserIcon = '1rem'
const unseenNotifications = keyframes`
  0% {
    opacity: ${props => props.theme.minimumOpacity};
  }
  100% {
    opacity: 1;
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
    border-left: solid 1px ${transparentize(0.8, '#ffffff')};
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
    ${props => (props.isGuest ? 'display:none;' : '')};
  }

  .user-unique span:first-of-type {
    display: inline-block;
    text-overflow: ellipsis;
    vertical-align: top;
    width: ${navigationRightUserIcon};
    padding: 0 ${navigationRightUserIcon};
  }

  .user-unique span:nth-of-type(2) {
    display: inline-block;
    overflow: hidden;
    max-width: calc(9rem - ${navigationRightUserIcon});
    padding: 0 ${navigationRightUserIcon} 0 0;
  }

  .user-guest {
    ${props => (!props.isGuest ? 'display:none;' : '')};
  }

  ${props => {
    if (props.hasUnseenNotifications) {
      return css`
        .item-alerts {
          opacity: 1;
          animation: ${unseenNotifications}
            ${props.theme.multiple(4, props.theme.coreTransitionTime, 's')} 5
            alternate ease-in-out;
        }

        .alerts-badge {
          transform: scale(1) translateY(-50%);
        }
      `
    }
  }};
`

export default function NavigationRight(props: Props) {
  return (
    <Root {...props}>
      {ExtensionPoints.navigationRight.map((Component: any, i: number) => (
        <Component key={i} />
      ))}
    </Root>
  )
}
