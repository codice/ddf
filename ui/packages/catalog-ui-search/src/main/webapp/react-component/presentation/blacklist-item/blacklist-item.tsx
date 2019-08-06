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
import { buttonTypeEnum, Button } from '../button'
import { transparentize, readableColor } from 'polished'

type Props = {
  onRemove: () => void
  onNavigate: () => void
  itemTitle: string
  clearing: boolean
}

const collapseAnimation = (initialHeight: string) => {
  return keyframes`
    from {
      height: ${initialHeight};
    }
    to {
      height: 0px;
    }
  `
}

const Root = styled<Props, 'div'>('div')`
  border-top: 1px solid;
  border-bottom: 1px solid;
  border-color: ${props =>
    transparentize(0.9, readableColor(props.theme.backgroundContent))};

  :hover {
    border-top: 1px solid;
    border-bottom: 1px solid;
    border-color: ${props =>
      transparentize(0.8, readableColor(props.theme.backgroundContent))};
  }
  display: block;
  text-align: center;
  margin-bottom: ${props => props.theme.minimumSpacing};
  cursor: pointer;
  height: ${props => props.theme.minimumButtonSize};
  overflow: hidden;
  ${props =>
    props.clearing
      ? `animation: ${collapseAnimation(props.theme.minimumButtonSize)} 
      ${props.theme.coreTransitionTime} linear forwards;`
      : ''};
`

const ItemDetails = styled.div`
  vertical-align: top;
  padding: 0px ${props => props.theme.minimumSpacing};
  text-align: center;
  width: calc(100% - 2 * ${props => props.theme.minimumButtonSize});
  height: ${props => props.theme.minimumButtonSize};
  line-height: ${props => props.theme.minimumButtonSize};
  text-overflow: ellipsis;
  white-space: nowrap;
  overflow: hidden;
  display: inline-block;
`

const BlacklistItemPresentation = (props: Props) => {
  return (
    <Root {...props}>
      <ItemDetails onClick={props.onNavigate}>{props.itemTitle}</ItemDetails>
      <Button
        style={{ float: 'right' }}
        icon="fa fa-eye"
        buttonType={buttonTypeEnum.neutral}
        onClick={props.onRemove}
      />
    </Root>
  )
}
export default hot(module)(BlacklistItemPresentation)
