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

import { hot } from 'react-hot-loader'
import * as React from 'react'
import styled from '../../styles/styled-components'
import MapActions from '../../container/map-actions'

type Props = {
  model: Backbone.Model
  exportActions: any
  otherActions: any
}

const Root = styled.div`
  overflow: auto;
  height: 100%;
  padding: 0px ${props => props.theme.largeSpacing};
`
const Header = styled.div`
  text-align: left;
  font-size: ${props => props.theme.largeFontSize};
  font-weight: bolder;
  opacity: 0.8;
`

const MapActionsDiv = styled.div`
  margin-top: ${props => props.theme.minimumSpacing};
`
const Divider = styled.div`
  height: ${props => props.theme.dividerHeight};
  margin: ${props => props.theme.minimumSpacing} 0px;
` //TODO add fade

const Actions = styled.div`
  padding: 0px ${props => props.theme.largeSpacing};
`

const ActionLink = styled.a`
  margin-top: ${props => props.theme.minimumSpacing};
  cursor: pointer;
`

const ExportActions = (exportActions: any) => {
  return (
    <>
      <Header>Export as:</Header>
      <Divider />
      <Actions>
        {exportActions.map((exportAction: any) => {
          return (
            <ActionLink
              href={exportAction.url}
              target="_blank"
              key={exportAction.url}
            >
              {exportAction.title}
            </ActionLink>
          )
        })}
      </Actions>
    </>
  )
}

const OtherActions = (otherActions: any) => {
  if (otherActions.length === 0) {
    return null
  }
  return (
    <>
      <Header>Various:</Header>
      <Divider />
      <Actions>
        {otherActions.map((otherAction: any) => {
          return (
            <ActionLink
              href={otherAction.url}
              target="_blank"
              key={otherAction.url}
            >
              {otherAction.title}
            </ActionLink>
          )
        })}
      </Actions>
      <Divider />
    </>
  )
}

const render = (props: Props) => {
  const { exportActions, otherActions, model } = props
  return (
    <Root>
      <Divider />
      <ExportActions exportActions={exportActions} />
      <Divider />
      <MapActionsDiv>
        <MapActions model={model} />
      </MapActionsDiv>
      <OtherActions otherActions={otherActions} />
    </Root>
  )
}

export default hot(module)(render)
