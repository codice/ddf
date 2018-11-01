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
import withListenTo, { WithBackboneProps } from '../backbone-container'
import { sortBy } from 'lodash'
import WorkspacesItems from '../../presentation/workspaces-items'
import MarionetteRegionContainer from '../../container/marionette-region-container'

const FilterDropdownView = require('component/dropdown/workspaces-filter/dropdown.workspaces-filter.view')
const SortDropdownView = require('component/dropdown/workspaces-filter/dropdown.workspaces-filter.view')
const user = require('component/singletons/user-instance')
const store = require('js/store')

const preferences = user.get('user').get('preferences')
const LoadingView = require('component/loading/loading.view')
const wreqr = require('wreqr')

interface State {
  filterDropdown: Marionette.View<any>
  sortDropdown: Marionette.View<any>
  byDate: boolean
  workspaces: Array<Backbone.Model[keyof Backbone.Model]>
}

function filterWorkspaces(workspaces: Backbone.Collection<Backbone.Model>) {
  return workspaces.filter((workspace: Backbone.Model) => {
    const localStorage = workspace.get('localStorage') || false
    const owner = workspace.get('metacard.owner')
    const email = user.get('user').get('email')

    switch (preferences.get('homeFilter')) {
      case 'Not owned by me':
        return !localStorage && email !== owner
      case 'Owned by me':
        return localStorage || email === owner
      case 'Owned by anyone':
      default:
        return true
    }
  })
}

function sortWorkspaces(workspaces: Backbone.Model[]) {
  return sortBy(workspaces, (workspace: Backbone.Model) => {
    switch (preferences.get('homeSort')) {
      case 'Title':
        return workspace.get('title').toLowerCase()
      default:
        return -workspace.get('metacard.modified')
    }
  })
}

function determineWorkspaces(workspaces: Backbone.Collection<Backbone.Model>) {
  return sortWorkspaces(filterWorkspaces(workspaces))
}

class WorkspacesItemsContainer extends React.Component<
  WithBackboneProps,
  State
> {
  constructor(props: WithBackboneProps) {
    super(props)
    this.state = {
      filterDropdown: FilterDropdownView.createSimpleDropdown({
        list: [
          {
            label: 'Owned by anyone',
            value: 'Owned by anyone',
          },
          {
            label: 'Owned by me',
            value: 'Owned by me',
          },
          {
            label: 'Not owned by me',
            value: 'Not owned by me',
          },
        ],
        defaultSelection: [preferences.get('homeFilter')],
      }),
      sortDropdown: SortDropdownView.createSimpleDropdown({
        list: [
          {
            label: 'Last modified',
            value: 'Last modified',
          },
          {
            label: 'Title',
            value: 'Title',
          },
        ],
        defaultSelection: [preferences.get('homeSort')],
      }),
      byDate: preferences.get('homeSort') === 'Last modified',
      workspaces: determineWorkspaces(store.get('workspaces')),
    }
  }
  componentDidMount() {
    this.props.listenTo(
      store.get('workspaces'),
      'add reset remove',
      this.updateWorkspaces.bind(this)
    )
    this.props.listenTo(
      preferences,
      'change:homeFilter',
      this.updateWorkspaces.bind(this)
    )
    this.props.listenTo(
      preferences,
      'change:homeSort',
      this.handleSort.bind(this)
    )
    this.props.listenTo(
      this.state.sortDropdown.model,
      'change:value',
      this.save('homeSort')
    )
    this.props.listenTo(
      this.state.filterDropdown.model,
      'change:value',
      this.save('homeFilter')
    )
  }
  updateWorkspaces() {
    this.setState({
      workspaces: determineWorkspaces(store.get('workspaces')),
    })
  }
  handleSort() {
    this.setState({
      workspaces: determineWorkspaces(store.get('workspaces')),
      byDate: preferences.get('homeSort') === 'Last modified',
    })
  }
  save(key: string) {
    return function(_unused_model: any, value: any) {
      var prefs = user.get('user').get('preferences')
      prefs.set(key, value[0])
      prefs.savePreferences()
    }
  }
  prepForNewWorkspace = () => {
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
  createBlankWorkspace = () => {
    this.prepForNewWorkspace()
    store.get('workspaces').createWorkspace()
  }
  render() {
    return (
      <WorkspacesItems
        byDate={this.state.byDate}
        filterDropdown={
          <MarionetteRegionContainer view={this.state.filterDropdown} />
        }
        sortDropdown={
          <MarionetteRegionContainer view={this.state.sortDropdown} />
        }
        workspaces={this.state.workspaces}
        createBlankWorkspace={this.createBlankWorkspace}
      />
    )
  }
}

export default withListenTo(WorkspacesItemsContainer)
