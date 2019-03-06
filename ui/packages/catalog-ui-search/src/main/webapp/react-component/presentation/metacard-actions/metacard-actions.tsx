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

  .is-header {
    text-align: left;
  }
  .actions {
    padding: 0px ${props => props.theme.largeSpacing};
  }
  .map-actions {
    margin-top: ${props => props.theme.minimumSpacing};
  }
  a {
    display: block;
    margin-top: ${props => props.theme.minimumSpacing};
  }
`

const render = (props: Props) => {
  const { exportActions, otherActions, model } = props
  return (
    <Root>
      <div className="is-divider" />
      <div className="is-header">Export as:</div>
      <div className="is-divider" />
      <div className="actions">
        {exportActions.map((exportAction: any) => {
          return (
            <a href={exportAction.url} target="_blank" key={exportAction.url}>
              {exportAction.title}
            </a>
          )
        })}
      </div>
      <div className="is-divider" />
      <div className="map-actions">
        <MapActions model={model} />
      </div>

      {otherActions.length !== 0 && (
        <>
          <div className="is-header">Various:</div>
          <div className="is-divider" />
          <div className="actions">
            {otherActions.map((otherAction: any) => {
              return (
                <a href={otherAction.url} target="_blank" key={otherAction.url}>
                  {otherAction.title}
                </a>
              )
            })}
          </div>
          <div className="is-divider" />
        </>
      )}
    </Root>
  )
}

export default hot(module)(render)
