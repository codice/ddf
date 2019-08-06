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
import MenuItem from '../menu-item'
import { withDropdown, withContext, ContextType } from '../dropdown'
import { hot } from 'react-hot-loader'
import { Subtract } from '../../../typescript'

type Props = {
  className?: string
  style?: React.CSSProperties
  icon?: string
  help?: string
  onClick?: (e: any, context: ContextType) => void
  children?: any
}

const ModifiedMenuItem = styled<Props>(MenuItem)`
  display: flex;
  align-items: center;
  justify-content: flex-start;
  > .icon {
    text-align: center;
    width: ${props => props.theme.minimumButtonSize};
  }
`

const render = withDropdown(
  (
    props: Props &
      withContext &
      Subtract<React.HTMLAttributes<HTMLDivElement>, Props>
  ) => {
    const {
      children,
      onClick,
      className,
      style,
      icon,
      help,
      dropdownContext,
      ...otherAttr
    } = props
    return (
      <ModifiedMenuItem
        onClick={(e: any) => {
          onClick && onClick(e, dropdownContext)
        }}
        className={className}
        style={style}
        data-help={help}
        {...otherAttr}
      >
        <span className={`icon ${icon}`} />
        <div>{children}</div>
      </ModifiedMenuItem>
    )
  }
)

export default hot(module)(render)
