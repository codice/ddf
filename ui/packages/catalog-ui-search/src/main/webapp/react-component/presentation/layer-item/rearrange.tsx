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
import * as React from 'react'
import styled from '../../styles/styled-components'
import { hot } from 'react-hot-loader'
import { Props, IsButton, HighlightBehavior, GrabCursor } from '.'
import { Order } from '../../container/layer-item'
/* stylelint-disable block-no-empty */
const Rearrange = styled.div``

const RearrangeButton = styled.button`
  ${props => IsButton(props.theme)};
  ${HighlightBehavior({ initialOpacity: 0 })};
  z-index: 1;
  position: absolute;
  height: calc(0.5 * ${props => props.theme.minimumButtonSize});
  line-height: calc(0.5 * ${props => props.theme.minimumButtonSize});
`
const Down = styled(RearrangeButton)`
  top: calc(100% - 0.5 * ${props => props.theme.minimumButtonSize});
`
const Up = styled(RearrangeButton)`
  top: 0px;
`
const RearrangeIcon = styled.span`
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translateX(-50%) translateY(-50%);
`

const Drag = styled.button`
  ${props => IsButton(props.theme)};
  ${props => HighlightBehavior({ initialOpacity: props.theme.minimumOpacity })};
  ${GrabCursor};
  vertical-align: middle;
  position: absolute;
  top: 0px;
  height: 100%;
`
const RearrangeUp = ({ order, handleClick }: { order:Order, handleClick: (e:any)=>void }) => {
    const {isTop} = order
    if (isTop) {
    return null
  }
  return (
    <Up onClick={handleClick}>
      <RearrangeIcon className="fa fa-angle-up" />
    </Up>
  )
}

const RearrangeDown = ({ order, handleClick }: { order: Order, handleClick: (e:any)=>void }) => {
    const {isBottom} = order
  if (isBottom) {
    return null
  }
  return (
    <Down onClick={handleClick}>
      <RearrangeIcon className="fa fa-angle-down" />
    </Down>
  )
}

const render = (props: Props) => {
  const { order } = props
  const {moveDown, moveUp} = props.actions
  return (
    <Rearrange>
      {RearrangeUp({order, handleClick:moveUp})}
      {RearrangeDown({order, handleClick:moveDown})}
      <Drag className = "layer-rearrange">
        <span className="fa fa-arrows-v" />
      </Drag>
    </Rearrange>
  )
}

export const LayerRearrange = hot(module)(render)
