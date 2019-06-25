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
import { PresentationProps } from '.'

import { LayerRearrange, LayerAlpha, LayerInteractions, LayerName } from '.'

const Root = styled<PresentationProps, 'div'>('div')`
  display: block;
  white-space: nowrap;
  width: 100%;
  overflow: hidden;
  position: relative;
  border: 2px solid rgba(255, 255, 255, 0.1);
  border-top: ${props => {
    if (!props.order.isTop) {
      return 'none'
    }
  }};
`

const LayerPropertiesRoot = styled.div`
  display: inline-block;
  vertical-align: middle;
  padding: 0 ${props => props.theme.mediumSpacing};
  margin-left: ${props => props.theme.minimumButtonSize};
  width: calc(100% - ${props => props.theme.minimumButtonSize});
  border-left: 2px solid rgba(255, 255, 255, 0.1);
`

const render = (props: PresentationProps) => {
  return (
    <Root {...props}>
      <LayerRearrange {...props} />
      <LayerPropertiesRoot>
        <LayerName {...props} />
        <LayerAlpha {...props} />
        <LayerInteractions {...props} />
      </LayerPropertiesRoot>
    </Root>
  )
}

export default hot(module)(render)
