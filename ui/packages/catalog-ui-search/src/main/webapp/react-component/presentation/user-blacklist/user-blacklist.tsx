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

import { hot } from 'react-hot-loader'
import * as React from 'react'
import styled, { keyframes } from '../../styles/styled-components'
import { Button, buttonTypeEnum } from '../button'
import BlacklistItemContainer from '../../container/blacklist-item'

const expandAnimation = keyframes`
  from {
      transform: scale(0)
  }
  to {
      transform: scale(1)
  }
`

const collapseAnimation = keyframes`
  from {
    transform: translateY(0) scaleY(1);
  }
  to {
    transform: translateY(-50%) scaleY(0);
  }
`

const EmptyBlacklist = styled.div`
  white-space: normal;
  padding: ${props => props.theme.minimumSpacing};
  text-align: center;
  font-size: ${props => props.theme.largeFontSize};
  overflow: hidden;
  animation: ${expandAnimation} ${props => props.theme.coreTransitionTime}
    linear;
`

const ItemsWrapper = styled<Props, 'div'>('div')`
  overflow: hidden;
  ${props =>
    props.clearing
      ? `animation: ${collapseAnimation} 
      ${props.theme.coreTransitionTime} linear forwards;`
      : ''};
`

type Props = {
  onClearBlacklist: () => void
  blacklist: Backbone.Collection<Backbone.Model>
  clearing: boolean
}

const Blacklist = (props: Props) => {
  return (
    <React.Fragment>
      <Button
        icon="fa fa-eye"
        buttonType={buttonTypeEnum.neutral}
        onClick={props.onClearBlacklist}
        style={{ width: '100%' }}
        text="Unhide All"
      />
      <ItemsWrapper {...props}>
        {props.blacklist.map(item => {
          return <BlacklistItemContainer key={item.id} item={item} />
        })}
      </ItemsWrapper>
    </React.Fragment>
  )
}

const UserBlacklistPresentation = (props: Props) => {
  return (
    <React.Fragment>
      {props.blacklist.length !== 0 ? (
        <Blacklist {...props} />
      ) : (
        <EmptyBlacklist>Nothing hidden.</EmptyBlacklist>
      )}
    </React.Fragment>
  )
}

export default hot(module)(UserBlacklistPresentation)
