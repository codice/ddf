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
import * as React from 'react'
import styled from '../../styles/styled-components'
import { hot } from 'react-hot-loader'

const Root = styled.div`
  overflow: auto;

  .export-action {
    cursor: pointer;
    display: block;
    line-height: ${props => props.theme.minimumButtonSize};
    padding: 0px 15px;
    background-color: ${props => props.theme.backgroundDropdown};
  }

  .export-action:hover {
    background-color: ${props => props.theme.backgroundAccentContent};
  }
`

type Action = {
  title: string
  url: string
}

type Props = {
  actions: Action[]
}

function alphabetizeExportActions(actions: Action[]) {
  return actions.sort((action1: Action, action2: Action) => {
    if (action1.title > action2.title) return 1
    if (action1.title < action2.title) return -1
    return 0
  })
}

const render = (props: Props) => {
  const { actions } = props
  const sortedActions = alphabetizeExportActions(actions)

  return (
    <Root>
      {sortedActions.map(action => (
        <div
          key={action.title}
          className={'export-action'}
          onClick={() => {
            window.open(action.url)
          }}
        >
          {action.title}
        </div>
      ))}
    </Root>
  )
}

export default hot(module)(render)
export const testComponent = render
