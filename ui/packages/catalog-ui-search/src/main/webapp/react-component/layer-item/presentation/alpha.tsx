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

const Alpha = styled.input.attrs({
  min: '0',
  max: '1',
  step: '0.01',
  type: 'range',
})`
  display: inline-block;
  vertical-align: middle !important;
`

const AlphaDisabled = styled(Alpha)`
  ${props => DisabledBehavior(props.theme)};
`

const AlphaEnabled = styled(Alpha)`
  opacity: 1;
  cursor: default !important;
`

const render = (props: PresentationProps) => {
  const { show, alpha } = props.visibility
  const { updateLayerAlpha } = props.actions
  return show ? (
    <AlphaEnabled onChange={updateLayerAlpha} value={alpha} />
  ) : (
    <AlphaDisabled onChange={updateLayerAlpha} value={alpha} disabled />
  )
}

export const LayerAlpha = hot(module)(render)
