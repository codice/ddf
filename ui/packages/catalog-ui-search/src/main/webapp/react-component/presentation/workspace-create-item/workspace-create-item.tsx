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

const store = require('js/store')
const LoadingView = require('component/loading/loading.view')
const wreqr = require('wreqr')
const Root = styled.div`
  width: 100%;
  height: 100%;
  display: inline-block;
  cursor: pointer;
  text-align: center;
  margin: auto;
  padding-top: 1rem;
`

const Header = styled.h3`
  padding-top: 1rem;
  font-weight: bolder;
`

class WorkspaceCreateItem extends React.Component {
  prepForNewWorkspace() {
    var loadingview = new LoadingView()
    store.get('workspaces').once('sync', function(workspace: any) {
      loadingview.remove()
      wreqr.vent.trigger('router:navigate', {
        fragment: 'workspaces/' + workspace.id,
        options: {
          trigger: true,
        },
      })
    })
  }

  createBlankWorkspace() {
    this.prepForNewWorkspace()
    store.get('workspaces').createWorkspace()
  }

  render() {
    return (
      <Root onClick={this.createBlankWorkspace.bind(this)}>
        <div className="fa fa-plus-circle fa-4x" />
        <Header>Add New Workspace</Header>
      </Root>
    )
  }
}

export default hot(module)(WorkspaceCreateItem)
