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
import WorkspaceTitlePresentation from '../../presentation/workspace-title'
import { hot } from 'react-hot-loader'

type Props = {
  title: string
  saved: boolean
  onChange: (value: string) => void
}
type State = {}

class WorkspaceTitle extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
  }
  render() {
    return (
      <WorkspaceTitlePresentation
        title={this.props.title}
        onChange={this.props.onChange}
        saved={this.props.saved}
      />
    )
  }
}

export default hot(module)(WorkspaceTitle)
