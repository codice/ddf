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
var $ = require('jquery')
var _ = require('underscore')
var Backbone = require('backbone')
var Query = require('./Query.js')
var List = require('./List.js')
var Common = require('../Common.js')
var ColorGenerator = require('../ColorGenerator.js')
var QueryPolling = require('../QueryPolling.js')
require('backbone-associations')
import PartialAssociatedModel from '../../js/extensions/backbone.partialAssociatedModel'

// This is a list of model attributes that if changed we do not want save the workspace for
const IGNORED_WORKSPACE_ATTRIBUTES = [
  'result',
  'saved',
  'metacard.modified',
  'id',
  'subscribed',
  'serverPageIndex',
]

/**
 * Check if any of the `changedAttributes` fall outside of the `IGNORED_WORKSPACE_ATTRIBUTES` list.
 * @param {model} model
 */
const workspaceShouldBeResaved = model =>
  model &&
  _.intersection(
    Object.keys(model.changedAttributes()),
    IGNORED_WORKSPACE_ATTRIBUTES
  ).length === 0

const WorkspaceQueryCollection = Backbone.Collection.extend({
  model: Query.Model,
  initialize: function() {
    var searchList = this
    this._colorGenerator = ColorGenerator.getNewGenerator()
    this.listenTo(this, 'add', function(query) {
      query.setColor(searchList._colorGenerator.getColor(query.getId()))
      QueryPolling.handleAddingQuery(query)
    })
    this.listenTo(this, 'remove', function(query) {
      searchList._colorGenerator.removeColor(query.getId())
      QueryPolling.handleRemovingQuery(query)
    })
  },
  canAddQuery: function() {
    return this.length < 10
  },
})

const WorkspaceListCollection = Backbone.Collection.extend({
  model: List,
  comparator: list => {
    return list.get('title').toLowerCase()
  },
})

module.exports = PartialAssociatedModel.extend({
  useAjaxSync: true,
  defaults: function() {
    return {
      queries: [],
      metacards: [],
      lists: [],
      saved: true,
    }
  },
  relations: [
    {
      type: Backbone.Many,
      key: 'queries',
      collectionType: WorkspaceQueryCollection,
    },
    {
      type: Backbone.Many,
      key: 'lists',
      collectionType: WorkspaceListCollection,
    },
  ],
  canAddQuery: function() {
    return this.get('queries').length < 10
  },
  tryToAddQuery: function(queryModel) {
    if (this.canAddQuery()) {
      this.get('queries').add(queryModel)
    }
  },
  addQuery: function() {
    var query = new Query.Model({
      excludeUnnecessaryAttributes: false,
    })
    this.get('queries').add(query)
    return query.get('id')
  },
  initialize: function() {
    this.get('queries').on('add', (model, collection) => {
      model.set('isLocal', this.isLocal())
      collection.trigger('change')
    })
    this.listenTo(
      this.get('queries'),
      'update add remove',
      this.handleQueryChange
    )
    this.listenTo(
      this.get('lists'),
      'change update add remove',
      this.handleListChange
    )
    this.listenTo(this.get('queries'), 'change', this.handleChange)
    this.listenTo(this, 'change', this.handleChange)
    this.listenTo(this, 'error', this.handleError)
  },
  handleListChange: function(model) {
    if (
      model !== undefined &&
      _.intersection(Object.keys(model.changedAttributes()), ['actions'])
        .length === 0
    ) {
      this.set('saved', false)
    }
  },
  handleQueryChange: function() {
    this.set('saved', false)
  },
  handleChange: function(model) {
    if (workspaceShouldBeResaved(model)) {
      this.set('saved', false)
    }
  },
  saveLocal: function(options) {
    this.set('id', this.get('id') || Common.generateUUID())
    this.set('metacard.modified', Date.now())
    var localWorkspaces = this.collection.getLocalWorkspaces()
    localWorkspaces[this.get('id')] = this.toJSON()
    window.localStorage.setItem('workspaces', JSON.stringify(localWorkspaces))
    this.trigger('sync', this, options)
  },
  destroyLocal: function(options) {
    var localWorkspaces = this.collection.getLocalWorkspaces()
    delete localWorkspaces[this.get('id')]
    window.localStorage.setItem('workspaces', JSON.stringify(localWorkspaces))
    this.collection.remove(this)
    this.trigger('sync', this, options)
  },
  save: function(options) {
    this.set('saved', true)
    if (this.get('localStorage')) {
      this.saveLocal(options)
    } else {
      Backbone.AssociatedModel.prototype.save.apply(this, arguments)
    }
  },
  handleError: function() {
    this.set('saved', false)
  },
  isLocal: function() {
    return Boolean(this.get('localStorage'))
  },
  isSaved: function() {
    return this.get('saved')
  },
  destroy: function(options) {
    this.get('queries').forEach(function(query) {
      QueryPolling.handleRemovingQuery(query)
    })
    if (this.get('localStorage')) {
      this.destroyLocal(options)
    } else {
      return Backbone.AssociatedModel.prototype.destroy.apply(this, arguments)
    }
  },
  subscribe: function() {
    $.ajax({
      type: 'post',
      url: './internal/subscribe/' + this.get('id'),
    }).then(
      function() {
        this.set('subscribed', true)
      }.bind(this)
    )
  },
  unsubscribe: function() {
    $.ajax({
      type: 'post',
      url: './internal/unsubscribe/' + this.get('id'),
    }).then(
      function() {
        this.set('subscribed', false)
      }.bind(this)
    )
  },
  clearResults: function() {
    this.get('queries').forEach(function(queryModel) {
      queryModel.clearResults()
    })
  },
})
