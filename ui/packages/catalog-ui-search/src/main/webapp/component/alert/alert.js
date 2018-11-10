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
/*global define*/
const _ = require('underscore')
const Backbone = require('backbone')
const Metacard = require('../../js/model/Metacard.js')
const Alert = require('../../js/model/Alert.js')
const Query = require('../../js/model/Query.js')
const QueryResponse = require('../../js/model/QueryResponse.js')
const QueryResult = require('../../js/model/QueryResult.js')
const router = require('../router/router.js')
const user = require('../singletons/user-instance.js')
const cql = require('../../js/cql.js')

module.exports = new (Backbone.AssociatedModel.extend({
  relations: [
    {
      type: Backbone.One,
      key: 'currentQuery',
      relatedModel: Query.Model,
    },
    {
      type: Backbone.One,
      key: 'currentResult',
      relatedModel: QueryResponse,
    },
    {
      type: Backbone.One,
      key: 'currentAlert',
      relatedModel: Alert,
    },
    {
      type: Backbone.Many,
      key: 'selectedResults',
      relatedModel: Metacard,
    },
    {
      type: Backbone.Many,
      key: 'activeSearchResults',
      relatedModel: QueryResult,
    },
    {
      type: Backbone.Many,
      key: 'completeActiveSearchResults',
      relatedModel: QueryResult,
    },
  ],
  defaults: {
    currentQuery: undefined,
    currentAlert: undefined,
    currentResult: undefined,
    selectedResults: [],
    activeSearchResults: [],
    activeSearchResultsAttributes: [],
    completeActiveSearchResults: [],
    completeActiveSearchResultsAttributes: [],
  },
  initialize: function() {
    this.set('currentResult', new QueryResponse())
    this.listenTo(this, 'change:currentAlert', this.clearSelectedResults)
    this.listenTo(
      this.get('activeSearchResults'),
      'update add remove reset',
      this.updateActiveSearchResultsAttributes
    )
    this.listenTo(
      this.get('completeActiveSearchResults'),
      'update add remove reset',
      this.updateActiveSearchResultsFullAttributes
    )
    this.listenTo(router, 'change', this.handleRoute)
    this.handleRoute()
  },
  handleRoute() {
    const routerJSON = router.toJSON()
    if (routerJSON.name === 'openAlert') {
      var alertId = routerJSON.args[0]
      var alert = user
        .get('user')
        .get('preferences')
        .get('alerts')
        .get(alertId)
      if (!alert) {
        router.notFound()
      } else {
        const queryForMetacards = new Query.Model({
          cql: cql.write({
            type: 'OR',
            filters: alert
              .get('metacardIds')
              .map(function(metacardId) {
                return {
                  type: '=',
                  value: metacardId,
                  property: '"id"',
                }
              })
              .concat({
                type: '=',
                value: '-1',
                property: '"id"',
              }),
          }),
          federation: 'enterprise',
        })
        if (this.get('currentQuery')) {
          this.get('currentQuery').cancelCurrentSearches()
        }
        queryForMetacards.startSearch()
        this.set({
          currentResult: queryForMetacards.get('result'),
          currentAlert: alert,
          currentQuery: queryForMetacards,
        })
        this.trigger('change:currentAlert', alert)
      }
    }
  },
  updateActiveSearchResultsAttributes: function() {
    var availableAttributes = this.get('activeSearchResults')
      .reduce(function(currentAvailable, result) {
        currentAvailable = _.union(
          currentAvailable,
          Object.keys(
            result
              .get('metacard')
              .get('properties')
              .toJSON()
          )
        )
        return currentAvailable
      }, [])
      .sort()
    this.set('activeSearchResultsAttributes', availableAttributes)
  },
  updateActiveSearchResultsFullAttributes: function() {
    var availableAttributes = this.get('completeActiveSearchResults')
      .reduce(function(currentAvailable, result) {
        currentAvailable = _.union(
          currentAvailable,
          Object.keys(
            result
              .get('metacard')
              .get('properties')
              .toJSON()
          )
        )
        return currentAvailable
      }, [])
      .sort()
    this.set('completeActiveSearchResultsAttributes', availableAttributes)
  },
  getCompleteActiveSearchResultsAttributes: function() {
    return this.get('completeActiveSearchResultsAttributes')
  },
  getCompleteActiveSearchResults: function() {
    return this.get('completeActiveSearchResults')
  },
  setCompleteActiveSearchResults: function(results) {
    this.get('completeActiveSearchResults').reset(results.models || results)
  },
  getActiveSearchResultsAttributes: function() {
    return this.get('activeSearchResultsAttributes')
  },
  getActiveSearchResults: function() {
    return this.get('activeSearchResults')
  },
  setActiveSearchResults: function(results) {
    this.get('activeSearchResults').reset(results.models || results)
  },
  addToActiveSearchResults: function(results) {
    this.get('activeSearchResults').add(results.models || results)
  },
  getSelectedResults: function() {
    return this.get('selectedResults')
  },
  clearSelectedResults: function() {
    this.getSelectedResults().reset()
  },
  addSelectedResult: function(metacard) {
    this.getSelectedResults().add(metacard)
  },
  removeSelectedResult: function(metacard) {
    this.getSelectedResults().remove(metacard)
  },
  setCurrentQuery: function(query) {
    this.set('currentQuery', query)
  },
  getCurrentQuery: function() {
    return this.get('currentQuery')
  },
}))()
