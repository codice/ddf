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
import WorkspaceMenuPresentation from '../../presentation/workspace-menu'
import withListenTo, { WithBackboneProps } from '../backbone-container'
import { hot } from 'react-hot-loader'
const store = require('js/store')

type Props = WithBackboneProps
type State = {
  currentWorkspace?: Backbone.Model
  saved: boolean
}

const mapStoretoState = () => {
  return {
    saved: store
      .get('content')
      .get('currentWorkspace')
      .isSaved(),
    currentWorkspace: store.get('content').get('currentWorkspace'),
  }
}

class WorkspaceMenu extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = mapStoretoState()
    props.listenTo(
      store.get('content'),
      'change:currentWorkspace',
      this.updateCurrentWorkspace.bind(this)
    )
  }
  updateCurrentWorkspace() {
    this.setState(mapStoretoState())
  }
  render() {
    return this.state.currentWorkspace === undefined ? null : (
      <WorkspaceMenuPresentation
        currentWorkspace={this.state.currentWorkspace}
        saved={this.state.saved}
      />
    )
  }
}

export default hot(module)(withListenTo(WorkspaceMenu))
