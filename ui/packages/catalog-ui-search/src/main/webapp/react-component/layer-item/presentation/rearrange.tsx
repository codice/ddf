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
import { hot } from 'react-hot-loader'
import { PresentationProps, IsButton, HighlightBehavior, GrabCursor } from '.'

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
const RearrangeUp = (
  props: PresentationProps,
  forwardedRef: React.RefObject<HTMLButtonElement>
) => {
  const { isTop } = props.order
  const { moveUp: handleClick } = props.actions

  return (
    !isTop && (
      <Up innerRef={forwardedRef} onClick={handleClick}>
        <RearrangeIcon className="fa fa-angle-up" />
      </Up>
    )
  )
}

const RearrangeDown = (
  props: PresentationProps,
  forwardedRef: React.RefObject<HTMLButtonElement>
) => {
  const { isBottom } = props.order
  const { moveDown: handleClick } = props.actions

  return (
    !isBottom && (
      <Down innerRef={forwardedRef} onClick={handleClick}>
        <RearrangeIcon className="fa fa-angle-down" />
      </Down>
    )
  )
}
class LayerRearrange extends React.Component<PresentationProps, {}> {
  private down: React.RefObject<HTMLButtonElement>
  private up: React.RefObject<HTMLButtonElement>
  constructor(props: PresentationProps) {
    super(props)
    this.down = React.createRef()
    this.up = React.createRef()
  }

  componentDidMount() {
    const { isTop, isBottom } = this.props.order
    const { id } = this.props.layerInfo
    const { focusModel } = this.props.options

    if (focusModel.id === id) {
      let focusRef = focusModel.isUp() ? this.up : this.down
      focusRef = isTop ? this.down : focusRef
      focusRef = isBottom ? this.up : focusRef
      setTimeout(() => focusRef.current!.focus(), 0)
    }
  }

  render() {
    return (
      <Rearrange>
        {RearrangeUp(this.props, this.up)}
        {RearrangeDown(this.props, this.down)}
        <Drag className="layer-rearrange">
          <span className="fa fa-arrows-v" />
        </Drag>
      </Rearrange>
    )
  }
}

export default hot(module)(LayerRearrange)
