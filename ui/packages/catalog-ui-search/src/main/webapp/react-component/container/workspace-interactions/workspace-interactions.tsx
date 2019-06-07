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
import WorkspaceInteractionsPresentation from '../../presentation/workspace-interactions'
import { hot } from 'react-hot-loader'
import withListenTo, { WithBackboneProps } from '../backbone-container'
import { Sharing } from '../sharing'
import { Security, Restrictions } from '../../utils/security'
const user = require('../../../component/singletons/user-instance.js')
const store = require('../../../js/store.js')
const lightboxInstance = require('../../../component/lightbox/lightbox.view.instance.js')
const wreqr = require('../../../js/wreqr.js')
const LoadingView = require('../../../component/loading/loading.view.js')
const ConfirmationView = require('../../../component/confirmation/confirmation.view.js')

type Props = {
  workspace: any
} & WithBackboneProps
type State = {
  subscribed: boolean
}

type Attribute = {
  attribute: string
  values: string[]
}

const mapPropsToState = (props: Props) => {
  return {
    subscribed: props.workspace.get('subscribed'),
  }
}

class WorkspaceInteractions extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = mapPropsToState(props)
    this.listenToWorkspace()
  }
  componentDidUpdate(prevProps: Props) {
    if (prevProps.workspace !== this.props.workspace) {
      this.props.stopListening(prevProps.workspace)
      this.listenToWorkspace()
      this.handleChange()
    }
  }
  listenToWorkspace = () => {
    this.props.listenTo(this.props.workspace, 'change', this.handleChange)
  }
  handleChange = () => {
    this.setState(mapPropsToState(this.props))
  }
  isShareable = () => {
    return user.canShare(this.props.workspace)
  }
  isDeletable = () => {
    return user.canWrite(this.props.workspace)
  }
  runAllSearches = () => {
    store.clearOtherWorkspaces(this.props.workspace.id)
    this.props.workspace.get('queries').forEach(function(query: any) {
      query.startSearch()
    })
  }
  cancelAllSearches = () => {
    this.props.workspace.get('queries').forEach(function(query: any) {
      query.cancelCurrentSearches()
    })
  }
  subscribeToWorkspace = () => {
    this.props.workspace.subscribe()
  }
  unsubscribeFromWorkspace = () => {
    this.props.workspace.unsubscribe()
  }
  openWorkspaceInNewTab = () => {
    window.open('./#workspaces/' + this.props.workspace.id)
  }
  updateWorkspaceRestrictions = (attributes: Attribute[]) => {
    store.setWorkspaceRestrictions(this.props.workspace.id, attributes)
  }
  viewSharing = () => {
    lightboxInstance.model.updateTitle('Workspace Sharing')
    lightboxInstance.model.open()
    lightboxInstance.showContent(
      <Sharing
        key={this.props.workspace.id}
        id={this.props.workspace.id}
        lightbox={lightboxInstance}
        onUpdate={this.updateWorkspaceRestrictions}
      />
    )
  }
  viewDetails = () => {
    wreqr.vent.trigger('router:navigate', {
      fragment: 'metacards/' + this.props.workspace.id,
      options: {
        trigger: true,
      },
    })
  }
  duplicateWorkspace = () => {
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
    store.get('workspaces').duplicateWorkspace(this.props.workspace)
  }
  deleteWorkspace = () => {
    var loadingview = new LoadingView()
    store
      .getWorkspaceById(this.props.workspace.id)
      .off(null, null, 'handleTrash')
    store.getWorkspaceById(this.props.workspace.id).once(
      'sync',
      function() {
        wreqr.vent.trigger('router:navigate', {
          fragment: 'workspaces',
          options: {
            trigger: true,
          },
        })
        loadingview.remove()
      },
      'handleTrash'
    )
    store.getWorkspaceById(this.props.workspace.id).once(
      'error',
      function() {
        loadingview.remove()
      },
      'handleTrash'
    )
    store.getWorkspaceById(this.props.workspace.id).destroy({
      wait: true,
    })
  }
  deletionPrompt = () => {
    const workspace = store.getWorkspaceById(this.props.workspace.id)
    const security = new Security(Restrictions.from(workspace))

    if (!security.isShared()) {
      this.deleteWorkspace()
    } else {
      const self = this
      this.props.listenTo(
        ConfirmationView.generateConfirmation({
          prompt:
            'Are you sure you want to delete this workspace? It has been shared with other users.',
          no: 'Cancel',
          yes: 'Delete',
        }),
        'change:choice',
        function(confirmation: any) {
          if (confirmation.get('choice')) {
            self.deleteWorkspace()
          }
        }.bind(this)
      )
    }
  }
  saveWorkspace = () => {
    this.props.workspace.save()
  }
  render() {
    const { workspace } = this.props
    const { subscribed } = this.state
    return (
      <WorkspaceInteractionsPresentation
        isLocal={workspace.isLocal()}
        isShareable={this.isShareable()}
        isDeletable={this.isDeletable()}
        isSubscribed={subscribed}
        saveWorkspace={this.saveWorkspace}
        runAllSearches={this.runAllSearches}
        cancelAllSearches={this.cancelAllSearches}
        subscribeToWorkspace={this.subscribeToWorkspace}
        unsubscribeFromWorkspace={this.unsubscribeFromWorkspace}
        openWorkspaceInNewTab={this.openWorkspaceInNewTab}
        viewSharing={this.viewSharing}
        viewDetails={this.viewDetails}
        duplicateWorkspace={this.duplicateWorkspace}
        deletionPrompt={this.deletionPrompt}
      />
    )
  }
}

export default hot(module)(withListenTo(WorkspaceInteractions))
