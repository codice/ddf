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
import { sortBy } from 'lodash'
import WorkspacesItems from '../../presentation/workspaces-items'
import MarionetteRegionContainer from '../../container/marionette-region-container'
import Dropdown from '../../presentation/dropdown'
import NavigationBehavior from '../../presentation/navigation-behavior'
import MenuSelection from '../../presentation/menu-selection'
import { hot } from 'react-hot-loader'

const SortDropdownView = require('../../../component/dropdown/workspaces-filter/dropdown.workspaces-filter.view.js')
const user = require('../../../component/singletons/user-instance.js')
const store = require('../../../js/store.js')

const preferences = user.get('user').get('preferences')
const LoadingView = require('../../../component/loading/loading.view.js')
const wreqr = require('../../../js/wreqr.js')

interface State {
  sortDropdown: Marionette.View<any>
  homeFilter: string
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
        // We want to sort in descending order. Parse the timestamp to a Date and find seconds since the epoch. Sort by the inverse of that.
        return -new Date(workspace.get('metacard.modified')).getTime()
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
      homeFilter: preferences.get('homeFilter'),
      byDate: preferences.get('homeSort') === 'Last modified',
      workspaces: determineWorkspaces(store.get('workspaces')),
    }
  }
  componentDidMount() {
    this.props.listenTo(
      store.get('workspaces'),
      'add reset remove change',
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
    this.props.listenTo(preferences, 'change:homeFilter', this.handleHomeFilter)
    this.props.listenTo(
      this.state.sortDropdown.model,
      'change:value',
      this.save('homeSort')
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
  handleHomeFilter = () => {
    this.setState({
      homeFilter: preferences.get('homeFilter'),
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
  reactComponentSave(key: string, value: string) {
    return function() {
      var prefs = user.get('user').get('preferences')
      prefs.set(key, value)
      prefs.savePreferences()
    }
  }
  render() {
    return (
      <WorkspacesItems
        byDate={this.state.byDate}
        filterDropdown={
          <Dropdown
            content={context => (
              <NavigationBehavior>
                <MenuSelection
                  onClick={() => {
                    this.reactComponentSave('homeFilter', 'Owned by anyone')()
                    context.closeAndRefocus()
                  }}
                  isSelected={this.state.homeFilter === 'Owned by anyone'}
                >
                  Owned by Anyone
                </MenuSelection>
                <MenuSelection
                  onClick={() => {
                    this.reactComponentSave('homeFilter', 'Owned by me')()
                    context.closeAndRefocus()
                  }}
                  isSelected={this.state.homeFilter === 'Owned by me'}
                >
                  Owned by Me
                </MenuSelection>
                <MenuSelection
                  onClick={() => {
                    this.reactComponentSave('homeFilter', 'Not owned by me')()
                    context.closeAndRefocus()
                  }}
                  isSelected={this.state.homeFilter === 'Not owned by me'}
                >
                  Not Owned by Me
                </MenuSelection>
              </NavigationBehavior>
            )}
          >
            {this.state.homeFilter}
            <span className="fa-filter fa" />
          </Dropdown>
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

export default hot(module)(withListenTo(WorkspacesItemsContainer))
