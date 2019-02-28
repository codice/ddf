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
import { keyframes } from '../../styles/styled-components'
import styled from '../../styles/styled-components'
import { buttonTypeEnum, Button } from '../button'

type Props = {
  remove: () => void
  navigate: () => void
  itemTitle: string
  clearing: boolean
}

const collapseAnimation = keyframes`
from {
    transform: translateY(0) scaleY(1);
  }
  to {
    transform: translateY(-50%) scaleY(0);
  }
`

const Root = styled<Props, 'div'>('div')`
  display: block;
  height: ${props =>
    props.theme.minimumButtonSize + props.theme.minimumSpacing};
  text-align: center;
  margin-bottom: ${props => props.theme.minimumSpacing};
  cursor: pointer;
  overflow: hidden;
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

  .button-remove {
    float: right;
  }
`

const ItemDetails = styled<Props, 'div'>('div')`
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
      <ItemDetails {...props}>{props.itemTitle}</ItemDetails>
      <Button
        className="button-remove"
        icon="fa fa-eye"
        buttonType={buttonTypeEnum.neutral}
        onClick={props.remove}
      />
    </Root>
  )
}
export default hot(module)(BlacklistItemPresentation)
