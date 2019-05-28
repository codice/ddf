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

const _ = require('underscore')
const Backbone = require('backbone')
const Metacard = require('../../js/model/Metacard.js')
const UploadBatch = require('../../js/model/UploadBatch.js')
const Query = require('../../js/model/Query.js')
const QueryResponse = require('../../js/model/QueryResponse.js')
const QueryResult = require('../../js/model/QueryResult.js')
const router = require('../router/router.js')
const cql = require('../../js/cql.js')
const user = require('../singletons/user-instance.js')

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
      key: 'currentUpload',
      relatedModel: UploadBatch,
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
    currentUpload: undefined,
    currentResult: undefined,
    selectedResults: [],
    activeSearchResults: [],
    activeSearchResultsAttributes: [],
  },
  initialize() {
    this.listenTo(router, 'change', this.handleRoute)
    this.set('currentResult', new QueryResponse())
    this.listenTo(this, 'change:currentUpload', this.clearSelectedResults)
    this.listenTo(
      this.get('activeSearchResults'),
      'update add remove reset',
      this.updateActiveSearchResultsAttributes
    )
    this.handleRoute()
  },
  handleRoute() {
    const routerJSON = router.toJSON()
    if (routerJSON.name === 'openUpload') {
      const uploadId = routerJSON.args[0]
      const upload = user
        .get('user')
        .get('preferences')
        .get('uploads')
        .get(uploadId)
      if (!upload) {
        router.notFound()
      } else {
        const queryForMetacards = new Query.Model({
          cql: cql.write({
            type: 'OR',
            filters: _.flatten(
              upload
                .get('uploads')
                .filter(file => file.id || file.get('children') !== undefined)
                .map(file => {
                  if (file.get('children') !== undefined) {
                    return file.get('children').map(child => ({
                      type: '=',
                      value: child,
                      property: '"id"',
                    }))
                  } else {
                    return {
                      type: '=',
                      value: file.id,
                      property: '"id"',
                    }
                  }
                })
                .concat({
                  type: '=',
                  value: '-1',
                  property: '"id"',
                })
            ),
          }),
          federation: 'enterprise',
        })
        if (this.get('currentQuery')) {
          this.get('currentQuery').cancelCurrentSearches()
        }
        queryForMetacards.startSearch()
        this.set({
          currentResult: queryForMetacards.get('result'),
          currentUpload: upload,
          currentQuery: queryForMetacards,
        })
        this.trigger('change:currentUpload', upload)
      }
    }
  },
  updateActiveSearchResultsAttributes() {
    const availableAttributes = this.get('activeSearchResults')
      .reduce((currentAvailable, result) => {
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
  getActiveSearchResultsAttributes() {
    return this.get('activeSearchResultsAttributes')
  },
  getActiveSearchResults() {
    return this.get('activeSearchResults')
  },
  setActiveSearchResults(results) {
    this.get('activeSearchResults').reset(results.models || results)
  },
  addToActiveSearchResults(results) {
    this.get('activeSearchResults').add(results.models || results)
  },
  getSelectedResults() {
    return this.get('selectedResults')
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
  setCurrentQuery(query) {
    this.set('currentQuery', query)
  },
  getCurrentQuery() {
    return this.get('currentQuery')
  },
}))()
