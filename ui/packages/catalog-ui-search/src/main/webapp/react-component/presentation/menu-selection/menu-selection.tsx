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
import { hot } from 'react-hot-loader'

type Props = {
  className?: string
  style?: React.CSSProperties
  onClick?: any
  isSelected?: boolean
  children?: any
}

const ModifiedMenuItem = styled<Props>(MenuItem)`
  display: flex;
  align-items: center;
  justify-content: flex-start;
  font-weight: ${props => (props.isSelected ? 'bolder' : 'inherit')};
  > .selection-indicator {
    opacity: ${props => (props.isSelected ? 1 : 0)};
    text-align: center;
    width: ${props => props.theme.minimumButtonSize};
  }
`

const render = (props: Props) => {
  const { isSelected, children, onClick, className, style } = props
  return (
    <ModifiedMenuItem
      onClick={onClick}
      className={`${className} ${isSelected ? 'is-active' : ''}`}
      style={style}
      isSelected={isSelected}
    >
      <span className="fa fa-check selection-indicator" />
      <div>{children}</div>
    </ModifiedMenuItem>
  )
}

export default hot(module)(render)
