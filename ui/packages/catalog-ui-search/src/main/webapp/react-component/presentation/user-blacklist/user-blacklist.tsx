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

import { hot } from 'react-hot-loader'
import * as React from 'react'
import styled from '../../styles/styled-components'
import { keyframes } from '../../styles/styled-components'
import { Button, buttonTypeEnum } from '../button'
import BlacklistItemContainer from '../../container/blacklist-item'

const Root = styled<Props, 'div'>('div')`
  > button {
    width: 100%;
  }
`

const expandAnimation = keyframes`
  from {
      transform: scale(0)
  }
  to {
      transform: scale(1)
  }
`
const EmptyText = styled<Props, 'div'>('div')`
  white-space: normal;
  padding: ${props => props.theme.minimumSpacing};
  text-align: center;
  font-size: ${props => props.theme.largeFontSize};
  overflow: hidden;
  animation: ${expandAnimation} ${props => props.theme.coreTransitionTime}
    linear;
`

const collapseAnimation = keyframes`
from {
    transform: translateY(0) scaleY(1);
  }
  to {
    transform: translateY(-50%) scaleY(0);
  }
`

const AnimationWrapper = styled<Props, 'div'>('div')`
  ${props => {
    if (props.clearing) {
      return (
        'animation: ' +
        collapseAnimation +
        ' ' +
        props.theme.coreTransitionTime +
        ' linear;'
      )
    } else {
      return ''
    }
  }};
  overflow: hidden;
`

type Props = {
  clearBlacklist: () => void
  blacklist: Backbone.Collection<Backbone.Model>
  clearing: boolean
}

const UserBlacklistPresentation = (props: Props) => {
  return (
    <Root {...props}>
      {props.blacklist.length !== 0 ? (
        [
          <Button
            icon="fa fa-eye"
            buttonType={buttonTypeEnum.neutral}
            onClick={props.clearBlacklist}
            text="Unhide All"
          />,
          <AnimationWrapper {...props}>
            <div className="is-list has-list-highlighting">
              {props.blacklist.map(item => {
                return <BlacklistItemContainer key={item.id} item={item} />
              })}
            </div>
          </AnimationWrapper>,
        ]
      ) : (
        <EmptyText {...props}>Nothing hidden.</EmptyText>
      )}
    </Root>
  )
}

export default hot(module)(UserBlacklistPresentation)
