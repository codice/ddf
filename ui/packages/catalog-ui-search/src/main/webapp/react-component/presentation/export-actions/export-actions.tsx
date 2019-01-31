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
import { hot } from 'react-hot-loader'
import styled from '../../styles/styled-components'

const Root = styled<{}, 'div'>('div')`
  overflow: auto;

  div {
    cursor: pointer;
    display: block;
    line-height: ${props => props.theme.minimumButtonSize};
    padding: 0px 15px;
    background-color: #253540;
  }

  div:hover {
    background-color: #34434c;
  }
`

type Action = {
  title: string
  url: string
}

type Props = {
  actions: Action[]
}

class ExportActions extends React.Component<Props, {}> {
  constructor(props: Props) {
    super(props)
  }
  alphabetizeExportActions(actions: Action[]) {
    return actions.sort((action1: Action, action2: Action) => {
      if (action1.title > action2.title) return 1
      if (action1.title < action2.title) return -1
      return 0
    })
  }
  render() {
    const sortedActions = this.alphabetizeExportActions(this.props.actions)

    return (
      <Root>
        {sortedActions.map(action => (
          <div
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
}

export default hot(module)(ExportActions)
