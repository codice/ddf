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
import withListenTo, { WithBackboneProps } from '../backbone-container'
import WorkspacesItem from '../../presentation/workspace-item'
const moment = require('moment')
const wreqr = require('../../../js/wreqr.js')

type Props = {
  workspace: any
} & WithBackboneProps

interface State {
  title: string
  date: string
  owner: string
  unsaved: boolean
  localStorage: boolean
}

const getStateFromWorkspace = (workspace: any) => {
  return {
    title: workspace.get('title'),
    date: moment(workspace.get('metacard.modified')).fromNow(),
    owner: workspace.get('metacard.owner') || 'Guest',
    unsaved: !workspace.isSaved(),
    localStorage: workspace.get('localStorage'),
  }
}

class WorkspacesItemContainer extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = getStateFromWorkspace(this.props.workspace)
  }
  componentDidMount() {
    this.props.listenTo(
      this.props.workspace,
      'change',
      this.handleChange.bind(this)
    )
  }
  handleChange() {
    this.setState(getStateFromWorkspace(this.props.workspace))
  }
  openWorkspace() {
    wreqr.vent.trigger('router:navigate', {
      fragment: 'workspaces/' + this.props.workspace.id,
      options: {
        trigger: true,
      },
    })
  }
  render() {
    return (
      <WorkspacesItem
        title={this.state.title}
        date={this.state.date}
        owner={this.state.owner}
        unsaved={this.state.unsaved}
        localStorage={this.state.localStorage}
        workspace={this.props.workspace}
        openWorkspace={this.openWorkspace.bind(this)}
      />
    )
  }
}

export default withListenTo(WorkspacesItemContainer)
