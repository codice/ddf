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
import { Subtract } from '../../../typescript'

/**
 * This is a quick way to emulate button behavior in a div so you can do
 * things like buttons within buttons which typically aren't allowed
 */

const EnterKeyCode = 13
const SpaceKeyCode = 32

type Props = {
  children?: any
  ref?: React.RefObject<HTMLDivElement>
  onClick?: any
  className?: string
  style?: React.CSSProperties
}

const Root = styled.div`
  display: inline-block;
  cursor: pointer;
  min-height: ${props => props.theme.minimumButtonSize};
  min-width: ${props => props.theme.minimumButtonSize};
`
/**
 * This should be done on keydown, check buttons out in a browser if you're in doubt.
 * @param event
 */
const handleKeyDown = (event: any) => {
  let code = event.keyCode
  if (event.charCode && code == 0) code = event.charCode
  if (event.target === event.currentTarget && code === EnterKeyCode) {
    event.preventDefault()
    event.stopPropagation()
    event.target.click()
  }
}

/**
 * This should be done on keyup, check buttons out in a browser if you're in doubt.
 * @param event
 */
const handleKeyUp = (event: any) => {
  let code = event.keyCode
  if (event.charCode && code == 0) code = event.charCode
  if (event.target === event.currentTarget && code === SpaceKeyCode) {
    event.preventDefault()
    event.stopPropagation()
    event.target.click()
  }
}

const render: React.ComponentType<
  Props & Subtract<React.HTMLAttributes<HTMLDivElement>, Props>
> = React.forwardRef(
  // @ts-ignore
  (
    props: Props & Subtract<React.HTMLAttributes<HTMLDivElement>, Props>,
    ref?: React.Ref<HTMLDivElement>
  ) => {
    const { children, onClick, className, style, ...otherAttr } = props
    return (
      <Root
        innerRef={ref}
        onKeyDown={handleKeyDown}
        onKeyUp={handleKeyUp}
        tabIndex={0}
        className={className}
        onClick={onClick}
        style={style as any}
        {...otherAttr as any}
      >
        {children}
      </Root>
    )
  }
)

export default render // don't hot export this until react-hot-loader forwards refs correctly
