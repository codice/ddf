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
import fetch from '../../react-component/utils/fetch'
var $ = require('jquery')
var _ = require('underscore')
var Backbone = require('backbone')
var Query = require('js/model/Query')
var cql = require('js/cql')
var user = require('component/singletons/user-instance')
var moment = require('moment')
require('backbone-associations')
var WorkspaceModel = require('js/model/Workspace')

const loadQueries = async id => {
  const response = await fetch(`./internal/workspace/${id}/queries`)
  return response.json()
}

module.exports = Backbone.Collection.extend({
  model: WorkspaceModel,
  url: './internal/workspaces',
  useAjaxSync: true,
  fetched: false,
  handleSync: function() {
    this.fetched = true
  },
  initialize: function() {
    this.listenTo(this, 'sync', this.handleSync)
    this.handleUserChange()
    this.listenTo(user, 'change', this.handleUserChange)
    var collection = this
    collection.on('add', function(workspace) {
      workspace.on('change:lastModifiedDate', function() {
        collection.sort()
      })
    })
    this.listenTo(this, 'add', this.tagGuestWorkspace)
  },
  handleUserChange: function() {
    this.fetch({
      remove: false,
    })
  },
  tagGuestWorkspace: function(model) {
    if (this.isGuestUser() && model.isNew()) {
      model.set({
        localStorage: true,
      })
    }
  },
  isGuestUser: function() {
    return user.get('user').isGuestUser()
  },
  comparator: function(workspace) {
    return -moment(workspace.get('lastModifiedDate')).unix()
  },
  createWorkspace: function(title) {
    this.create({
      title: title || 'New Workspace',
    })
  },
  createWorkspaceWithQuery: function(queryModel) {
    this.createWorkspaceAndStartSearch('New Workspace', queryModel)
  },
  createAdhocWorkspace(text) {
    var cqlQuery
    var title = text
    if (text.length === 0) {
      cqlQuery = "anyText ILIKE '%'"
      title = '*'
    } else {
      cqlQuery = "anyText ILIKE '" + cql.translateUserqlToCql(text) + "'"
    }
    var queryForWorkspace = new Query.Model({
      title: title,
      cql: cqlQuery,
      type: 'text',
    })
    this.createWorkspaceAndStartSearch(title, queryForWorkspace)
  },
  createLocalWorkspace: function() {
    var queryForWorkspace = new Query.Model({
      title: 'Example Local',
      federation: 'local',
      excludeUnnecessaryAttributes: false,
      cql: "anyText ILIKE '%'",
      type: 'basic',
    })
    this.createWorkspaceAndStartSearch('Template Local', queryForWorkspace)
  },
  createAllWorkspace: function() {
    var queryForWorkspace = new Query.Model({
      title: 'Example Federated',
      federation: 'enterprise',
      excludeUnnecessaryAttributes: false,
      cql: "anyText ILIKE '%'",
      type: 'basic',
    })
    this.createWorkspaceAndStartSearch('Template Federated', queryForWorkspace)
  },
  createGeoWorkspace: function() {
    var queryForWorkspace = new Query.Model({
      title: 'Example Location',
      excludeUnnecessaryAttributes: false,
      cql:
        "anyText ILIKE '%' AND INTERSECTS(anyGeo, POLYGON((-130.7514 20.6825, -130.7514 44.5780, -65.1230 44.5780, -65.1230 20.6825, -130.7514 20.6825)))",
      type: 'basic',
    })
    this.createWorkspaceAndStartSearch('Template Location', queryForWorkspace)
  },
  createLatestWorkspace: function() {
    var queryForWorkspace = new Query.Model({
      title: 'Example Temporal',
      excludeUnnecessaryAttributes: false,
      cql:
        'anyText ILIKE \'%\' AND ("created" AFTER ' +
        moment()
          .subtract(1, 'days')
          .toISOString() +
        ')',
      type: 'basic',
    })
    this.createWorkspaceAndStartSearch('Template Temporal', queryForWorkspace)
  },
  createWorkspaceAndStartSearch(title, queryModel) {
    this.create({
      title: title,
      queries: [(queryModel.toJSON && queryModel.toJSON()) || queryModel],
    })
      .get('queries')
      .first()
      .startSearch()
  },
  duplicateWorkspace: function(workspace) {
    let duplicateWorkspace = _.pick(workspace.toJSON(), 'title', 'queries')
    duplicateWorkspace.queries = duplicateWorkspace.queries.map(query =>
      _.omit(query, 'isLocal', 'id')
    )
    this.create(duplicateWorkspace)
  },
  fetch() {
    //NOTE: This isn't terribly efficient and actual query retrieval should probably happen when the workspace is actually clicked on
    this.once('sync', async workspaces => {
      await Promise.all(
        workspaces.map(async workspace => {
          const queries = await loadQueries(workspace.get('id'))
          workspace.set(
            { queries: queries.map(query => new Query.Model(query)) },
            { silent: true }
          )
        })
      )
    })
    Backbone.Collection.prototype.fetch.apply(this, arguments)
  },
  saveAll: function() {
    this.forEach(function(workspace) {
      if (!workspace.isSaved()) {
        workspace.save()
      }
    })
  },
  saveLocalWorkspaces: function() {
    var localWorkspaces = this.chain()
      .filter(function(workspace) {
        return workspace.get('localStorage')
      })
      .reduce(function(blob, workspace) {
        blob[workspace.get('id')] = workspace.toJSON()
        return blob
      }, {})
      .value()

    window.localStorage.setItem('workspaces', JSON.stringify(localWorkspaces))
  },
  convert2_10Format: function(localWorkspaceJSON) {
    if (localWorkspaceJSON.constructor === Array) {
      return localWorkspaceJSON.reduce(function(blob, workspace) {
        blob[workspace.id] = workspace
        return blob
      }, {})
    } else {
      return localWorkspaceJSON
    }
  },
  getLocalWorkspaces: function() {
    var localWorkspaces = window.localStorage.getItem('workspaces') || '{}'
    try {
      return this.convert2_10Format(JSON.parse(localWorkspaces))
    } catch (e) {
      console.error('Failed to parse local workspaces.', localWorkspaces)
    }
    return {}
  },
  // override parse to merge server response with local storage
  parse(resp) {
    var localWorkspaces = _.map(this.getLocalWorkspaces())
    return resp.concat(localWorkspaces)
  },
})
