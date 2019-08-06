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
import { PresentationProps, DisabledBehavior } from '.'

const Name = styled.div`
  line-height: ${props => props.theme.minimumButtonSize};
  overflow: hidden;
  text-overflow: ellipsis;
`
const NameDisabled = styled(Name)`
  ${props => DisabledBehavior(props.theme)};
  cursor: text !important;
`

const NameEnabled = styled(Name)`
  opacity: 1;
`

const render = (props: PresentationProps) => {
  const { name = 'Untitled' } = props.layerInfo
  const { show } = props.visibility
  return show ? (
    <NameEnabled title={name}>{name}</NameEnabled>
  ) : (
    <NameDisabled title={name}>{name}</NameDisabled>
  )
}

export const LayerName = hot(module)(render)
