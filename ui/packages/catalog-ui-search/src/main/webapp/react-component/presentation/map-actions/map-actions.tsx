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

import { hot } from 'react-hot-loader'
import * as React from 'react'
import styled from '../../styles/styled-components'
import { readableColor } from 'polished'

type Props = {
  overlayActions: any
  currentOverlayUrl: string
  overlayImage: (event: any) => void
  hasMapActions: any
}

type OverlayAction = {
  url: string
  description: string
  overlayText: string
}

const Header = styled.div`
  text-align: left;
  font-size: ${props => props.theme.largeFontSize};
  font-weight: bolder;
  opacity: 0.8;
`

const Divider = styled.div`
  height: ${props => props.theme.borderRadius};
  margin: ${props => props.theme.minimumSpacing} 0px;
  background: ${props => readableColor(props.theme.backgroundContent)};
  opacity: 0.1;
`

const Actions = styled.div`
  padding: 0px ${props => props.theme.largeSpacing};
`

const OverlayActionLink = styled.a`
  margin-top: ${props => props.theme.minimumSpacing};
  cursor: pointer;
  display: block;
`

const render = (props: Props) => {
  const {
    hasMapActions,
    overlayActions,
    overlayImage,
    currentOverlayUrl,
  } = props

  if (!hasMapActions) {
    return null
  }
  return (
    <>
      <Header>Map:</Header>
      <Divider />
      <Actions>
        {overlayActions.map((overlayAction: OverlayAction) => {
          return (
            <OverlayActionLink
              data-url={overlayAction.url}
              title={overlayAction.description}
              onClick={overlayImage}
              key={overlayAction.url}
            >
              {overlayAction.overlayText}
              {overlayAction.url === currentOverlayUrl ? ' (remove)' : ''}
            </OverlayActionLink>
          )
        })}
      </Actions>
      <Divider />
    </>
  )
}

export default hot(module)(render)
