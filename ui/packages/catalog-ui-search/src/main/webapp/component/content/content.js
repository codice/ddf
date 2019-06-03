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

const $ = require('jquery')
const _ = require('underscore')
const Backbone = require('backbone')
const wreqr = require('../../js/wreqr.js')
const Metacard = require('../../js/model/Metacard.js')
const Query = require('../../js/model/Query.js')
const Workspace = require('../../js/model/Workspace.js')
const QueryResult = require('../../js/model/QueryResult.js')

module.exports = Backbone.AssociatedModel.extend({
  relations: [
    {
      type: Backbone.One,
      key: 'currentQuery',
      relatedModel: Query.Model,
    },
    {
      type: Backbone.One,
      key: 'currentWorkspace',
      relatedModel: Workspace,
    },
    {
      type: Backbone.Many,
      key: 'selectedResults',
      relatedModel: Metacard,
    },
    {
      type: Backbone.Many,
      key: 'results',
      relatedModel: Metacard,
    },
    {
      type: Backbone.Many,
      key: 'filteredQueries',
      relatedModel: Query.Model,
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
    currentWorkspace: undefined,
    selectedResults: [],
    queryId: undefined,
    savedItems: undefined,
    query: undefined,
    state: undefined,
    results: [], //list of metacards
    filteredQueries: [],
    editing: true,
    activeSearchResults: [],
    activeSearchResultsAttributes: [],
    drawing: false,
    drawingModel: undefined,
  },
  initialize() {
    this.listenTo(wreqr.vent, 'search:drawline', this.turnOnDrawing)
    this.listenTo(wreqr.vent, 'search:drawcircle', this.turnOnDrawing)
    this.listenTo(wreqr.vent, 'search:drawpoly', this.turnOnDrawing)
    this.listenTo(wreqr.vent, 'search:drawbbox', this.turnOnDrawing)
    this.listenTo(wreqr.vent, 'search:drawstop', this.turnOffDrawing)
    this.listenTo(wreqr.vent, 'search:drawend', this.turnOffDrawing)
    this.listenTo(
      this.get('activeSearchResults'),
      'update add remove reset',
      this.updateActiveSearchResultsAttributes
    )
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
  turnOnDrawing(model) {
    this.set('drawing', true)
    this.set('drawingModel', model)
    $('html').toggleClass('is-drawing', true)
  },
  turnOffDrawing() {
    this.set('drawing', false)
    $('html').toggleClass('is-drawing', false)
  },
  isEditing() {
    return this.get('editing')
  },
  turnOnEditing() {
    this.set('editing', true)
  },
  turnOffEditing() {
    this.set('editing', false)
  },
  getQuery() {
    return this.get('query')
  },
  setQuery(queryRef) {
    this.set('query', queryRef)
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
  filterQuery(queryRef) {
    const filteredQueries = this.get('filteredQueries')
    const filtered = Boolean(filteredQueries.get(queryRef))
    if (filtered) {
      filteredQueries.remove(queryRef)
    } else {
      filteredQueries.add(queryRef)
    }
  },
  setCurrentQuery(query) {
    this.set('currentQuery', query)
  },
  getCurrentQuery() {
    return this.get('currentQuery')
  },
})
