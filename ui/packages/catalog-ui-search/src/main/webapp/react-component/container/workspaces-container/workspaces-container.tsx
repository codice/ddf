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
import Workspaces from '../../presentation/workspaces'

import withListenTo, { WithBackboneProps } from '../backbone-container'
const store = require('../../../js/store.js')

function hasUnsaved() {
  return store.get('workspaces').find(function(workspace: any) {
    return !workspace.isSaved()
  })
}

type State = {
  hasUnsaved: boolean
}

class WorkspacesContainer extends React.Component<WithBackboneProps, State> {
  constructor(props: WithBackboneProps) {
    super(props)
    this.state = {
      hasUnsaved: hasUnsaved(),
    }
  }
  componentDidMount() {
    this.props.listenTo(
      store.get('workspaces'),
      'change:saved update add remove',
      this.handleSaved.bind(this)
    )
  }
  handleSaved() {
    this.setState({
      hasUnsaved: hasUnsaved(),
    })
  }
  saveAllWorkspaces() {
    store.get('workspaces').saveAll()
  }
  render() {
    return (
      <Workspaces
        hasUnsaved={this.state.hasUnsaved}
        saveAllWorkspaces={this.saveAllWorkspaces.bind(this)}
      />
    )
  }
}

export default withListenTo(WorkspacesContainer)
