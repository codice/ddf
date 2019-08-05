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
import styled from 'styled-components'

interface Props {
  shown: boolean
  className?: string
  style?: React.CSSProperties
}

const Root = styled<Props, 'span'>('span')`
  display: inline-block;
  line-height: inherit;
  vertical-align: top;
  color: ${props => {
    return props.theme.warningColor
  }};

  transition: ${props => {
    return `transform ${props.theme.coreTransitionTime} ease-out, opacity ${
      props.theme.coreTransitionTime
    } ease-out;`
  }};

  transform: ${props => {
    return `scale(${props.shown ? 1 : 2});`
  }};

  opacity: ${props => {
    return props.shown ? 1 : 0
  }};
`

export default function UnsavedIndicator(props: Props) {
  const { className, style, ...otherProps } = props
  return (
    <Root className={className} style={style as any} {...otherProps}>
      *
    </Root>
  )
}
