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
import WorkspacesTemplates from '../../presentation/workspaces-templates'
import { hot } from 'react-hot-loader'

const store = require('../../../js/store.js')
const LoadingView = require('../../../component/loading/loading.view.js')
const wreqr = require('../../../js/wreqr.js')
const properties = require('../../../js/properties.js')

interface Props {
  hasUnsaved: boolean
}

interface State {
  value: string
  placeholder: string
}

class WorkspacesTemplatesContainer extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = {
      placeholder: 'Search ' + properties.branding + ' ' + properties.product,
      value: '',
    }
  }
  startAdhocSearch() {
    this.prepForCreateNewWorkspace()
    store.get('workspaces').createAdhocWorkspace(this.state.value)
  }
  onChange(value: string) {
    this.setState({
      value,
    })
  }
  prepForCreateNewWorkspace() {
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
  render() {
    return (
      <WorkspacesTemplates
        startAdhocSearch={this.startAdhocSearch.bind(this)}
        onChange={this.onChange.bind(this)}
        value={this.state.value}
        placeholder={this.state.placeholder}
      />
    )
  }
}

export default hot(module)(WorkspacesTemplatesContainer)
