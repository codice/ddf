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

const Backbone = require('backbone')
const WorkspaceCollection = require('./model/Workspace.collection.js')
const Content = require('../component/content/content.js')
const router = require('../component/router/router.js')

module.exports = new (Backbone.Model.extend({
  initialize() {
    this.set('content', new Content())
    this.set('workspaces', new WorkspaceCollection())

    window.onbeforeunload = () => {
      const unsaved = this.get('workspaces')
        .chain()
        .map(workspace => {
          if (!workspace.isSaved()) {
            return workspace.get('title')
          }
        })
        .filter(title => title !== undefined)
        .value()

      if (unsaved.length > 0) {
        return (
          'Do you really want to close? Unsaved workspaces: ' +
          unsaved.join(', ')
        )
      }
    }

    this.listenTo(this.get('workspaces'), 'remove', function() {
      const currentWorkspace = this.getCurrentWorkspace()
      if (currentWorkspace && !this.get('workspaces').get(currentWorkspace)) {
        this.get('content').set('currentWorkspace', undefined)
      }
    })
    this.listenTo(
      this.get('content'),
      'change:currentWorkspace',
      this.handleWorkspaceChange
    )
    this.listenTo(router, 'change', this.handleRoute)
    this.handleRoute()
  },
  handleRoute() {
    if (router.toJSON().name === 'openWorkspace') {
      const workspaceId = router.get('args')[0]
      if (this.get('workspaces').get(workspaceId) !== undefined) {
        this.setCurrentWorkspaceById(workspaceId)
      } else {
        router.notFound()
      }
    }
  },
  clearOtherWorkspaces(workspaceId) {
    this.get('workspaces').forEach(workspaceModel => {
      if (workspaceId !== workspaceModel.id) {
        workspaceModel.clearResults()
      }
    })
  },
  handleWorkspaceChange() {
    if (this.get('content').changedAttributes().currentWorkspace) {
      const previousWorkspace = this.get('content').previousAttributes()
        .currentWorkspace
      if (
        previousWorkspace &&
        previousWorkspace.id !== this.get('content').get('currentWorkspace').id
      ) {
        previousWorkspace.clearResults()
      }
    }
  },
  getWorkspaceById(workspaceId) {
    return this.get('workspaces').get(workspaceId)
  },
  setCurrentWorkspaceById(workspaceId) {
    this.get('content').set(
      'currentWorkspace',
      this.get('workspaces').get(workspaceId)
    )
  },
  getCurrentWorkspace() {
    return this.get('content').get('currentWorkspace')
  },
  getCurrentQueries() {
    return this.getCurrentWorkspace().get('queries')
  },
  setQueryById(queryId) {
    const queryRef = this.getCurrentQueries().get(queryId)
    this.setQueryByReference(queryRef.clone())
  },
  setQueryByReference(queryRef) {
    this.get('content').set('query', queryRef)
  },
  getQuery() {
    return this.get('content').get('query')
  },
  getQueryById(queryId) {
    return this.getCurrentQueries().get(queryId)
  },
  getSelectedResults() {
    return this.get('content').get('selectedResults')
  },
  clearSelectedResults() {
    this.getSelectedResults().reset()
  },
  addSelectedResult(metacard) {
    this.getSelectedResults().add(metacard)
  },
  removeSelectedResult(metacard) {
    this.getSelectedResults().remove(metacard)
  },
  getActiveSearchResultsAttributes() {
    return this.get('content').getActiveSearchResultsAttributes()
  },
  getActiveSearchResults() {
    return this.get('content').getActiveSearchResults()
  },
  setActiveSearchResults(results) {
    this.get('content').setActiveSearchResults(results)
  },
  addToActiveSearchResults(results) {
    this.get('content').addToActiveSearchResults(results)
  },
  saveCurrentWorkspace() {
    this.getCurrentWorkspace().save()
  },
  setCurrentQuery(query) {
    this.get('content').setCurrentQuery(query)
  },
  getCurrentQuery() {
    return this.get('content').getCurrentQuery()
  },
  setWorkspaceRestrictions(workspaceId, restrictions) {
    const metacard = this.getWorkspaceById(workspaceId)
    restrictions.forEach(restriction => {
      metacard.attributes[restriction.attribute] = restriction.values
    })
  },
}))()
